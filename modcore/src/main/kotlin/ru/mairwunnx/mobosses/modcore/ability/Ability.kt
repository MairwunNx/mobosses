package ru.mairwunnx.mobosses.modcore.ability

import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/**
 * One boss ability, as on Paper: whether it's on, the level each class gets it at, its cooldown, and what it does.
 * [cast] returns whether it did anything, so a boss with nobody in reach keeps it for later.
 */
interface Ability {
  /** Its key in the boss's saved cooldowns and in the debug log. */
  val key: String
  /** What the player whose hit set it off reads. */
  val message: Message

  fun enabled(settings: AbilitySettings): Boolean

  fun threshold(thresholds: AbilityThresholds): Int

  /** Milliseconds, and the bias that stretches them at random. */
  fun cooldown(settings: AbilitySettings): Pair<Long, Double>

  /** Whether this boss may cast it now, beyond its level and cooldown. */
  fun usable(cast: Cast): Boolean = true

  fun cast(cast: Cast): Boolean
}
