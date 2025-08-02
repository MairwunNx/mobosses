package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.algebra.core.flattenedClassMul
import ru.mairwunnx.mobosses.algebra.core.levelScale
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection

/**
 * Armor и Toughness: те же кривые, что и для health/damage,
 * но с капами в очках (по ванильной механике).
 *
 * Рекомендуемые капы:
 *   - armor: до ~30 (выше почти не даёт выгоды)
 *   - toughness: до ~20 (в ваниле редко выше ~12)
 */
context(preset: FormulaSection) fun armorPoints(
  baseArmor: Double,      // "база" моба в очках брони
  classArmorMul: Double,  // множитель от класса (как в health/damage)
  level: Int,
  cap: Double = 30.0
): Double {
  val (scale, progress) = levelScale(level)
  val classMulEff = flattenedClassMul(classArmorMul, progress)
  return (baseArmor * classMulEff * scale).coerceIn(0.0, cap)
}

context(preset: FormulaSection) fun armorToughness(
  baseTough: Double,        // "база" стойкости (0..)
  classToughMul: Double,    // множитель от класса
  level: Int,
  cap: Double = 20.0
): Double {
  val (scale, progress) = levelScale(level)
  val classMulEff = flattenedClassMul(classToughMul, progress)
  return (baseTough * classMulEff * scale).coerceIn(0.0, cap)
}