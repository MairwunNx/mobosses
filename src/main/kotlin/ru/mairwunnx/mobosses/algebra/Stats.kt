package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings

/** A boss's stats from its mob's vanilla values, its class, and its level. */
object Stats {
  /** Capped at 1024, the game's limit for max health. */
  fun health(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = Curves.scaled(curve, base, classMul, level).coerceAtMost(1024.0)

  fun damage(curve: Settings.Curve, base: Double, classMul: Double, level: Int) = Curves.scaled(curve, base, classMul, level)

  /**
   * Armor or toughness, from the mob's own value or `base_points`, whichever is higher: most mobs
   * have no toughness and skeletons no armor, and nothing times a multiplier stays nothing.
   */
  fun armor(curve: Settings.CappedCurve, base: Double, classMul: Double, level: Int) =
    Curves.scaled(curve.curve, maxOf(base, curve.basePoints), classMul, level).coerceIn(0.0, curve.capPoints.toDouble())

  /** The vanilla experience a boss drops, from what its mob would. */
  fun experience(curve: Settings.Curve, baseExp: Int, classMul: Double, expModifier: Double, level: Int): Int =
    Math.round(baseExp * classMul * Curves.scale(curve, level).first * expModifier).toInt()
}
