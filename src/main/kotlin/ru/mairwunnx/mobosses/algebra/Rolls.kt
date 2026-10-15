package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/** Counts and random picks: minions, ability numbers, cooldowns, and weighted choices. */
object Rolls {
  /** How many minions a summon brings, before the cap on minions alive. */
  fun minions(curve: Settings.MinionsCurve, level: Int, rng: Random): Int {
    val base = curve.base.coerceAtLeast(0)
    val max = curve.max.coerceAtLeast(base)
    val lp = level.coerceAtLeast(1).toDouble().pow(curve.power)
    val ideal = base + (max - base) * (lp / (curve.offset + lp))
    val varied = if (curve.variance > 0.0) ideal * rng.nextDouble(1 - curve.variance, 1 + curve.variance) else ideal
    return varied.roundToInt().coerceIn(base, max)
  }

  /** A stat that grows by level with a cap, as abilities scale. */
  fun linear(base: Int, perLevel: Double, max: Int, level: Int) = (base + perLevel * level).toInt().coerceAtMost(max)

  fun linear(base: Double, perLevel: Double, max: Double, level: Int) = (base + perLevel * level).coerceAtMost(max)

  /** A duration or cooldown stretched by a random factor between 1 and [bias]; a bias of 1 or less leaves it. */
  fun biased(value: Double, bias: Double, rng: Random) = if (bias > 1.0) value * rng.nextDouble(1.0, bias) else value

  /** A point between [min] and [max], pulled toward [max] as [bias] grows. */
  fun biasedInRange(min: Double, max: Double, bias: Double, rng: Random): Double = min + (max - min) * (1.0 - (1.0 - rng.nextDouble()).pow(1.0 + bias))

  /** One of [items] by weight; null when none has any. */
  fun <T> weighted(items: List<Pair<T, Double>>, rng: Random): T? {
    val usable = items.filter { it.second > 0.0 }
    if (usable.isEmpty()) return null
    var roll = rng.nextDouble() * usable.sumOf { it.second }
    for ((item, weight) in usable) {
      roll -= weight
      if (roll < 0.0) return item
    }
    return usable.last().first
  }
}
