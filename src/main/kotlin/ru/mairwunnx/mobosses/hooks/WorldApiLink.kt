package ru.mairwunnx.mobosses.hooks

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID

/**
 * Mo’Bosses' data in WorldAPI, when it runs: the `mobosses` namespace with the best hunters, players'
 * cards and bestiaries, the bosses alive, and the counts, and events for kills, levels, achievements,
 * second phases, deaths to bosses, and full moons. [BossesExpansion] speaks WorldAPI's expansion API;
 * this class holds no type of it, so it loads without WorldAPI. Mo’Bosses keeps its data; WorldAPI reads
 * it on request.
 */
class WorldApiLink(private val plugin: MoBossesPlugin) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when WorldAPI is installed; nothing happens without it. */
  fun connect() {
    if (plugin.server.pluginManager.getPlugin(WORLD_API) == null) return
    try {
      BossesExpansion.register(plugin, this)
      connected = true
      plugin.log.info("WorldAPI found: hunters, cards, bestiaries, bosses, and events at /extensions/${BossesExpansion.ID}")
    } catch (error: LinkageError) {
      // WorldAPI before 3.4.0 has no expansion API.
      plugin.log.warn("WorldAPI is too old for Mo’Bosses' data; it needs 3.4.0 or newer")
    } catch (error: Exception) {
      plugin.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { BossesExpansion.unregister() }
  }

  /** An event for WorldAPI's clients; dropped quietly while WorldAPI isn't there. */
  fun publish(event: String, data: Map<String, Any?>, player: Player? = null, world: String? = null) {
    if (!connected) return
    runCatching { BossesExpansion.publish(event, data, player?.uniqueId, player?.name, world) }
      .onFailure { plugin.log.debug { "WorldAPI didn't take $event: ${it.message}" } }
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

  fun hunter(id: UUID, name: String?, progress: Progress) = mapOf(
    "uuid" to id, "name" to name, "kills" to progress.kills, "experience" to progress.experience,
    "level" to Leveling.playerLevel(plugin.catalog.settings.progression.levelCurve, progress.experience, progress.kills),
  )

  /** A boss's type, class, level, and max health; its place under `location`, which anonymous clients don't get. */
  fun boss(boss: Boss): Map<String, Any?> = mapOf(
    "uuid" to boss.uuid, "type" to boss.type.key, "class" to boss.clazz.key, "level" to boss.level, "phaseTwo" to boss.phaseTwo,
    "maxHealth" to (boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0), "location" to location(boss.entity.location),
  )

  private fun location(at: Location) = mapOf("world" to at.world.name, "x" to at.blockX, "y" to at.blockY, "z" to at.blockZ)

  /** A UUID, or the name of a player the server knows. */
  fun player(raw: String): UUID = runCatching { UUID.fromString(raw) }.getOrNull()
    ?: Bukkit.getOfflinePlayerIfCached(raw)?.uniqueId
    ?: throw NoSuchElementException("No player named $raw has played here")

  private companion object {
    const val WORLD_API = "WorldAPI"
  }
}
