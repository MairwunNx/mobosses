package ru.mairwunnx.mobosses.boss

import kotlin.math.abs

/**
 * The menu's way to spawn a boss, the same on every platform: its mob, its class, its level on a dial of digits,
 * and a chunk on a grid around the admin, who stands in the middle. In the chunk the boss comes at the safest spot
 * at the admin's height.
 */
object SpawnPlan {
  /** The grid's columns and rows of chunks, north at the top. */
  const val COLUMNS = 9
  const val ROWS = 5
  /** The slot of the admin's own chunk. */
  const val CENTER = (ROWS / 2) * COLUMNS + COLUMNS / 2

  /** How many digits a level up to [max] takes. */
  fun places(max: Int) = max.coerceAtLeast(1).toString().length

  /** The digit of [level] at [place]: 0 the ones, 1 the tens, and so on. */
  fun digit(level: Int, place: Int): Int = (level / pow10(place)) % 10

  /** [level] with one more, or one less, at [place], kept from 1 to [max]. */
  fun step(level: Int, place: Int, up: Boolean, max: Int): Int = (level + (if (up) 1 else -1) * pow10(place)).coerceIn(1, max.coerceAtLeast(1))

  /** The chunk of a grid [slot], east and south of the admin's; null past the grid. */
  fun offset(slot: Int): Pair<Int, Int>? {
    if (slot !in 0 until COLUMNS * ROWS) return null
    return (slot % COLUMNS - COLUMNS / 2) to (slot / COLUMNS - ROWS / 2)
  }

  /** A block a boss may stand on: its feet at [y]. */
  data class Spot(val x: Int, val y: Int, val z: Int)

  /**
   * The safest of [spots] in a chunk: the nearest to the admin's height [y], then to the chunk's middle at
   * [middleX], [middleZ], so a boss neither falls nor ends up in a wall at the chunk's edge.
   */
  fun safest(spots: List<Spot>, y: Int, middleX: Double, middleZ: Double): Spot? =
    spots.minWithOrNull(compareBy<Spot> { abs(it.y - y) }.thenBy { (it.x + 0.5 - middleX).let { dx -> dx * dx } + (it.z + 0.5 - middleZ).let { dz -> dz * dz } })

  private fun pow10(place: Int): Int {
    var result = 1
    repeat(place.coerceAtLeast(0)) { result *= 10 }
    return result
  }
}
