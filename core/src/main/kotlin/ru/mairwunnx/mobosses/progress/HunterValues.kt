package ru.mairwunnx.mobosses.progress

import kotlin.math.roundToLong

/**
 * A player's numbers as placeholders show them, the same on every platform: `level`, `xp` (or `experience`), `kills`,
 * and since 2.20 `deaths` to bosses, the `streak` of them in a row, and the `damage` dealt to bosses killed, whole.
 */
object HunterValues {
  val NAMES = listOf("level", "xp", "experience", "kills", "deaths", "streak", "damage")

  /** Null for a name it doesn't know. */
  fun value(name: String, progress: Progress, level: Int): String? = when (name.lowercase()) {
    "level" -> level.toString()
    "xp", "experience" -> progress.experience.toString()
    "kills" -> progress.kills.toString()
    "deaths" -> progress.deaths.toString()
    "streak" -> progress.streak.toString()
    "damage" -> progress.damage.roundToLong().toString()
    else -> null
  }
}
