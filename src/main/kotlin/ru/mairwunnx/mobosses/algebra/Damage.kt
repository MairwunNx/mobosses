package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.algebra.core.flattenedClassMul
import ru.mairwunnx.mobosses.algebra.core.levelScale
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection

context(_: FormulaSection) fun damage(
  baseDamage: Double,      // ванильный базовый урон
  classDamageMul: Double,  // из Classes.damage (1.0..3.0)
  level: Int,
): Double {
  val (scale, progress) = levelScale(level)
  val classMulEff = flattenedClassMul(classDamageMul, progress)
  return baseDamage * classMulEff * scale
}
