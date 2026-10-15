package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.pow
import kotlin.math.roundToInt

/** A boss's stats from its mob's vanilla values, its class, and its level. */
object Stats {
  /** From the mob's own health or `base_points`, whichever is higher; capped at 1024, the game's limit. */
  fun health(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = Curves.scaled(curve, maxOf(base, curve.basePoints), classMul, level).coerceAtMost(1024.0)

  fun damage(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = Curves.scaled(curve, base, classMul, level)

  /** What a boss's own arrows, fireballs, and tridents are multiplied by: a [share] of its melee growth. */
  fun ranged(curve: Settings.Curve, classMul: Double, level: Int, share: Double): Double =
    1.0 + (Curves.scaled(curve, 1.0, classMul, level) - 1.0) * share.coerceIn(0.0, 1.0)

  /** A creeper boss's whole blast radius, from the game's [base] (3, or 6 when charged), plus what its level adds. */
  fun explosionRadius(explosion: Settings.Explosion, base: Int, level: Int): Int {
    val lp = level.coerceAtLeast(1).toDouble().pow(explosion.power)
    return (base + explosion.maxExtra.coerceAtLeast(0.0) * lp / (explosion.offset.coerceAtLeast(1e-9) + lp)).roundToInt().coerceAtLeast(base)
  }

  /**
   * Armor or toughness, from the mob's own value or `base_points`, whichever is higher: most mobs
   * have no toughness and skeletons no armor, and nothing times a multiplier stays nothing.
   */
  fun armor(curve: Settings.CappedCurve, base: Double, classMul: Double, level: Int) =
    Curves.scaled(curve.curve, maxOf(base, curve.basePoints), classMul, level).coerceIn(0.0, curve.capPoints.toDouble())

  /** The vanilla experience a boss drops, from what its mob would, or `base_points` for mobs that drop less. */
  fun experience(curve: Settings.Curve, baseExp: Int, classMul: Double, expModifier: Double, level: Int): Int =
    Math.round(maxOf(baseExp.toDouble(), curve.basePoints) * classMul * Curves.scale(curve, level).first * expModifier).toInt()
}
