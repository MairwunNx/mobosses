package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.pow

/**
 * Spawner fatigue, the same on every platform: the spawner kills a player made lately, kept on the player as text,
 * and what they leave of the next reward. Each kill near the new one counts, fading by half every
 * `half_life_minutes`; past `free` of them, each one more takes `step` off, down to `floor`.
 */
object Fatigue {
  class Kill(val world: String, val x: Int, val z: Int, val at: Long)

  /** The kills a player keeps: enough for any farm, little enough for the player's data. */
  private const val LIMIT = 40

  /** What a reward for a spawner boss killed at [world], [x], [z] is multiplied by, and the kills to keep after it. */
  fun take(cfg: Settings.SpawnerFatigue, stored: String?, world: String, x: Int, z: Int, now: Long): Pair<Double, String> {
    val halfLife = cfg.halfLifeMinutes.coerceAtLeast(1) * 60_000.0
    val radius = cfg.radius.toDouble()
    // Kills older than six half-lives weigh under 2%: forgotten.
    val kills = decode(stored).filter { now - it.at < halfLife * 6 }
    val weight = kills.filter { it.world == world && (it.x - x).toDouble().pow(2) + (it.z - z).toDouble().pow(2) <= radius * radius }
      .sumOf { 0.5.pow((now - it.at) / halfLife) }
    return factor(cfg.free, cfg.step, cfg.floor, weight) to encode((kills + Kill(world, x, z, now)).takeLast(LIMIT))
  }

  fun factor(free: Double, step: Double, floor: Double, weight: Double): Double =
    (1.0 / (1.0 + step.coerceAtLeast(0.0) * (weight - free).coerceAtLeast(0.0))).coerceIn(floor.coerceIn(0.0, 1.0), 1.0)

  private fun decode(text: String?): List<Kill> = text.orEmpty().split(';').mapNotNull { entry ->
    val parts = entry.split('|')
    if (parts.size != 4) return@mapNotNull null
    Kill(parts[0], parts[1].toIntOrNull() ?: return@mapNotNull null, parts[2].toIntOrNull() ?: return@mapNotNull null, parts[3].toLongOrNull() ?: return@mapNotNull null)
  }

  private fun encode(kills: List<Kill>) = kills.joinToString(";") { "${it.world}|${it.x}|${it.z}|${it.at}" }
}
