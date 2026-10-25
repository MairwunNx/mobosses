package ru.mairwunnx.mobosses.modcore.hooks

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID

/**
 * Mo’Bosses' data in WorldAPI, when it runs, as on Paper: the `mobosses` namespace and its events. [BossesExpansion]
 * speaks WorldAPI's expansion API; this class holds no type of it, so it loads without WorldAPI. Worlds go by their
 * dimension's id, as WorldAPI names them on the mods.
 */
class WorldApiLink(private val mod: MoBosses) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when WorldAPI is installed; nothing happens without it. */
  fun connect() {
    if (!mod.loader.loaded(WORLD_API)) return
    try {
      BossesExpansion.register(mod, this)
      connected = true
      mod.log.info("WorldAPI found: hunters, cards, bestiaries, bosses, and events at /extensions/${BossesExpansion.ID}")
    } catch (error: LinkageError) {
      mod.log.warn("WorldAPI is too old for Mo’Bosses' data: ${error.message}")
    } catch (error: Exception) {
      mod.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { BossesExpansion.unregister() }
  }

  /** An event for WorldAPI's clients; dropped quietly while WorldAPI isn't there. */
  fun publish(event: String, data: Map<String, Any?>, player: ServerPlayer? = null, world: String? = null) {
    if (!connected) return
    runCatching { BossesExpansion.publish(event, data, player?.uuid, player?.plainTextName, world) }
      .onFailure { mod.log.debug { "WorldAPI didn't take $event: ${it.message}" } }
  }

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    val players = shares.entries.sortedByDescending { it.value }.map { (id, share) -> mapOf("uuid" to id, "name" to mod.nameOf(id), "share" to share) }
    publish("boss.killed", boss(boss) + mapOf("experience" to experience, "players" to players), shares.maxByOrNull { it.value }?.key?.let(mod.server.playerList::getPlayer), world(boss.entity))
  }

  fun levelUp(player: ServerPlayer, from: Int, to: Int) = publish("player.level_up", mapOf("from" to from, "level" to to), player, world(player))

  fun achievement(player: ServerPlayer, id: String) {
    val code = mod.catalog.settings.achievements.language.ifBlank { mod.languages.default }
    val title = mod.languages.render(code, "achievements.$id.title")?.let(PlainTextComponentSerializer.plainText()::serialize) ?: id
    publish("player.achievement", mapOf("id" to id, "title" to title), player, world(player))
  }

  fun phaseTwo(boss: Boss) = publish("boss.phase_two", boss(boss), world = world(boss.entity))

  fun diedTo(player: ServerPlayer, boss: Boss, lost: Long) = publish("player.died_to_boss", boss(boss) + mapOf("lost" to lost), player, world(player))

  fun fullMoon(world: String) = publish("full_moon", mapOf("world" to world), world = world)

  fun hunter(id: UUID, name: String?, progress: Progress) = mapOf(
    "uuid" to id, "name" to name, "kills" to progress.kills, "experience" to progress.experience,
    "level" to Leveling.playerLevel(mod.catalog.settings.progression.levelCurve, progress.experience),
  )

  /** A boss's type, class, level, and max health; its place under `location`, which anonymous clients don't get. */
  fun boss(boss: Boss): Map<String, Any?> = mapOf(
    "uuid" to boss.uuid, "type" to boss.type.key, "class" to boss.clazz.key, "level" to boss.level, "phaseTwo" to boss.phaseTwo,
    "maxHealth" to boss.entity.maxHealth.toDouble(), "location" to location(boss.entity),
  )

  private fun location(at: Entity) = mapOf("world" to world(at), "x" to at.blockX, "y" to at.blockY, "z" to at.blockZ)

  /** WorldAPI names a world on the mods by its dimension: `overworld`, or another mod's as `namespace:path`. */
  fun world(entity: Entity): String {
    val id = entity.level().dimension().identifier()
    return if (id.namespace == "minecraft") id.path else id.toString()
  }

  /** A UUID, or the name of a player the server knows. */
  fun player(raw: String): UUID = runCatching { UUID.fromString(raw) }.getOrNull()
    ?: mod.idOf(raw)
    ?: throw NoSuchElementException("No player named $raw has played here")

  private companion object {
    const val WORLD_API = "worldapi"
  }
}
