package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Players' levels and the levels of the bosses they meet. */
object Leveling {
  /** A player's level from their Mo’Bosses experience and the kills that counted, 1 to `max_level`. */
  fun playerLevel(curve: Settings.LevelCurve, experience: Long, kills: Int): Int {
    val raw = sqrt(experience.coerceAtLeast(0).toDouble()) / curve.expScale + kills.coerceAtLeast(0).toDouble() / curve.killScale
    val progress = raw.pow(curve.power) / (curve.offset + raw.pow(curve.power))
    // Rounded, not cut: the curve only nears its top, and cutting kept max_level out of reach.
    return (1 + (curve.maxLevel - 1) * progress).roundToInt().coerceIn(1, curve.maxLevel.coerceAtLeast(1))
  }

  /** The level a group sets for a boss: their average, so one veteran doesn't drag newcomers into their fights. */
  fun groupLevel(levels: List<Int>): Int = if (levels.isEmpty()) 1 else levels.average().roundToInt().coerceAtLeast(1)

  /**
   * A natural boss's level: the group's, sometimes cut by a share, more often and by more at high
   * levels, so weaker bosses keep turning up without swinging from level 80 to 20.
   */
  fun bossLevel(variance: Settings.Variance, level: Int, maxLevel: Int, rng: Random): Int {
    val progress = (level.toDouble() / maxLevel.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    if (rng.nextDouble() > variance.baseChance + variance.extraChance * progress) return level
    val low = variance.minCut.coerceIn(0.0, 1.0)
    val high = (low + (variance.maxCut - low) * progress.pow(variance.power)).coerceIn(low, 1.0)
    val cut = if (high > low) rng.nextDouble(low, high) else low
    return (level * (1.0 - cut)).roundToInt().coerceAtLeast(1)
  }

  /** A minion that rolls into a boss is this much weaker than the boss that summoned it. */
  fun childLevel(parentLevel: Int, curve: Settings.MiniBossesCurve, rng: Random): Int {
    val lp = parentLevel.coerceAtLeast(1).toDouble().pow(curve.power)
    val progress = lp / (curve.offset + lp)
    val curveDelta = (curve.minDelta + (curve.maxDelta - curve.minDelta) * progress).roundToInt()
    val percentDelta = floor(parentLevel * curve.percentCap).toInt().coerceAtLeast(curve.minDelta)
    val jitter = if (curve.jitter > 0) rng.nextInt(-curve.jitter, curve.jitter + 1) else 0
    val delta = (min(curveDelta, percentDelta) + jitter).coerceAtLeast(curve.minDelta)
    return (parentLevel - delta).coerceAtLeast(1)
  }

  /** The Mo’Bosses experience a whole kill is worth, before the player's share and relevance. */
  fun bossExperience(gain: Settings.ExpGain, maxHealth: Double, classMul: Double, level: Int): Long {
    val hpPart = maxHealth * gain.healthFactor
    val lvlPart = gain.levelFactor * level.toDouble().pow(gain.levelPower)
    return ((hpPart + lvlPart) * (if (gain.rarityMul) classMul else 1.0)).toLong()
  }

  /** How much a kill means to a player: less for bosses far below their level, a little more above it. */
  fun relevance(relevance: Settings.Relevance, playerLevel: Int, bossLevel: Int): Double =
    (1.0 + (bossLevel - playerLevel).toDouble() / relevance.gap.coerceAtLeast(1)).coerceIn(relevance.floor, maxOf(relevance.floor, relevance.bonus))

  /** Whether a kill counts toward a player's kills: they dealt enough, and the boss wasn't far below them. */
  fun countsAsKill(progression: Settings.Progression, share: Double, playerLevel: Int, bossLevel: Int): Boolean =
    share >= progression.minShare && bossLevel >= playerLevel - progression.relevance.gap
}
