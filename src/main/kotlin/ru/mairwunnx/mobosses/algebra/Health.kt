package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.algebra.core.flattenedClassMul
import ru.mairwunnx.mobosses.algebra.core.levelScale
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection

context(preset: FormulaSection) fun health(
  baseHealth: Double,      // ванильная база моба
  classHealthMul: Double,  // из Classes.health (1.0..5.0)
  level: Int,
): Double {
  val (scale, progress) = levelScale(level)
  val classMulEff = flattenedClassMul(classHealthMul, progress)
  return (baseHealth * classMulEff * scale).coerceAtMost(1024.0)
}
