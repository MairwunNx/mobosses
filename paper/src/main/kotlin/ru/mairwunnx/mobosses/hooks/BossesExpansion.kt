package ru.mairwunnx.mobosses.hooks

import com.mairwunnx.worldapi.expansion.v2.Expansion
import com.mairwunnx.worldapi.expansion.v2.Place
import com.mairwunnx.worldapi.expansion.v2.PlayerRef
import com.mairwunnx.worldapi.expansion.v2.Resource
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.progress.Deaths
import ru.mairwunnx.mobosses.progress.Progress
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * The `mobosses` namespace in WorldAPI, through its expansion API 2 (`com.mairwunnx:worldapi-expansion`, package `v2`).
 * WorldAPI brings the API's classes since 4.5.0, so these classes load only when it runs. This one holds the resources
 * and events, [BossesExpansion] registers them alone on WorldAPI 4.5, and [BossesActions] adds API 2.1's actions on 4.6; [WorldApiLink] is the side the
 * rest of the plugin calls. Everything is public but the bosses alive. Places are [Place]s, which WorldAPI leaves out
 * for clients that mustn't see coordinates. The JSON keeps every field API 1 wrote.
 */
abstract class BossesData : Expansion("mobosses", "Mo’Bosses", "Mo’Bosses: bosses, their hunters, and the fights between them") {
  /** The plugin the handlers read, set before [register]. */
  @Volatile lateinit var plugin: MoBossesPlugin

  val limit = integer("limit", "How many players", 1, 500, 10)
  val player = player("player", "A player's name or UUID")
  val board = choice("board", "What the board ranks", Board::class.java, Board.KILLS)

  val hunters = capped(resource("hunters", "The best hunters by level, with kills and experience, at most $HUNTERS") { req -> topHunters(req[limit].coerceAtMost(HUNTERS)) }.query(limit).returnsListOf(Hunter::class.java).publicly())
  val card = resource("players/{player}", "A player's card: level, experience, kills, deaths to bosses and the streak of them, damage dealt, and rank") { req -> cardOf(req[player]) }.returns(Card::class.java).publicly()
  val bestiary = resource("players/{player}/bestiary", "A player's bestiary: kills and best level by boss type and class") { req -> bestiaryOf(req[player]) }.returnsListOf(Bestiary::class.java).publicly()
  val leaderboards = resource("leaderboards/{board}", "Players ranked by kills, level, experience, deaths to bosses, or damage to them, shaped as WorldAPI's own leaderboards") { req -> boardOf(req[board], req[limit]) }.query(limit).returns(Leaderboard::class.java).publicly()
  val stats = resource("stats", "Bosses alive now, spawned, killed, and the players they killed, in all and by type and class, and how often each ability went off") { counts() }.returns(Stats::class.java).publicly()
  val bosses = resource("bosses", "The bosses alive in loaded chunks, with health and coordinates: uuid is the id DELETE bosses/{boss} takes") { alive() }.returnsListOf(AliveBoss::class.java).onServerThread()

  val killed = event("boss.killed", BossKilled::class.java, "Players killed a boss: its type, class, level, and each player's share").publicly().recorded()
  val leveledUp = event("player.level_up", LevelUp::class.java, "A player reached a new Mo’Bosses level").publicly().recorded()
  val earned = event("player.achievement", AchievementEarned::class.java, "A player earned a Mo’Bosses achievement").publicly().recorded()
  val died = event("player.died_to_boss", DiedToBoss::class.java, "A boss killed a player, what progress it burned, and their deaths in a row").publicly()
  val levelLost = event("player.demoted", Demoted::class.java, "Deaths to bosses in a row cost a player a Mo’Bosses level").publicly().recorded()
  val secondPhase = event("boss.phase_two", PhaseTwo::class.java, "A boss entered its second phase").publicly()
  val moonRose = event("full_moon", FullMoon::class.java, "A full moon rose: a night of the hunt").publicly().recorded()

  /** What a leaderboard ranks, by its name in the path. */
  enum class Board(val title: String, val unit: String, val score: (Progress) -> Double) {
    KILLS("Bosses killed", "bosses", { it.kills.toDouble() }),
    LEVEL("Mo’Bosses level", "level", { it.experience.toDouble() }),
    EXPERIENCE("Mo’Bosses experience", "xp", { it.experience.toDouble() }),
    DEATHS("Deaths to bosses", "deaths", { it.deaths.toDouble() }),
    DAMAGE("Damage to bosses", "hp", { it.damage }),
  }

