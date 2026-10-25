package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.DifficultySettings

/** The game's difficulty, as every platform names it, and the modifier it gives new bosses. */
enum class Difficulty {
  PEACEFUL, EASY, NORMAL, HARD;

  companion object {
    /** A boss made before difficulties graded bosses was made for hard. */
    const val UNGRADED = 1.0

    /** The modifier a new boss takes; null on peaceful, where no mob becomes a boss. */
    fun modifier(settings: DifficultySettings, difficulty: Difficulty, hardcore: Boolean): Double? {
      if (difficulty == PEACEFUL) return null
      if (!settings.enabled) return UNGRADED
      val modifiers = settings.modifiers
      val value = when {
        hardcore -> modifiers.hardcore
        difficulty == EASY -> modifiers.easy
        difficulty == NORMAL -> modifiers.normal
        else -> modifiers.hard
      }
      return value.coerceAtLeast(0.0)
    }

    /** What a part of a boss is multiplied by: [modifier] pulled toward 1 by what its [weight] leaves out. */
    fun part(modifier: Double, weight: Double): Double = (1.0 + (modifier - 1.0) * weight.coerceIn(0.0, 1.0)).coerceAtLeast(0.01)
  }
}
