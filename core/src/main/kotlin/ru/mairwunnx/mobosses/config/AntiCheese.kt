package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `anti_cheese` in `config.yml`: what keeps players from beating bosses with tricks instead of a fight. */
@Serializable
data class AntiCheese(
  /** Bosses never get into boats or minecarts. */
  val vehicles: Boolean = true,
  /** Bosses never go through portals. */
  val portals: Boolean = true,
  @SerialName("step_out") val stepOut: StepOut = StepOut(),
) {
  /**
   * A boss a player keeps hitting from beyond [reach], while it can't get to them for [unreachableSeconds] or hasn't
   * moved a block for [stuckSeconds], steps out next to them, or pulls them off their pillar when nothing there fits.
   */
  @Serializable
  data class StepOut(
    val enabled: Boolean = true,
    @SerialName("unreachable_seconds") val unreachableSeconds: Int = 8,
    @SerialName("stuck_seconds") val stuckSeconds: Int = 6,
    @SerialName("cooldown_seconds") val cooldownSeconds: Int = 20,
    /** Within this many blocks of a boss a player stands in its reach, and nothing counts as a trick. */
    val reach: Double = 3.0,
  )
}
