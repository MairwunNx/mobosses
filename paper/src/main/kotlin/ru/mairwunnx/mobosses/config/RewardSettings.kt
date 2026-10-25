package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `rewards.yml`: potion effects for players who dealt enough of a boss's health. */
@Serializable
data class RewardSettings(
  val enabled: Boolean = true,
  @SerialName("extinguish_player") val extinguishPlayer: Boolean = true,
  @SerialName("cleanse_negatives") val cleanseNegatives: Boolean = true,
  /** The share of a boss's health a player must deal to get rewards. */
  @SerialName("min_damage_share") val minDamageShare: Double = 0.27777777,
  @SerialName("base_duration_seconds") val baseDurationSeconds: Int = 8,
  @SerialName("duration_per_level") val durationPerLevel: Double = 0.6,
  @SerialName("max_duration_seconds") val maxDurationSeconds: Int = 60,
  @SerialName("amplifier_base") val amplifierBase: Int = 0,
  @SerialName("amplifier_per_level") val amplifierPerLevel: Double = 0.035,
  @SerialName("amplifier_max") val amplifierMax: Int = 3,
  @SerialName("cooldown_multiplier") val cooldownMultiplier: Double = 2.0,
  @SerialName("cooldown_min_seconds") val cooldownMinSeconds: Int = 8,
  @SerialName("cooldown_max_seconds") val cooldownMaxSeconds: Int = 180,
  val tiers: Map<String, Tier> = DEFAULT_TIERS,
) {
  @Serializable
  data class Tier(
    @SerialName("effects_min") val effectsMin: Int = 1,
    @SerialName("effects_max") val effectsMax: Int = 1,
    @SerialName("extra_absorption_hearts") val extraAbsorptionHearts: Int = 0,
    @SerialName("effect_pool") val effectPool: List<String> = emptyList(),
  )

  companion object {
    private val BASE = listOf("SPEED", "HASTE", "REGENERATION", "RESISTANCE", "FIRE_RESISTANCE")

    val DEFAULT_TIERS = mapOf(
      "common" to Tier(1, 1, 0, listOf("SPEED", "HASTE", "REGENERATION")),
      "uncommon" to Tier(1, 2, 0, listOf("REGENERATION", "HASTE", "WATER_BREATHING", "NIGHT_VISION", "JUMP_BOOST")),
      "rare" to Tier(2, 2, 2, BASE + "LUCK"),
      "epic" to Tier(2, 3, 4, BASE + listOf("DOLPHINS_GRACE", "CONDUIT_POWER")),
      "legendary" to Tier(3, 3, 6, BASE + listOf("DOLPHINS_GRACE", "CONDUIT_POWER")),
      "mythic" to Tier(3, 4, 8, BASE + listOf("STRENGTH", "DOLPHINS_GRACE", "CONDUIT_POWER", "SATURATION")),
    )

    fun parse(text: String, warn: (String) -> Unit = {}): RewardSettings = Yamls.decode(serializer(), text, warn)
  }
}
