package ru.mairwunnx.mobosses.modcore.hooks

import com.mairwunnx.worldapi.expansion.Event
import com.mairwunnx.worldapi.expansion.EventType
import com.mairwunnx.worldapi.expansion.ExpansionApi
import com.mairwunnx.worldapi.expansion.Resource
import com.mairwunnx.worldapi.expansion.WorldApiExpansion
import com.mairwunnx.worldapi.expansion.WorldApiExpansions
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID

/**
 * The `mobosses` namespace in WorldAPI, through its expansion API (`com.mairwunnx:worldapi-expansion`). WorldAPI
 * brings the API's classes, so this class loads only when it runs; [WorldApiLink] is the side the rest of the
 * mod calls. The same resources and events as on Paper: everything public but the bosses alive, which carry
 * coordinates.
 */
class BossesExpansion private constructor(private val mod: MoBosses, private val link: WorldApiLink) : WorldApiExpansion {
  override fun id() = ID

  override fun requiresApi(): String = ExpansionApi.VERSION

  override fun name() = "Mo’Bosses"

  override fun description() = "Mo’Bosses: bosses, their hunters, and the fights between them"

  override fun resources(): List<Resource> = listOf(
    Resource.of("hunters", "The best hunters by level, with kills and experience; ?limit=1..50") { request ->
      val limit = request.query("limit")?.toIntOrNull()?.coerceIn(1, 50) ?: 10
      mod.progression.top(limit).thenApply { hunters -> hunters.map { link.hunter(UUID.fromString(it.uuid), mod.nameOf(it), it.progress) } }
    }.publicly(),
    Resource.of("players/{player}", "A player's card, by UUID or name: level, experience, kills, and rank") { request ->
      val id = link.player(request.path("player"))
      mod.progression.load(id).thenCombine(mod.progression.rank(id)) { progress, rank ->
        link.hunter(id, mod.nameOf(id), progress) + mapOf("rank" to rank, "maxLevel" to mod.catalog.settings.progression.levelCurve.maxLevel)
      }
    }.publicly(),
    Resource.of("players/{player}/bestiary", "A player's bestiary: kills and best level by boss type and class") { request ->
      mod.progression.bestiary(link.player(request.path("player"))).thenApply { entries ->
        entries.map { mapOf("type" to it.type, "class" to it.clazz, "kills" to it.kills, "bestLevel" to it.bestLevel) }
      }
    }.publicly(),
    Resource.of("leaderboards/{board}", "Players ranked by kills, level, or experience, shaped as WorldAPI's own leaderboards; ?limit=1..500") { request ->
      val board = request.path("board")
      val curve = mod.catalog.settings.progression.levelCurve
      val (title, unit, score) = when (board) {
        "kills" -> Triple("Bosses killed", "bosses", { p: Progress -> p.kills.toDouble() })
        "level" -> Triple("Mo’Bosses level", "level", { p: Progress -> Leveling.score(curve, p.experience, p.kills) })
        "experience" -> Triple("Mo’Bosses experience", "xp", { p: Progress -> p.experience.toDouble() })
        else -> throw NoSuchElementException("Boards: kills, level, experience")
      }
      mod.progression.board(request.query("limit")?.toIntOrNull()?.coerceIn(1, 500) ?: 10, score).thenApply { (hunters, total) ->
        mapOf(
          "id" to board, "title" to title, "unit" to unit, "total" to total,
          "entries" to hunters.mapIndexed { i, hunter ->
            val value = when (board) {
              "kills" -> hunter.progress.kills.toDouble()
              "level" -> Leveling.playerLevel(curve, hunter.progress.experience, hunter.progress.kills).toDouble()
              else -> hunter.progress.experience.toDouble()
            }
            mapOf("place" to i + 1, "player" to mapOf("uuid" to hunter.uuid, "name" to mod.nameOf(hunter)), "value" to value)
          },
        )
      }
    }.publicly(),
    Resource.of("stats", "Bosses alive now, spawned, and killed, in all and by type and class") {
      mod.progression.counts().thenApply { counts ->
        mapOf(
          "alive" to mod.bosses.all.size,
          "spawned" to counts.sumOf { it.spawned },
          "killed" to counts.sumOf { it.killed },
          "counts" to counts.map { mapOf("type" to it.type, "class" to it.clazz, "spawned" to it.spawned, "killed" to it.killed) },
        )
      }
    }.publicly(),
    // Coordinates, so only for clients with a token.
    Resource.of("bosses", "The bosses alive in loaded chunks, with health and coordinates") {
      mod.bosses.all.filter { it.entity.isAlive }.map { link.boss(it) + mapOf("health" to it.entity.health, "fighters" to it.damage.size) }
    }.onServerThread(),
  )

  override fun events(): List<EventType> = listOf(
    EventType.of("boss.killed", "Players killed a boss: its type, class, level, and each player's share").publicly().recorded(),
    EventType.of("player.level_up", "A player reached a new Mo’Bosses level").publicly().recorded(),
    EventType.of("player.achievement", "A player earned a Mo’Bosses achievement").publicly().recorded(),
    EventType.of("player.died_to_boss", "A boss killed a player, and what progress it burned").publicly(),
    EventType.of("boss.phase_two", "A boss entered its second phase").publicly(),
    EventType.of("full_moon", "A full moon rose: a night of the hunt").publicly().recorded(),
  )

  companion object {
    const val ID = "mobosses"

    fun register(mod: MoBosses, link: WorldApiLink) = WorldApiExpansions.register(BossesExpansion(mod, link))

    fun unregister() = WorldApiExpansions.unregister(ID)

    fun publish(event: String, data: Map<String, Any?>, player: UUID?, playerName: String?, world: String?) {
      var built = Event.of(event, data).world(world)
      if (player != null) built = built.player(player, playerName)
      WorldApiExpansions.publish(ID, built)
    }
  }
}
