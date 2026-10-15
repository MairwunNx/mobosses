package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The math behind bosses and players, the same as in 1.x. Pure functions, so the tests check them
 * against numbers 1.12.0 produced.
 */
object Formulas {
  /**
   * Growth by level: 1 at level 0, approaching `max_scale`, with `offset` delaying the climb and
   * `power` making it steeper. Returns the scale and the progress (0 to 1) that flattens classes.
   */
  fun levelScale(curve: Settings.Curve, level: Int): Pair<Double, Double> {
    val lp = level.coerceAtLeast(1).toDouble().pow(curve.power)
    val progress = lp / (curve.offset + lp)
    return 1.0 + (curve.maxScale - 1.0) * progress to progress
  }

  /** A class multiplier pulled toward 1 as [progress] grows, by `flatten`, so classes matter less at high levels. */
  fun flattenedClassMul(curve: Settings.Curve, classMul: Double, progress: Double): Double {
    val flatten = curve.flatten ?: 0.3
    if (flatten <= 0.0) return classMul
    val f = 1.0 - flatten.coerceIn(0.0, 1.0) * progress.coerceIn(0.0, 1.0)
    return 1.0 + (classMul - 1.0) * f
  }

  private fun scaled(curve: Settings.Curve, base: Double, classMul: Double, level: Int): Double {
    val (scale, progress) = levelScale(curve, level)
    return base * flattenedClassMul(curve, classMul, progress) * scale
  }

  /** Capped at 1024, the game's limit for max health. */
  fun health(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = scaled(curve, base, classMul, level).coerceAtMost(1024.0)

  fun damage(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = scaled(curve, base, classMul, level)

  fun armor(curve: Settings.CappedCurve, base: Double, classMul: Double, level: Int) =
    scaled(curve.curve, base, classMul, level).coerceIn(0.0, curve.capPoints.toDouble())

  /** The vanilla experience a boss drops. */
  fun experience(curve: Settings.Curve, baseExp: Int, classMul: Double, expModifier: Double, level: Int): Int {
    val l = level.toDouble()
    val scale = 1.0 + (curve.maxScale - 1.0) * l.pow(curve.power) / (curve.offset + l.pow(curve.power))
    return (baseExp * classMul * scale * expModifier).roundToInt()
  }

  /** The Mo’Bosses experience a kill is worth, before the player's share of the damage. */
  fun bossExperience(gain: Settings.ExpGain, maxHealth: Double, classMul: Double, level: Int): Long {
    val hpPart = maxHealth * gain.healthFactor
    val lvlPart = gain.levelFactor * level.toDouble().pow(gain.levelPower)
    val rarity = if (gain.rarityMul) classMul else 1.0
    return ((hpPart + lvlPart) * rarity).toLong()
  }

  /** A player's Mo’Bosses level from their experience and kills, 1 to `max_level`. */
  fun playerLevel(curve: Settings.LevelCurve, totalExp: Long, kills: Int): Int {
    val raw = sqrt(totalExp.coerceAtLeast(0).toDouble()) / curve.expScale + kills.coerceAtLeast(0).toDouble() / curve.killScale
    val progress = raw.pow(curve.power) / (curve.offset + raw.pow(curve.power))
    return (1 + (curve.maxLevel - 1) * progress).toInt()
  }

  /** A natural boss's level: the player's, sometimes lowered so weaker bosses still show up. */
  fun bossLevel(variance: Settings.Variance, playerLevel: Int, rng: Random): Int {
    val progress = playerLevel / 100.0
    val chance = variance.baseChance + variance.extraChance * progress
    if (rng.nextDouble() > chance) return playerLevel

    val minReduction = variance.minReduction.coerceAtLeast(0)
    val maxReduction = (minReduction + (variance.maxReduction - minReduction) * progress.pow(variance.power)).toInt().coerceAtLeast(minReduction)
    val reduction = rng.nextInt(minReduction, maxReduction + 1)
    return (playerLevel - reduction).coerceAtLeast(1)
  }

  /** A minion that turns into a boss is this much weaker than the boss that summoned it. */
  fun childLevel(parentLevel: Int, curve: Settings.MiniBossesCurve, rng: Random): Int {
    val lp = parentLevel.coerceAtLeast(1).toDouble().pow(curve.power)
    val progress = lp / (curve.offset + lp)
    val curveDelta = (curve.minDelta + (curve.maxDelta - curve.minDelta) * progress).roundToInt()
    val percentDelta = floor(parentLevel * curve.percentCap).toInt().coerceAtLeast(curve.minDelta)
    val baseDelta = min(curveDelta, percentDelta)
    val jitter = if (curve.jitter > 0) rng.nextInt(-curve.jitter, curve.jitter + 1) else 0
    val finalDelta = (baseDelta + jitter).coerceAtLeast(curve.minDelta)
    return (parentLevel - finalDelta).coerceAtLeast(1)
  }

  /** How many minions a summon brings. */
  fun minions(curve: Settings.MinionsCurve, level: Int, rng: Random): Int {
    val base = curve.base.coerceAtLeast(0)
    val max = curve.max.coerceAtLeast(base)
    val lp = level.coerceAtLeast(1).toDouble().pow(curve.power)
    val ideal = base + (max - base) * (lp / (curve.offset + lp))
    val varied = if (curve.variance > 0.0) {
      val delta = ideal * curve.variance
      rng.nextDouble(ideal - delta, ideal + delta)
    } else ideal
    return varied.roundToInt().coerceIn(base, max)
  }

  /** A linear stat by level with a cap, as abilities scale. */
  fun linear(base: Int, perLevel: Double, max: Int, level: Int) = (base + perLevel * level).toInt().coerceAtMost(max)

  fun linear(base: Double, perLevel: Double, max: Double, level: Int) = (base + perLevel * level).coerceAtMost(max)

  /** A random duration or cooldown: [value] stretched by a factor between 1 and [bias]. */
  fun biased(value: Double, bias: Double, rng: Random) = if (bias > 1.0) value * rng.nextDouble(1.0, bias) else value

  /** A point between [min] and [max], pulled toward [max] as [bias] grows; random durability uses it. */
  fun biasedInRange(min: Double, max: Double, bias: Double, rng: Random): Double {
    val t = 1.0 - (1.0 - rng.nextDouble()).pow(1.0 + bias)
    return min + (max - min) * t
  }
}
