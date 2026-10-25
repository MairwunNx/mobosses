package ru.mairwunnx.mobosses.progress

import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.algebra.LegacyLevels
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Players' Mo’Bosses levels. Progress loads while a player logs in, off the server thread, and
 * stays cached while they're online; each kill saves in the background. Without a database (it
 * failed to open) progress lives only until the player leaves. A platform hands in its log, the
 * settings it runs, the players online, and a way back to the server thread, and calls [login] and
 * [quit] from its events.
 */
class Progression(
  private val log: Log,
  private val settings: () -> Settings,
  private val online: () -> List<UUID>,
  private val onMain: (Runnable) -> Unit,
) {
  private val cache = ConcurrentHashMap<UUID, Progress>()

  @Volatile var store: Store? = null; private set
  private var database: Settings.Database? = null

  /** Opens the database off the server thread, since a remote one can take a while; only when its settings changed. */
  fun open(next: Settings.Database, folder: File) {
    if (next == database) return
    database = next
    val old = store
    store = null
    Thread({
      old?.close()
      try {
        val curve = settings().progression.levelCurve
        store = Store(next, folder) { LegacyLevels.carry(curve, it.experience, it.kills) }.also {
          log.info("Progress is stored in ${it.description}")
          if (it.carried > 0) log.info("Carried ${it.carried} players over to 2.11.0's levels, each at the level they had; the rows as they were are in player_progress_before_2_11")
        }
        loadOnline()
      } catch (e: Exception) {
        log.error("Can't open the database; players' progress lasts only until they leave: ${e.message}")
      }
    }, "Mo’Bosses database").start()
  }

  fun close() {
    store?.close()
    store = null
    database = null
  }

  /** Any thread: PlaceholderAPI asks from its own. */
  fun of(player: UUID): Progress = cache[player] ?: Progress()

  /** A player's record straight from the database, for players who aren't online. */
  fun load(player: UUID): CompletableFuture<Progress> = store?.load(player) ?: CompletableFuture.completedFuture(Progress())

  /** The best hunters; empty without a database. Any thread. */
  fun top(limit: Int): CompletableFuture<List<Hunter>> = store?.top(limit, score()) ?: CompletableFuture.completedFuture(emptyList())

  /** A board by any [score] of progress, with how many players are on it; empty without a database. */
  fun board(limit: Int, score: (Progress) -> Double): CompletableFuture<Pair<List<Hunter>, Int>> =
    store?.board(limit, score) ?: CompletableFuture.completedFuture(emptyList<Hunter>() to 0)

  /** A player's place on the board; null without a database. */
  fun rank(player: UUID): CompletableFuture<Int?> = store?.rank(player, score())?.thenApply { it } ?: CompletableFuture.completedFuture(null)

  /** The board ranks by experience, what levels grow from, so it agrees with them. */
  private fun score(): (Progress) -> Double = { it.experience.toDouble() }

  /** Bosses by type and class; empty without a database. */
  fun counts(): CompletableFuture<List<BossCount>> = store?.counts() ?: CompletableFuture.completedFuture(emptyList())

  /** Notes a player's kill for their bestiary; the future has the whole bestiary after it, empty without a database. */
  fun bestiaryKill(player: UUID, type: String, clazz: String, level: Int): CompletableFuture<List<BestiaryEntry>> =
    store?.bestiaryKill(player, type, clazz, level) ?: CompletableFuture.completedFuture(emptyList())

  fun bestiary(player: UUID): CompletableFuture<List<BestiaryEntry>> = store?.bestiary(player) ?: CompletableFuture.completedFuture(emptyList())

  /** Counts a boss that spawned, or that players killed, for the statistics. */
  fun count(type: String, clazz: String, killed: Boolean) {
    store?.count(type, clazz, killed)
  }

  /** Null for a player whose progress isn't loaded, so Plan keeps what it stored last instead of a zero. */
  fun known(player: UUID): Progress? = cache[player]

  fun level(player: UUID): Int = Leveling.playerLevel(settings().progression.levelCurve, of(player).experience)

  /** A player logging in, off the server thread: their progress loads before they join. */
  fun login(player: UUID, name: String) {
    val store = store ?: return
    runCatching { store.load(player).get(10, TimeUnit.SECONDS) }
      .onSuccess { cache[player] = it }
      .onFailure { log.warn("Can't load the progress of $name: ${it.message}") }
  }

  fun quit(player: UUID) {
    cache.remove(player)
  }

  /** For players already online when the plugin starts or the database changes. */
  fun loadOnline() {
    val store = store ?: return
    online().forEach { id ->
      store.load(id).whenComplete { progress, error ->
        if (error != null) log.warn("Can't load the progress of $id: ${error.message}")
        else cache.putIfAbsent(id, progress)
      }
    }
  }

  /**
   * Wipes a player's progress, online or not. The cache empties at once, and again once the database
   * is done: a kill still saving would otherwise put the old totals back.
   */
  fun reset(player: UUID): CompletableFuture<Unit> {
    cache.computeIfPresent(player) { _, _ -> Progress() }
    val store = store ?: return CompletableFuture.completedFuture(Unit)
    return store.reset(player).whenComplete { _, error -> if (error == null) cache.computeIfPresent(player) { _, _ -> Progress() } }
  }

  /**
   * After a player died to a boss: their experience drops to the least that keeps their level, so the
   * way to the next one starts over. [done] gets what they lost, on the server thread.
   */
  fun penalize(player: UUID, name: String, done: (lost: Long) -> Unit) {
    val curve = settings().progression.levelCurve
    val change = { p: Progress -> p.copy(experience = Leveling.levelFloor(curve, p.experience)) }
    val store = store
    if (store == null) {
      val before = of(player)
      cache.computeIfPresent(player) { _, p -> change(p) }
      return (before.experience - of(player).experience).takeIf { it > 0 }?.let(done) ?: Unit
    }
    store.update(player, name, change).whenComplete { result, error ->
      if (error != null) return@whenComplete log.warn("Can't save the progress of $name: ${error.message}")
      val (before, after) = result
      cache.computeIfPresent(player) { _, _ -> after }
      val lost = before.experience - after.experience
      if (lost > 0) onMain(Runnable { done(lost) })
    }
  }

  /** Adds a player's experience from a kill, and the kill itself when it [counts]. The server thread. */
  fun add(player: UUID, name: String, experience: Long, counts: Boolean) {
    val kills = if (counts) 1 else 0
    cache[player] = of(player).let { Progress(it.kills + kills, it.experience + experience) }
    store?.add(player, name, kills, experience)?.whenComplete { saved, error ->
      if (error != null) log.warn("Can't save the progress of $name: ${error.message}")
      // The database's totals win: they count kills from before a failed load, or from another server on the same database.
      else cache.computeIfPresent(player) { _, _ -> saved }
    }
    log.debug { "$name: +$experience experience${if (counts) " and a kill" else ""}, level ${level(player)}" }
  }
}
