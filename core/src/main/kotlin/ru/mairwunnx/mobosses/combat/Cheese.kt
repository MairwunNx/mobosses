package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.config.AntiCheese

/**
 * One boss watched for tricks, sampled about once a second. A boss is pressed while a player hit it lately from
 * beyond its reach, and blocked while it can't get to that player; [sample] says when it has been either long enough
 * to step out. Kept in memory: a restart only gives a trick a few seconds more.
 */
class Cheese {
  private var anchor: DoubleArray? = null
  private var stillSince: Long? = null
  private var blockedSince: Long? = null
  private var steppedAt: Long? = null

  /** Whether the boss at [x], [y], [z] should step out [now], in milliseconds. */
  fun sample(settings: AntiCheese.StepOut, x: Double, y: Double, z: Double, now: Long, pressed: Boolean, blocked: Boolean): Boolean {
    if (!settings.enabled || !pressed) {
      anchor = doubleArrayOf(x, y, z)
      stillSince = null
      blockedSince = null
      return false
    }
    val from = anchor
    if (from == null || moved(from, x, y, z)) {
      anchor = doubleArrayOf(x, y, z)
      stillSince = now
    } else if (stillSince == null) stillSince = now
    blockedSince = if (blocked) blockedSince ?: now else null

    val stuck = now - (stillSince ?: now) >= settings.stuckSeconds * 1000L
    val unreachable = blockedSince?.let { now - it >= settings.unreachableSeconds * 1000L } == true
    val rested = steppedAt?.let { now - it >= settings.cooldownSeconds * 1000L } ?: true
    return (stuck || unreachable) && rested
  }

  /** The boss stepped out [now]: the watch starts over, and the cooldown runs. */
  fun stepped(now: Long) {
    steppedAt = now
    anchor = null
    stillSince = null
    blockedSince = null
  }

  private fun moved(from: DoubleArray, x: Double, y: Double, z: Double): Boolean {
    val dx = x - from[0]
    val dy = y - from[1]
    val dz = z - from[2]
    return dx * dx + dy * dy + dz * dz > 1.0
  }
}
