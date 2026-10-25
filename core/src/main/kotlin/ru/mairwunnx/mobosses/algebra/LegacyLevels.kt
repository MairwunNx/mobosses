package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Levels before 2.11.0, from experience and kills together, kept only to carry players over: [carry] gives the
 * experience that puts a player at the same level, and as far on the way to the next, under today's formula.
 */
object LegacyLevels {
  private const val MIN = 1e-9

  fun level(curve: Settings.LevelCurve, experience: Long, kills: Int): Int {
    val raw = sqrt(experience.coerceAtLeast(0).toDouble()) / curve.expScale.coerceAtLeast(MIN) + kills.coerceAtLeast(0).toDouble() / curve.killScale.coerceAtLeast(MIN)
    val progress = raw.pow(curve.power) / (curve.offset.coerceAtLeast(MIN) + raw.pow(curve.power))
    return (1 + (curve.maxLevel - 1) * progress).roundToInt().coerceIn(1, curve.maxLevel.coerceAtLeast(1))
  }

  /** The least experience that reached [level] with [kills]; null when no experience does. */
  fun experienceFor(curve: Settings.LevelCurve, level: Int, kills: Int): Long? {
    if (level > curve.maxLevel.coerceAtLeast(1)) return null
    var high = 1L
    while (level(curve, high, kills) < level) {
      if (high > Long.MAX_VALUE / 4) return null
      high *= 2
    }
    var low = 0L
    while (low < high) {
      val mid = low + (high - low) / 2
      if (level(curve, mid, kills) >= level) high = mid else low = mid + 1
    }
    return low
  }

  /** Today's experience for a player who had [experience] and [kills] before 2.11.0. */
  fun carry(curve: Settings.LevelCurve, experience: Long, kills: Int): Long {
    if (experience <= 0 && kills <= 0) return 0
    val level = level(curve, experience, kills)
    val from = Leveling.experienceFor(curve, level) ?: return experience
    val to = Leveling.experienceFor(curve, level + 1) ?: return from
    val low = experienceFor(curve, level, kills) ?: 0L
    val high = experienceFor(curve, level + 1, kills)
    val way = if (high == null || high <= low) 0.0 else ((experience - low).toDouble() / (high - low)).coerceIn(0.0, 0.999)
    // Cut, never rounded up, and short of the next level: a short level would otherwise round a player past it.
    return (from + ((to - from) * way).toLong()).coerceAtMost(to - 1).coerceAtLeast(from)
  }
}
