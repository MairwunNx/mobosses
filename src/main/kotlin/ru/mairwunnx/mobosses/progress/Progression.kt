package ru.mairwunnx.mobosses.progress

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
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

  @Volatile var store: Store? = null

  /** Any thread: PlaceholderAPI asks from its own. */
  fun of(player: UUID): Progress = cache[player] ?: Progress()

  fun level(player: UUID): Int = plugin.catalog.settings.progression.levelCurve.let { Formulas.playerLevel(it, of(player).experience, of(player).kills) }

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

  /** Counts a kill the player helped with. The server thread. */
  fun add(player: Player, experience: Long) {
    val id = player.uniqueId
    cache[id] = of(id).let { Progress(it.kills + 1, it.experience + experience) }
    store?.add(id, 1, experience)?.whenComplete { saved, error ->
      if (error != null) plugin.log.warn("Can't save the progress of ${player.name}: ${error.message}")
      // The database's totals win: they count kills from before a failed load, or from another server on the same database.
      else cache.computeIfPresent(id) { _, _ -> saved }
    }
    plugin.log.debug { "${player.name}: +$experience experience, level ${level(id)}" }
  }
}
