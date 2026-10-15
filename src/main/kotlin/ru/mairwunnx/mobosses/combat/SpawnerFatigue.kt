package ru.mairwunnx.mobosses.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import kotlin.math.pow

/**
 * Bosses from spawners give less to a player who keeps killing them in one spot. Each such kill near
 * here counts, fading by half every `half_life_minutes`; past `free` of them, each one more takes
 * `step` off, down to `floor`. The kills live on the player, so a relog doesn't wipe them.
 */
class SpawnerFatigue(private val plugin: MoBossesPlugin) {
  private class Kill(val world: String, val x: Int, val z: Int, val at: Long)

  /** What the reward for a spawner boss killed at [where] is multiplied by, and the kill noted. */
  fun take(player: Player, where: Location, now: Long = System.currentTimeMillis()): Double {
    val cfg = plugin.catalog.settings.spawnerFatigue
    if (!cfg.enabled) return 1.0
    val halfLife = cfg.halfLifeMinutes.coerceAtLeast(1) * 60_000.0
    val radius = cfg.radius.toDouble()
    // Kills older than six half-lives weigh under 2%: forgotten.
    val kills = read(player).filter { now - it.at < halfLife * 6 }
    val weight = kills.filter { it.world == where.world.name && (it.x - where.blockX).toDouble().pow(2) + (it.z - where.blockZ).toDouble().pow(2) <= radius * radius }
      .sumOf { 0.5.pow((now - it.at) / halfLife) }
    write(player, (kills + Kill(where.world.name, where.blockX, where.blockZ, now)).takeLast(LIMIT))
    return factor(cfg.free, cfg.step, cfg.floor, weight)
  }

  private fun read(player: Player): List<Kill> =
    player.persistentDataContainer.get(plugin.keys.spawnerKills, PersistentDataType.STRING).orEmpty().split(';').mapNotNull { entry ->
      val parts = entry.split('|')
      if (parts.size != 4) return@mapNotNull null
      Kill(parts[0], parts[1].toIntOrNull() ?: return@mapNotNull null, parts[2].toIntOrNull() ?: return@mapNotNull null, parts[3].toLongOrNull() ?: return@mapNotNull null)
    }

  private fun write(player: Player, kills: List<Kill>) =
    player.persistentDataContainer.set(plugin.keys.spawnerKills, PersistentDataType.STRING, kills.joinToString(";") { "${it.world}|${it.x}|${it.z}|${it.at}" })

  companion object {
    /** The kills a player keeps: enough for any farm, little enough for the player's data. */
    private const val LIMIT = 40

    fun factor(free: Double, step: Double, floor: Double, weight: Double): Double =
      (1.0 / (1.0 + step.coerceAtLeast(0.0) * (weight - free).coerceAtLeast(0.0))).coerceIn(floor.coerceIn(0.0, 1.0), 1.0)
  }
}
