package ru.mairwunnx.mobosses.hooks

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServiceRegisterEvent
import org.bukkit.event.server.ServiceUnregisterEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID
import java.util.function.Function

/**
 * Mo’Bosses' data in WorldAPI, when it runs: the `mobosses` namespace with the best hunters, players'
 * cards and bestiaries, the bosses alive, and the counts, and events for kills, levels, achievements,
 * second phases, deaths to bosses, and full moons. It talks through WorldAPI's bridge, a `Function` in
 * Bukkit's services, with plain Java values, so nothing of WorldAPI's is on our classpath. Mo’Bosses
 * keeps its data; WorldAPI reads it on request.
 */
class WorldApiLink(private val plugin: MoBossesPlugin) : Listener {
  @Volatile private var bridge: Function<Map<String, Any?>, Any?>? = null

  val connected get() = bridge != null

  /** Finds WorldAPI's bridge and registers with it; nothing happens without WorldAPI. */
  fun connect() {
    val found = find() ?: return
    runCatching { found.apply(registration()) }
      .onSuccess {
        bridge = found
        plugin.log.info("WorldAPI found: hunters, cards, bestiaries, bosses, and events at /extensions/$NAMESPACE")
      }
      .onFailure { plugin.log.warn("WorldAPI didn't take Mo’Bosses' data: ${it.message}") }
  }

  @EventHandler
  fun onService(event: ServiceRegisterEvent) {
    if (event.provider.plugin.name == WORLD_API && event.provider.service == Function::class.java) connect()
  }

  @EventHandler
  fun onServiceGone(event: ServiceUnregisterEvent) {
    if (event.provider.plugin.name == WORLD_API) bridge = null
  }

