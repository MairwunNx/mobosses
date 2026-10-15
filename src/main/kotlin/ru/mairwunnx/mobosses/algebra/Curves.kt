package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.pow

/**
 * The growth curve every stat shares: 1 at level 0, rising fastest around `offset^(1/power)` and
 * leveling off toward `max_scale`, so no single level brings a jump.
 */
object Curves {
  /** The scale for a level and the progress, 0 to 1, that also flattens classes. */
  fun scale(curve: Settings.Curve, level: Int): Pair<Double, Double> {
    val lp = level.coerceAtLeast(1).toDouble().pow(curve.power)
    val progress = lp / (curve.offset + lp)
    return 1.0 + (curve.maxScale - 1.0) * progress to progress
  }

  /** A class multiplier pulled toward 1 as [progress] grows, by `flatten`, so classes matter less at high levels. */
  fun flattened(curve: Settings.Curve, classMul: Double, progress: Double): Double {
    val flatten = curve.flatten ?: 0.3
    if (flatten <= 0.0) return classMul
    return 1.0 + (classMul - 1.0) * (1.0 - flatten.coerceIn(0.0, 1.0) * progress.coerceIn(0.0, 1.0))
  }

  /** A base value grown by level and class. */
  fun scaled(curve: Settings.Curve, base: Double, classMul: Double, level: Int): Double {
    val (scale, progress) = scale(curve, level)
    return base * flattened(curve, classMul, progress) * scale
  }
}
