package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.MinionsFormula
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

context(preset: MinionsFormula) fun minions(level: Int, rng: Random = Random.Default): Int {
  require(preset.max >= preset.base) { "max_count must be >= base_count" }
  val l = level.coerceAtLeast(1).toDouble()
  val lp = l.pow(preset.power)
  val progress = lp / (preset.offset + lp) // 0..~1
  val ideal = preset.base + (preset.max - preset.base) * progress
  val varied = if (preset.variance > 0.0) {
    val delta = ideal * preset.variance
    rng.nextDouble(ideal - delta, ideal + delta)
  } else ideal
  return varied.roundToInt().coerceIn(preset.base, preset.max)
}