  /** An event for WorldAPI's clients; dropped quietly while WorldAPI isn't there. */
  fun publish(event: String, data: Map<String, Any?>, player: Player? = null, world: String? = null) {
    val bridge = bridge ?: return
    val request = mutableMapOf<String, Any?>("v" to CONTRACT, "op" to "publish", "namespace" to NAMESPACE, "event" to event, "data" to data, "world" to world)
    player?.let { request["player"] = it.uniqueId; request["playerName"] = it.name }
    runCatching { bridge.apply(request) }.onFailure { plugin.log.debug { "WorldAPI didn't take $event: ${it.message}" } }
  }

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    val players = shares.entries.sortedByDescending { it.value }.map { (id, share) ->
      mapOf("uuid" to id, "name" to (Bukkit.getPlayer(id)?.name ?: Bukkit.getOfflinePlayer(id).name), "share" to share)
    }
    publish("boss.killed", boss(boss) + mapOf("experience" to experience, "players" to players), shares.maxByOrNull { it.value }?.key?.let(Bukkit::getPlayer), boss.entity.world.name)
  }

  fun levelUp(player: Player, from: Int, to: Int) = publish("player.level_up", mapOf("from" to from, "level" to to), player, player.world.name)

  fun achievement(player: Player, id: String) {
    val code = plugin.catalog.settings.achievements.language.ifBlank { plugin.languages.default }
    val title = plugin.languages.render(code, "achievements.$id.title")?.let(PlainTextComponentSerializer.plainText()::serialize) ?: id
    publish("player.achievement", mapOf("id" to id, "title" to title), player, player.world.name)
  }

  fun phaseTwo(boss: Boss) = publish("boss.phase_two", boss(boss), world = boss.entity.world.name)

  fun diedTo(player: Player, boss: Boss, lost: Long) = publish("player.died_to_boss", boss(boss) + mapOf("lost" to lost), player, player.world.name)

  fun fullMoon(world: String) = publish("full_moon", mapOf("world" to world), world = world)

  private fun registration(): Map<String, Any?> = mapOf(
    "v" to CONTRACT, "op" to "register", "plugin" to plugin, "namespace" to NAMESPACE,
    "description" to "Mo’Bosses: bosses, their hunters, and the fights between them",
    "resources" to listOf(
      resource("hunters", "The best hunters by level, with kills and experience; ?limit=1..50", public = true) { params ->
        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
        plugin.progression.top(limit).thenApply { hunters -> hunters.map { hunter(UUID.fromString(it.uuid), plugin.menuStats.name(it), it.progress) } }
      },
      resource("players/{player}", "A player's card, by UUID or name: level, experience, kills, and rank", public = true) { params ->
        val id = player(params.getValue("player"))
        plugin.progression.load(id).thenCombine(plugin.progression.rank(id)) { progress, rank ->
          hunter(id, Bukkit.getOfflinePlayer(id).name, progress) + mapOf("rank" to rank, "maxLevel" to plugin.catalog.settings.progression.levelCurve.maxLevel)
        }
      },
      resource("players/{player}/bestiary", "A player's bestiary: kills and best level by boss type and class", public = true) { params ->
        plugin.progression.bestiary(player(params.getValue("player"))).thenApply { entries ->
          entries.map { mapOf("type" to it.type, "class" to it.clazz, "kills" to it.kills, "bestLevel" to it.bestLevel) }
        }
      },
      resource(
        "leaderboards/{board}",
        "Players ranked by kills, level, or experience, shaped as WorldAPI's own leaderboards; ?limit=1..500", public = true,
      ) { params ->
        val curve = plugin.catalog.settings.progression.levelCurve
        val (title, unit, score) = when (params.getValue("board")) {
          "kills" -> Triple("Bosses killed", "bosses", { p: Progress -> p.kills.toDouble() })
          "level" -> Triple("Mo’Bosses level", "level", { p: Progress -> Leveling.score(curve, p.experience, p.kills) })
          "experience" -> Triple("Mo’Bosses experience", "xp", { p: Progress -> p.experience.toDouble() })
          else -> throw NoSuchElementException("Boards: kills, level, experience")
        }
        val board = params.getValue("board")
        plugin.progression.board(params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 10, score).thenApply { (hunters, total) ->
          mapOf(
            "id" to board, "title" to title, "unit" to unit, "total" to total,
            "entries" to hunters.mapIndexed { i, hunter ->
              val value = when (board) {
                "kills" -> hunter.progress.kills.toDouble()
                "level" -> Leveling.playerLevel(curve, hunter.progress.experience, hunter.progress.kills).toDouble()
                else -> hunter.progress.experience.toDouble()
              }
              mapOf("place" to i + 1, "player" to mapOf("uuid" to hunter.uuid, "name" to plugin.menuStats.name(hunter)), "value" to value)
            },
          )
        }
      },
      resource("stats", "Bosses alive now, spawned, and killed, in all and by type and class", public = true) {
        plugin.progression.counts().thenApply { counts ->
          mapOf(
            "alive" to PlanData.bossesAlive,
            "spawned" to counts.sumOf { it.spawned },
            "killed" to counts.sumOf { it.killed },
            "counts" to counts.map { mapOf("type" to it.type, "class" to it.clazz, "spawned" to it.spawned, "killed" to it.killed) },
          )
        }
      },
      resource("bosses", "The bosses alive in loaded chunks, with health and coordinates", public = false, mainThread = true) {
        plugin.bosses.all.filter { it.entity.isValid }.map { boss(it) + mapOf("health" to it.entity.health, "fighters" to it.damage.size) }
      },
    ),
    "events" to listOf(
      event("boss.killed", "Players killed a boss: its type, class, level, and each player's share", public = true, recorded = true),
      event("player.level_up", "A player reached a new Mo’Bosses level", public = true, recorded = true),
      event("player.achievement", "A player earned a Mo’Bosses achievement", public = true, recorded = true),
      event("player.died_to_boss", "A boss killed a player, and what progress it burned", public = true, recorded = false),
      event("boss.phase_two", "A boss entered its second phase", public = true, recorded = false),
      event("full_moon", "A full moon rose: a night of the hunt", public = true, recorded = true),
    ),
  )

  private fun resource(path: String, description: String, public: Boolean, mainThread: Boolean = false, get: (Map<String, String>) -> Any?) =
    mapOf("path" to path, "description" to description, "public" to public, "mainThread" to mainThread, "get" to Function<Map<String, String>, Any?> { get(it) })

  private fun event(name: String, description: String, public: Boolean, recorded: Boolean) =
    mapOf("name" to name, "description" to description, "public" to public, "recorded" to recorded)

  private fun hunter(id: UUID, name: String?, progress: Progress) = mapOf(
    "uuid" to id, "name" to name, "kills" to progress.kills, "experience" to progress.experience,
    "level" to Leveling.playerLevel(plugin.catalog.settings.progression.levelCurve, progress.experience, progress.kills),
  )

  /** A boss's type, class, level, and max health; its place under `location`, which anonymous clients don't get. */
  private fun boss(boss: Boss): Map<String, Any?> = mapOf(
    "uuid" to boss.uuid, "type" to boss.type.key, "class" to boss.clazz.key, "level" to boss.level, "phaseTwo" to boss.phaseTwo,
    "maxHealth" to (boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0), "location" to location(boss.entity.location),
  )

  private fun location(at: Location) = mapOf("world" to at.world.name, "x" to at.blockX, "y" to at.blockY, "z" to at.blockZ)

  /** A UUID, or the name of a player the server knows. */
  private fun player(raw: String): UUID = runCatching { UUID.fromString(raw) }.getOrNull()
    ?: Bukkit.getOfflinePlayerIfCached(raw)?.uniqueId
    ?: throw NoSuchElementException("No player named $raw has played here")

  private fun find(): Function<Map<String, Any?>, Any?>? {
    @Suppress("UNCHECKED_CAST")
    return Bukkit.getServicesManager().getRegistrations(Function::class.java).firstOrNull { it.plugin.name == WORLD_API }?.provider as? Function<Map<String, Any?>, Any?>
  }

  private companion object {
    const val WORLD_API = "WorldAPI"
    const val NAMESPACE = "mobosses"
    /** The bridge contract this speaks. */
    const val CONTRACT = 1
  }
}
