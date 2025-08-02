@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer
import ru.mairwunnx.mobosses.serializers.SoundSerializer

@Serializable class RewardsConfigurationModel(
  @SerialName("enabled") val enabled: Boolean = true,
  @SerialName("extinguish_player") val extinguishPlayer: Boolean = true,
  @SerialName("cleanse_negatives") val cleanseNegatives: Boolean = true,

  @SerialName("base_duration_seconds") val baseDurationSeconds: Int = 6,
  @SerialName("duration_per_level") val durationPerLevel: Double = 0.06,
  @SerialName("max_duration_seconds") val maxDurationSeconds: Int = 16,

  @SerialName("amplifier_base") val amplifierBase: Int = 0,
  @SerialName("amplifier_per_level") val amplifierPerLevel: Double = 0.02,
  @SerialName("amplifier_max") val amplifierMax: Int = 2,

  @SerialName("cooldown_multiplier") val cooldownMultiplier: Double = 2.0,
  @SerialName("cooldown_min_seconds") val cooldownMinSeconds: Int = 8,
  @SerialName("cooldown_max_seconds") val cooldownMaxSeconds: Int = 180,

  @SerialName("tiers") val tiers: Map<String, TierReward> = defaultTiers(),
) {
  @Serializable class TierReward(
    @SerialName("effects_min") val effectsMin: Int,
    @SerialName("effects_max") val effectsMax: Int,
    @SerialName("extra_absorption_hearts") val extraAbsorptionHearts: Int = 0,
    @SerialName("effect_pool") val effectPool: List<PotionEffectType> = emptyList()
  )

  companion object {
    fun default() = RewardsConfigurationModel()

    private fun defaultTiers(): Map<String, TierReward> = mapOf(
      "common"    to TierReward(effectsMin = 1, effectsMax = 1, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.NIGHT_VISION
      )),
      "uncommon"  to TierReward(effectsMin = 1, effectsMax = 2, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.WATER_BREATHING,
        PotionEffectType.NIGHT_VISION, PotionEffectType.JUMP_BOOST
      )),
      "rare"      to TierReward(effectsMin = 2, effectsMax = 2, extraAbsorptionHearts = 2, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.LUCK
      )),
      "epic"      to TierReward(effectsMin = 2, effectsMax = 3, extraAbsorptionHearts = 4, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.STRENGTH,
        PotionEffectType.DOLPHINS_GRACE, PotionEffectType.CONDUIT_POWER
      )),
      "legendary" to TierReward(effectsMin = 3, effectsMax = 3, extraAbsorptionHearts = 6, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.STRENGTH,
        PotionEffectType.DOLPHINS_GRACE, PotionEffectType.CONDUIT_POWER, PotionEffectType.HERO_OF_THE_VILLAGE
      )),
      "mythic"    to TierReward(effectsMin = 3, effectsMax = 3, extraAbsorptionHearts = 8, effectPool = listOf(
        PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.STRENGTH,
        PotionEffectType.DOLPHINS_GRACE, PotionEffectType.CONDUIT_POWER, PotionEffectType.HERO_OF_THE_VILLAGE,
        PotionEffectType.SATURATION
      ))
    )
  }
}