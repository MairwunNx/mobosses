package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

/** Players' levels and the levels of the bosses they meet. */
object Leveling {
  /** Keeps a zero in the config from dividing by zero. */
  private const val MIN = 1e-9

  /** A player's level from their Mo’Bosses experience and the kills that counted, 1 to `max_level`. */
  fun playerLevel(curve: Settings.LevelCurve, experience: Long, kills: Int): Int {
    val raw = score(curve, experience, kills)
    val progress = raw.pow(curve.power) / (curve.offset.coerceAtLeast(MIN) + raw.pow(curve.power))
    // Rounded, not cut: the curve only nears its top, and cutting kept max_level out of reach.
    return (1 + (curve.maxLevel - 1) * progress).roundToInt().coerceIn(1, curve.maxLevel.coerceAtLeast(1))
  }

  /** What a level grows from: experience and kills together. The board ranks by it, so it never disagrees with levels. */
  fun score(curve: Settings.LevelCurve, experience: Long, kills: Int): Double =
    sqrt(experience.coerceAtLeast(0).toDouble()) / curve.expScale.coerceAtLeast(MIN) + kills.coerceAtLeast(0).toDouble() / curve.killScale.coerceAtLeast(MIN)

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
    val progress = lp / (curve.offset.coerceAtLeast(MIN) + lp)
    val curveDelta = (curve.minDelta + (curve.maxDelta - curve.minDelta) * progress).roundToInt()
    val percentDelta = floor(parentLevel * curve.percentCap).toInt().coerceAtLeast(curve.minDelta)
    val jitter = if (curve.jitter > 0) rng.nextInt(-curve.jitter, curve.jitter + 1) else 0
    val delta = (min(curveDelta, percentDelta) + jitter).coerceAtLeast(curve.minDelta)
    return (parentLevel - delta).coerceAtLeast(1)
  }

  /** The Mo’Bosses experience a whole kill is worth, before the player's share and relevance. */
  fun bossExperience(gain: Settings.ExpGain, maxHealth: Double, classMul: Double, level: Int): Double {
    val hpPart = maxHealth * gain.healthFactor
    val lvlPart = gain.levelFactor * level.toDouble().pow(gain.levelPower)
    return (hpPart + lvlPart) * (if (gain.rarityMul) classMul else 1.0)
  }

  /** What a player gains from a kill: never nothing for a kill that counts, or the first bosses would show “+0”. */
  fun gained(worth: Double, counts: Boolean): Long = worth.roundToLong().let { if (counts) it.coerceAtLeast(1) else it.coerceAtLeast(0) }

  /** A boss's level against a player's, by ratio: `(boss + offset) / (player + offset)`. */
  fun ratio(relevance: Settings.Relevance, playerLevel: Int, bossLevel: Int): Double =
    (bossLevel.coerceAtLeast(1) + relevance.offset.coerceAtLeast(0.0)) / (playerLevel.coerceAtLeast(1) + relevance.offset.coerceAtLeast(0.0))

  /**
   * How much a kill means to a player: the ratio to `power` for bosses below them, so a boss a few
   * levels down costs little and one half their level most of it; the ratio itself above them, up to `bonus`.
   */
  fun relevance(relevance: Settings.Relevance, playerLevel: Int, bossLevel: Int): Double {
    val ratio = ratio(relevance, playerLevel, bossLevel)
    return if (ratio < 1.0) ratio.pow(relevance.power).coerceAtLeast(relevance.floor) else ratio.coerceAtMost(maxOf(1.0, relevance.bonus))
  }

  /** The share a threshold asks of each of [players]: the threshold, or less in a group, so four friends all get their part. */
  fun threshold(base: Double, groupShare: Double, players: Int): Double = minOf(base, groupShare / players.coerceAtLeast(1))

  /** Whether a kill counts toward a player's kills: they dealt enough, and the boss wasn't far below them. */
  fun countsAsKill(progression: Settings.Progression, share: Double, players: Int, playerLevel: Int, bossLevel: Int): Boolean =
    share >= threshold(progression.minShare, progression.groupShare, players) && ratio(progression.relevance, playerLevel, bossLevel) >= progression.relevance.killRatio

  /** What pet damage is worth in experience at a boss's level: nearly all of it at first, `min_weight` at the top. */
  fun petWeight(pets: Settings.PetDamage, level: Int): Double {
    val lp = level.coerceAtLeast(1).toDouble().pow(pets.power)
    val progress = lp / (pets.offset.coerceAtLeast(MIN) + lp)
    return 1.0 - (1.0 - pets.minWeight.coerceIn(0.0, 1.0)) * progress
  }
}