  @JvmRecord data class Hunter(val uuid: UUID, val name: String?, val kills: Int, val experience: Long, val deaths: Int, val streak: Int, val damage: Double, val level: Int)

  @JvmRecord data class Card(val uuid: UUID, val name: String?, val kills: Int, val experience: Long, val deaths: Int, val streak: Int, val damage: Double, val level: Int, val rank: Int?, val maxLevel: Int)

  @JvmRecord data class Bestiary(val type: String, val `class`: String, val kills: Long, val bestLevel: Int)

  @JvmRecord data class Entry(val place: Int, val player: PlayerRef, val value: Double)

  @JvmRecord data class Leaderboard(val id: String, val title: String, val unit: String, val total: Int, val entries: List<Entry>)

  @JvmRecord data class Count(val type: String, val `class`: String, val spawned: Long, val killed: Long, val playerKills: Long)

  @JvmRecord data class Stats(val alive: Int, val spawned: Long, val killed: Long, val playerKills: Long, val counts: List<Count>, val abilities: Map<String, Long>)

  @JvmRecord data class AliveBoss(val uuid: UUID, val type: String, val `class`: String, val level: Int, val phaseTwo: Boolean, val maxHealth: Double, val location: Place, val health: Double, val fighters: Int)

  @JvmRecord data class Share(val uuid: UUID, val name: String?, val share: Double)

  /** `player`, the biggest share, came with API 2: WorldAPI files the event under the first player among its fields. */
  @JvmRecord data class BossKilled(val player: PlayerRef?, val uuid: UUID, val type: String, val `class`: String, val level: Int, val phaseTwo: Boolean, val maxHealth: Double, val location: Place, val experience: Int, val players: List<Share>)

  /** `player` and `location` came with API 2: WorldAPI takes the event's player and world from them, as API 1 was told. */
  @JvmRecord data class LevelUp(val player: PlayerRef, val from: Int, val level: Int, val location: Place)

  @JvmRecord data class AchievementEarned(val player: PlayerRef, val id: String, val title: String, val location: Place)

  @JvmRecord data class DiedToBoss(val player: PlayerRef, val uuid: UUID, val type: String, val `class`: String, val level: Int, val phaseTwo: Boolean, val maxHealth: Double, val location: Place, val lost: Long, val streak: Int, val demoted: Boolean)

  @JvmRecord data class Demoted(val player: PlayerRef, val from: Int, val level: Int, val streak: Int, val location: Place)

  @JvmRecord data class PhaseTwo(val uuid: UUID, val type: String, val `class`: String, val level: Int, val phaseTwo: Boolean, val maxHealth: Double, val location: Place)

  @JvmRecord data class FullMoon(val world: String)

