package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.AbilitySettings
import kotlin.random.Random

/** Where lightning's extra bolts go: around the players the boss fights, or now and then right into one of them. */
object Bolts {
  /**
   * For each of [extra] bolts past the [foes]' own: the index of the foe it strikes outright, by `chance`, while that
   * foe has had fewer than `max` bolts in all; null for one that falls around them.
   */
  fun aim(foes: Int, extra: Int, settings: AbilitySettings.LightningAimed, rng: Random = Random): List<Int?> {
    val struck = IntArray(foes) { 1 }
    return List(extra.coerceAtLeast(0)) {
      if (foes == 0 || rng.nextDouble() >= settings.chance) return@List null
      (0 until foes).filter { struck[it] < settings.max }.randomOrNull(rng)?.also { struck[it]++ }
    }
  }
}
