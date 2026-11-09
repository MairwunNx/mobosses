package ru.mairwunnx.mobosses.modcore.hooks

import com.mairwunnx.worldapi.expansion.v2.Expansion
import com.mairwunnx.worldapi.expansion.v2.Place
import com.mairwunnx.worldapi.expansion.v2.PlayerRef
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.progress.Deaths
import ru.mairwunnx.mobosses.progress.Progress
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * The `mobosses` namespace in WorldAPI, through its expansion API 2 (`com.mairwunnx:worldapi-expansion`, package `v2`).
 * WorldAPI brings the API's classes since 4.5.0, so this object loads only when it runs; [WorldApiLink] is the side the
 * rest of the mod calls. The same resources and events as on Paper: everything public but the bosses alive. Places are
 * [Place]s, which WorldAPI leaves out for clients that mustn't see coordinates. Worlds go by their dimension's id, as
 * WorldAPI names them on the mods. The JSON keeps every field API 1 wrote.
 */
object BossesExpansion : Expansion("mobosses", "Mo’Bosses", "Mo’Bosses: bosses, their hunters, and the fights between them") {
  /** The server the handlers read, set before [register]: a new one for each world in singleplayer. */
  @Volatile lateinit var mod: MoBosses

  val limit = integer("limit", "How many players", 1, 500, 10)
  val player = player("player", "A player's name or UUID")
  val board = choice("board", "What the board ranks", Board::class.java, Board.KILLS)

  val hunters = resource("hunters", "The best hunters by level, with kills and experience") { req -> topHunters(req[limit]) }.query(limit).returnsListOf(Hunter::class.java).publicly()
  val card = resource("players/{player}", "A player's card: level, experience, kills, deaths to bosses and the streak of them, damage dealt, and rank") { req -> cardOf(req[player]) }.returns(Card::class.java).publicly()
  val bestiary = resource("players/{player}/bestiary", "A player's bestiary: kills and best level by boss type and class") { req -> bestiaryOf(req[player]) }.returnsListOf(Bestiary::class.java).publicly()
  val leaderboards = resource("leaderboards/{board}", "Players ranked by kills, level, experience, deaths to bosses, or damage to them, shaped as WorldAPI's own leaderboards") { req -> boardOf(req[board], req[limit]) }.query(limit).returns(Leaderboard::class.java).publicly()
  val stats = resource("stats", "Bosses alive now, spawned, killed, and the players they killed, in all and by type and class, and how often each ability went off") { counts() }.returns(Stats::class.java).publicly()
  val bosses = resource("bosses", "The bosses alive in loaded chunks, with health and coordinates") { alive() }.returnsListOf(AliveBoss::class.java).onServerThread()

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

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    val players = shares.entries.sortedByDescending { it.value }.map { (id, share) -> Share(id, mod.nameOf(id), share) }
    val top = players.firstOrNull()?.let { PlayerRef(it.uuid, it.name) }
    killed.publish(BossKilled(top, boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity), experience, players))
  }

  fun levelUp(player: ServerPlayer, from: Int, to: Int) = leveledUp.publish(LevelUp(ref(player), from, to, place(player)))

  fun achievement(player: ServerPlayer, id: String) {
    val code = mod.catalog.settings.achievements.language.ifBlank { mod.languages.default }
    val title = mod.languages.render(code, "achievements.$id.title")?.let(PlainTextComponentSerializer.plainText()::serialize) ?: id
    earned.publish(AchievementEarned(ref(player), id, title, place(player)))
  }

  fun phaseTwo(boss: Boss) = secondPhase.publish(PhaseTwo(boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity)))

  /** `streak` and `demoted` came in 2.20: the deaths in a row so far, and whether this one cost a level. */
  fun diedTo(player: ServerPlayer, boss: Boss, death: Deaths.Death) = died.publish(
    DiedToBoss(ref(player), boss.uuid, boss.type.key, boss.clazz.key, boss.level, boss.phaseTwo, maxHealth(boss), place(boss.entity), death.lost, death.after.streak, death.demoted),
  )

  fun demoted(player: ServerPlayer, from: Int, to: Int, streak: Int) = levelLost.publish(Demoted(ref(player), from, to, streak, place(player)))

  fun fullMoon(world: String) = moonRose.publish(FullMoon(world))

  private fun topHunters(limit: Int) = mod.progression.top(limit).thenApply { hunters -> hunters.map { hunter(UUID.fromString(it.uuid), mod.nameOf(it), it.progress) } }

  private fun cardOf(player: PlayerRef): CompletableFuture<Card> {
    val id = player.uuid()
    return mod.progression.load(id).thenCombine(mod.progression.rank(id)) { progress, rank ->
      val it = hunter(id, mod.nameOf(id) ?: player.name(), progress)
      Card(id, it.name, it.kills, it.experience, it.deaths, it.streak, it.damage, it.level, rank, mod.catalog.settings.progression.levelCurve.maxLevel)
    }
  }

  private fun bestiaryOf(player: PlayerRef) = mod.progression.bestiary(player.uuid()).thenApply { entries -> entries.map { Bestiary(it.type, it.clazz, it.kills, it.bestLevel) } }

  private fun boardOf(board: Board, limit: Int): CompletableFuture<Leaderboard> {
    val curve = mod.catalog.settings.progression.levelCurve
    return mod.progression.board(limit, board.score).thenApply { (hunters, total) ->
      val entries = hunters.mapIndexed { i, hunter ->
        val value = when (board) {
          Board.KILLS -> hunter.progress.kills.toDouble()
          Board.LEVEL -> Leveling.playerLevel(curve, hunter.progress.experience).toDouble()
          Board.EXPERIENCE -> hunter.progress.experience.toDouble()
          Board.DEATHS -> hunter.progress.deaths.toDouble()
          Board.DAMAGE -> hunter.progress.damage
        }
        Entry(i + 1, PlayerRef(UUID.fromString(hunter.uuid), mod.nameOf(hunter)), value)
      }
      Leaderboard(board.name.lowercase(Locale.ROOT), board.title, board.unit, total, entries)
    }
  }

  private fun counts() = mod.progression.counts().thenCombine(mod.progression.casts()) { counts, casts ->
    val each = counts.map { Count(it.type, it.clazz, it.spawned, it.killed, it.playerKills) }
    Stats(mod.bosses.all.size, counts.sumOf { it.spawned }, counts.sumOf { it.killed }, counts.sumOf { it.playerKills }, each, casts.toSortedMap())
  }

  private fun alive() = mod.bosses.all.filter { it.entity.isAlive }.map {
    AliveBoss(it.uuid, it.type.key, it.clazz.key, it.level, it.phaseTwo, maxHealth(it), place(it.entity), it.entity.health.toDouble(), it.damage.size)
  }

  private fun hunter(id: UUID, name: String?, progress: Progress) =
    Hunter(id, name, progress.kills, progress.experience, progress.deaths, progress.streak, progress.damage, Leveling.playerLevel(mod.catalog.settings.progression.levelCurve, progress.experience))

  private fun ref(player: ServerPlayer) = PlayerRef(player.uuid, player.plainTextName)

  private fun maxHealth(boss: Boss) = boss.entity.maxHealth.toDouble()

  /** The block a boss or a player stands in, as API 1 wrote it. */
  private fun place(at: Entity) = Place(mod.worldApi.world(at), at.blockX.toDouble(), at.blockY.toDouble(), at.blockZ.toDouble())
}