  /** `hunters` with its own bounds for `limit`, which WorldAPI 4.6 takes: [BossesActions] sets them, and 4.5 keeps the shared ones. */
  protected open fun capped(hunters: Resource): Resource = hunters

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    val players = shares.entries.sortedByDescending { it.value }.map { (id, share) -> Share(id, Bukkit.getPlayer(id)?.name ?: Bukkit.getOfflinePlayer(id).name, share) }
    val top = players.firstOrNull()?.let { PlayerRef(it.uuid, it.name) }
    killed.publish(BossKilled(top, boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity.location), experience, players))
  }

  fun levelUp(player: Player, from: Int, to: Int) = leveledUp.publish(LevelUp(ref(player), from, to, place(player.location)))

  fun achievement(player: Player, id: String) {
    val code = plugin.catalog.settings.achievements.language.ifBlank { plugin.languages.default }
    val title = plugin.languages.render(code, "achievements.$id.title")?.let(PlainTextComponentSerializer.plainText()::serialize) ?: id
    earned.publish(AchievementEarned(ref(player), id, title, place(player.location)))
  }

  fun phaseTwo(boss: Boss) = secondPhase.publish(PhaseTwo(boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity.location)))

  /** `streak` and `demoted` came in 2.20: the deaths in a row so far, and whether this one cost a level. */
  fun diedTo(player: Player, boss: Boss, death: Deaths.Death) = died.publish(
    DiedToBoss(ref(player), boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity.location), death.lost, death.after.streak, death.demoted),
  )

  fun demoted(player: Player, from: Int, to: Int, streak: Int) = levelLost.publish(Demoted(ref(player), from, to, streak, place(player.location)))

  fun fullMoon(world: String) = moonRose.publish(FullMoon(world))

  private fun topHunters(limit: Int) = plugin.progression.top(limit).thenApply { hunters -> hunters.map { hunter(UUID.fromString(it.uuid), plugin.menuStats.name(it), it.progress) } }

  private fun cardOf(player: PlayerRef): CompletableFuture<Card> {
    val id = player.uuid()
    return plugin.progression.load(id).thenCombine(plugin.progression.rank(id)) { progress, rank ->
      val it = hunter(id, Bukkit.getOfflinePlayer(id).name ?: player.name(), progress)
      Card(id, it.name, it.kills, it.experience, it.deaths, it.streak, it.damage, it.level, rank, plugin.catalog.settings.progression.levelCurve.maxLevel)
    }
  }

  private fun bestiaryOf(player: PlayerRef) = plugin.progression.bestiary(player.uuid()).thenApply { entries -> entries.map { Bestiary(it.type, it.clazz, it.kills, it.bestLevel) } }

  private fun boardOf(board: Board, limit: Int): CompletableFuture<Leaderboard> {
    val curve = plugin.catalog.settings.progression.levelCurve
    return plugin.progression.board(limit, board.score).thenApply { (hunters, total) ->
      val entries = hunters.mapIndexed { i, hunter ->
        val value = when (board) {
          Board.KILLS -> hunter.progress.kills.toDouble()
          Board.LEVEL -> Leveling.playerLevel(curve, hunter.progress.experience).toDouble()
          Board.EXPERIENCE -> hunter.progress.experience.toDouble()
          Board.DEATHS -> hunter.progress.deaths.toDouble()
          Board.DAMAGE -> hunter.progress.damage
        }
        Entry(i + 1, PlayerRef(UUID.fromString(hunter.uuid), plugin.menuStats.name(hunter)), value)
      }
      Leaderboard(board.name.lowercase(Locale.ROOT), board.title, board.unit, total, entries)
    }
  }

  private fun counts() = plugin.progression.counts().thenCombine(plugin.progression.casts()) { counts, casts ->
    val each = counts.map { Count(it.type, it.clazz, it.spawned, it.killed, it.playerKills) }
    Stats(PlanData.bossesAlive, counts.sumOf { it.spawned }, counts.sumOf { it.killed }, counts.sumOf { it.playerKills }, each, casts.toSortedMap())
  }

  private fun alive() = plugin.bosses.all.filter { it.entity.isValid }.map {
    AliveBoss(it.uuid, it.type.key, it.clazz.key, it.level, it.phaseTwo, maxHealth(it), place(it.entity.location), it.entity.health, it.damage.size)
  }

  private fun hunter(id: UUID, name: String?, progress: Progress) =
    Hunter(id, name, progress.kills, progress.experience, progress.deaths, progress.streak, progress.damage, Leveling.playerLevel(plugin.catalog.settings.progression.levelCurve, progress.experience))

  private fun ref(player: Player) = PlayerRef(player.uniqueId, player.name)

  protected fun maxHealth(boss: Boss) = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0

  /** The block a boss or a player stands in, as API 1 wrote it. */
  protected fun place(at: Location) = Place(at.world.name, at.blockX.toDouble(), at.blockY.toDouble(), at.blockZ.toDouble())

  companion object {
    /** The most hunters `hunters` lists, as before 2.22.0; the boards take up to 500. */
    const val HUNTERS = 50

    /** The namespace WorldAPI took: [BossesActions] where WorldAPI takes actions, else [BossesExpansion]. */
    @Volatile lateinit var current: BossesData
      private set

    /**
     * Registers the namespace; true when it came with its actions. WorldAPI 4.5 gets the resources and events alone:
     * [BossesActions] would fail to build there, and an object that failed to build stays broken, so it's picked only
     * where `action` exists.
     */
    fun connect(plugin: MoBossesPlugin): Boolean {
      val actions = takesActions()
      current = if (actions) withActions(plugin) else readsOnly(plugin)
      return actions
    }

    /** Whether the expansion API WorldAPI brings has actions and a resource's own bounds (2.1, WorldAPI 4.6.0). */
    private fun takesActions() = Expansion::class.java.declaredMethods.any { it.name == "action" } && Resource::class.java.methods.any { it.name == "between" }

    // One object per method, so the JVM never has to build the one it doesn't use.
    private fun withActions(plugin: MoBossesPlugin): BossesData = BossesActions.also { it.plugin = plugin; it.register() }

    private fun readsOnly(plugin: MoBossesPlugin): BossesData = BossesExpansion.also { it.plugin = plugin; it.register() }
  }
}

/** The namespace for WorldAPI 4.5: its resources and events alone. */
object BossesExpansion : BossesData()
