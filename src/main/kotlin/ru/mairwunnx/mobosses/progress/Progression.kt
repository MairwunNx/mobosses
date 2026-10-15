package ru.mairwunnx.mobosses.progress

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Players' Mo’Bosses levels. Progress loads while a player logs in, off the server thread, and
 * stays cached while they're online; each kill saves in the background. Without a database (it
 * failed to open) progress lives only until the player leaves.
 */
class Progression(private val plugin: MoBossesPlugin) : Listener {
  private val cache = ConcurrentHashMap<UUID, Progress>()

  @Volatile var store: Store? = null; private set
  private var settings: Settings.Database? = null

  /** Opens the database off the server thread, since a remote one can take a while; only when its settings changed. */
  fun open(next: Settings.Database, folder: File) {
    if (next == settings) return
    settings = next
    val old = store
    store = null
    Thread({
      old?.close()
      try {
        store = Store(next, folder).also { plugin.log.info("Progress is stored in ${it.description}") }
        loadOnline()
      } catch (e: Exception) {
        plugin.log.error("Can't open the database; players' progress lasts only until they leave: ${e.message}")
      }
    }, "Mo’Bosses database").start()
  }

  fun close() {
    store?.close()
    store = null
    settings = null
  }

  /** Any thread: PlaceholderAPI asks from its own. */
  fun of(player: UUID): Progress = cache[player] ?: Progress()

  /** A player's record straight from the database, for players who aren't online. */
  fun load(player: UUID): CompletableFuture<Progress> = store?.load(player) ?: CompletableFuture.completedFuture(Progress())

  /** The best hunters; empty without a database. Any thread. */
  fun top(limit: Int): CompletableFuture<List<Hunter>> = store?.top(limit) ?: CompletableFuture.completedFuture(emptyList())

  /** A player's place on the board; null without a database. */
  fun rank(player: UUID): CompletableFuture<Int?> = store?.rank(player)?.thenApply { it } ?: CompletableFuture.completedFuture(null)

  /** Bosses by type and class; empty without a database. */
  fun counts(): CompletableFuture<List<BossCount>> = store?.counts() ?: CompletableFuture.completedFuture(emptyList())

  /** Counts a boss that spawned, or that players killed, for the statistics. */
  fun count(type: String, clazz: String, killed: Boolean) {
    store?.count(type, clazz, killed)
  }

  /** Null for a player whose progress isn't loaded, so Plan keeps what it stored last instead of a zero. */
  fun known(player: UUID): Progress? = cache[player]

  fun level(player: UUID): Int = plugin.catalog.settings.progression.levelCurve.let { Leveling.playerLevel(it, of(player).experience, of(player).kills) }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
    val store = store ?: return
    runCatching { store.load(event.uniqueId).get(10, TimeUnit.SECONDS) }
      .onSuccess { cache[event.uniqueId] = it }
      .onFailure { plugin.log.warn("Can't load the progress of ${event.name}: ${it.message}") }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) {
    cache.remove(event.player.uniqueId)
  }

  /** For players already online when the plugin starts or the database changes. */
  fun loadOnline() {
    val store = store ?: return
    Bukkit.getOnlinePlayers().forEach { player ->
      val id = player.uniqueId
      store.load(id).whenComplete { progress, error ->
        if (error != null) plugin.log.warn("Can't load the progress of ${player.name}: ${error.message}")
        else cache.putIfAbsent(id, progress)
      }
    }
  }

  /** Adds a player's experience from a kill, and the kill itself when it [counts]. The server thread. */
  fun add(player: Player, experience: Long, counts: Boolean) {
    val id = player.uniqueId
    val kills = if (counts) 1 else 0
    cache[id] = of(id).let { Progress(it.kills + kills, it.experience + experience) }
    store?.add(id, player.name, kills, experience)?.whenComplete { saved, error ->
      if (error != null) plugin.log.warn("Can't save the progress of ${player.name}: ${error.message}")
      // The database's totals win: they count kills from before a failed load, or from another server on the same database.
      else cache.computeIfPresent(id) { _, _ -> saved }
    }
    plugin.log.debug { "${player.name}: +$experience experience${if (counts) " and a kill" else ""}, level ${level(id)}" }
  }
}
