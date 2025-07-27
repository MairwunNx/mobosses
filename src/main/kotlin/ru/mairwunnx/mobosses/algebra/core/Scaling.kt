package ru.mairwunnx.mobosses.algebra.core

import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection
import kotlin.math.pow

/**
 * Универсальная кривая роста по уровню: >= 1.0, асимптота = maxScale.
 * Возвращает пару (scale, progress) — второе удобно для сплющивания классов.
 */
context(preset: FormulaSection) fun levelScale(level: Int): Pair<Double, Double> {
  require(level >= 1) { "level must be >= 1" }
  val l = level.toDouble()
  val lp = l.pow(preset.power)
  val progress = lp / (preset.offset + lp)              // 0..~1
  val scale = 1.0 + (preset.maxScale - 1.0) * progress  // 1..maxScale
  return scale to progress
}

fun scaleInt(base: Int, perLevel: Double, max: Int, level: Int) =
  (base + perLevel * level).toInt().coerceAtMost(max)

fun scaleDouble(base: Double, perLevel: Double, max: Double, level: Int) =
  (base + perLevel * level).coerceAtMost(max)