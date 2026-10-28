package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `aggro` in `config.yml`: whom a boss chases. Each player's damage counts as threat, fading by half every
 * [halfLifeSeconds], and a player whose threat beats the target's by [margin] draws the boss.
 */
@Serializable
data class Aggro(
  val enabled: Boolean = true,
  @SerialName("half_life_seconds") val halfLifeSeconds: Double = 6.0,
  val margin: Double = 1.25,
)
