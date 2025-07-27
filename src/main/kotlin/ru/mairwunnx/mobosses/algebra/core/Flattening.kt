package ru.mairwunnx.mobosses.algebra.core

import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection

/**
 * Применяет сплющивание классового множителя к 1 в зависимости от progress.
 */
context(preset: FormulaSection) fun flattenedClassMul(classMul: Double, progress: Double): Double {
  val flatten = preset.flatten ?: 0.3 // Дефолтное значение, никогда не должно случиться, но мало-ли%)
  if (flatten <= 0.0) return classMul
  val f = 1.0 - (flatten.coerceIn(0.0, 1.0) * progress.coerceIn(0.0, 1.0))
  return 1.0 + (classMul - 1.0) * f
}
