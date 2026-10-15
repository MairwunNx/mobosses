package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `abilities.yml`, flat as in 1.x so its files keep loading. Cooldowns are in milliseconds; a
 * cooldown's bias stretches it by a random factor between 1 and the bias. The levels a class gets
 * each ability at live in `classes.yml`.
 */
@Serializable
data class AbilitySettings(
  @SerialName("is_lightning_enabled") val lightningEnabled: Boolean = true,
  @SerialName("is_summoning_enabled") val summoningEnabled: Boolean = true,
  @SerialName("is_regeneration_enabled") val regenerationEnabled: Boolean = true,
  @SerialName("is_fire_ring_enabled") val fireRingEnabled: Boolean = true,
  @SerialName("is_web_ring_enabled") val webRingEnabled: Boolean = true,
  @SerialName("is_teleport_strike_enabled") val teleportStrikeEnabled: Boolean = true,
  @SerialName("is_meteor_shower_enabled") val meteorShowerEnabled: Boolean = true,
  @SerialName("is_arrow_arc_enabled") val arrowArcEnabled: Boolean = true,
  @SerialName("is_arrow_rain_enabled") val arrowRainEnabled: Boolean = true,

  @SerialName("general_cooldown") val generalCooldown: Long = 5000,
  @SerialName("general_cooldown_bias") val generalCooldownBias: Double = 1.4,
  @SerialName("lightning_cooldown") val lightningCooldown: Long = 10000,
  @SerialName("lightning_cooldown_bias") val lightningCooldownBias: Double = 1.5,
  @SerialName("summoning_cooldown") val summoningCooldown: Long = 20000,
  @SerialName("summoning_cooldown_bias") val summoningCooldownBias: Double = 1.2,
  @SerialName("regeneration_cooldown") val regenerationCooldown: Long = 30000,
  @SerialName("regeneration_cooldown_bias") val regenerationCooldownBias: Double = 1.1,
  @SerialName("fire_ring_cooldown") val fireRingCooldown: Long = 6000,
  @SerialName("fire_ring_cooldown_bias") val fireRingCooldownBias: Double = 1.3,
  @SerialName("web_ring_cooldown") val webRingCooldown: Long = 8000,
  @SerialName("web_ring_cooldown_bias") val webRingCooldownBias: Double = 1.4,
  @SerialName("teleport_strike_cooldown") val teleportStrikeCooldown: Long = 11000,
  @SerialName("teleport_strike_cooldown_bias") val teleportStrikeCooldownBias: Double = 1.2,

  @SerialName("lightning_damage") val lightningDamage: Double = 5.0,
  @SerialName("lightning_damage_modifier") val lightningDamageModifier: Double = 0.1,
  @SerialName("lightning_damage_max") val lightningDamageMax: Double = 20.0,
  @SerialName("lightning_radius") val lightningRadius: Double = 3.0,
  @SerialName("lightning_radius_modifier") val lightningRadiusModifier: Double = 0.05,
  @SerialName("lightning_radius_max") val lightningRadiusMax: Double = 10.0,
  @SerialName("lightning_strikes") val lightningStrikes: Int = 6,
  @SerialName("lightning_strikes_modifier") val lightningStrikesModifier: Double = 0.1,
  @SerialName("lightning_strikes_max") val lightningStrikesMax: Int = 26,

  @SerialName("regeneration_duration_bias") val regenerationDurationBias: Double = 1.4,
  @SerialName("regen_duration_seconds_base") val regenDurationSecondsBase: Int = 8,
  @SerialName("regen_duration_seconds_per_level") val regenDurationSecondsPerLevel: Double = 0.2,
  @SerialName("regen_duration_seconds_max") val regenDurationSecondsMax: Int = 20,
  @SerialName("regen_amplifier_base") val regenAmplifierBase: Int = 0,
  @SerialName("regen_amplifier_per_level") val regenAmplifierPerLevel: Double = 0.1,
  @SerialName("regen_amplifier_max") val regenAmplifierMax: Int = 3,
  @SerialName("regen_aura_radius") val regenAuraRadius: Double = 4.0,
  @SerialName("regen_extra_resistance_seconds") val regenExtraResistanceSeconds: Int = 6,
  @SerialName("regen_cleanse_negatives") val regenCleanseNegatives: Boolean = true,
  @SerialName("regen_anti_projectiles") val regenAntiProjectiles: Boolean = true,
  @SerialName("regen_slow_players") val regenSlowPlayers: Boolean = true,

  @SerialName("fire_ring_min_radius") val fireRingMinRadius: Double = 4.0,
  @SerialName("fire_ring_max_radius") val fireRingMaxRadius: Double = 7.0,
  @SerialName("fire_ring_density") val fireRingDensity: Double = 0.7,

  @SerialName("web_ring_radius") val webRingRadius: Double = 3.0,
  /** Seconds the cobwebs stay. */
  @SerialName("web_ring_duration") val webRingDuration: Int = 8,
  @SerialName("web_ring_potion_enabled") val webRingPotionEnabled: Boolean = true,
  @SerialName("web_ring_potion_base_duration") val webRingPotionBaseDuration: Int = 4,
  @SerialName("web_ring_potion_max_duration") val webRingPotionMaxDuration: Int = 10,
  @SerialName("web_ring_potion_level_modifier") val webRingPotionLevelModifier: Double = 0.06,

  @SerialName("meteor_shower_cooldown") val meteorShowerCooldown: Long = 16000,
  @SerialName("meteor_shower_cooldown_bias") val meteorShowerCooldownBias: Double = 1.2,
  @SerialName("meteor_shower_waves_base") val meteorWavesBase: Int = 2,
  @SerialName("meteor_shower_waves_per_level") val meteorWavesPerLevel: Double = 0.03,
  @SerialName("meteor_shower_waves_max") val meteorWavesMax: Int = 7,
  @SerialName("meteor_shower_wave_interval_ticks") val meteorWaveIntervalTicks: Long = 20,
  @SerialName("meteor_shower_meteors_per_wave_base") val meteorsPerWaveBase: Int = 6,
  @SerialName("meteor_shower_meteors_per_wave_per_level") val meteorsPerWavePerLevel: Double = 0.08,
  @SerialName("meteor_shower_meteors_per_wave_max") val meteorsPerWaveMax: Int = 20,
  @SerialName("meteor_shower_radius_base") val meteorRadiusBase: Double = 6.0,
  @SerialName("meteor_shower_radius_per_level") val meteorRadiusPerLevel: Double = 0.05,
  @SerialName("meteor_shower_radius_max") val meteorRadiusMax: Double = 14.0,
  @SerialName("meteor_shower_height") val meteorHeight: Double = 18.0,
  @SerialName("meteor_shower_fall_speed") val meteorFallSpeed: Double = 1.2,
  @SerialName("meteor_shower_meteor_lifetime_seconds") val meteorLifetimeSeconds: Int = 8,
  @SerialName("meteor_shower_impact_damage_base") val meteorDamageBase: Double = 4.0,
  @SerialName("meteor_shower_impact_damage_per_level") val meteorDamagePerLevel: Double = 0.12,
  @SerialName("meteor_shower_impact_damage_max") val meteorDamageMax: Double = 18.0,
  @SerialName("meteor_shower_fire_radius") val meteorFireRadius: Double = 2.5,
  @SerialName("meteor_shower_ignite_seconds") val meteorIgniteSeconds: Int = 3,

  @SerialName("arrow_arc_cooldown") val arrowArcCooldown: Long = 7000,
  @SerialName("arrow_arc_cooldown_bias") val arrowArcCooldownBias: Double = 1.1,
  @SerialName("arrow_arc_arrows_base") val arcArrowsBase: Int = 18,
  @SerialName("arrow_arc_arrows_per_level") val arcArrowsPerLevel: Double = 0.2,
  @SerialName("arrow_arc_arrows_max") val arcArrowsMax: Int = 48,
  @SerialName("arrow_arc_rings") val arcRings: Int = 3,
  @SerialName("arrow_arc_vertical_spread") val arcVerticalSpread: Double = 0.0,
  @SerialName("arrow_arc_staggered") val arcStaggered: Boolean = true,
  @SerialName("arrow_arc_no_gravity") val arcNoGravity: Boolean = true,
  @SerialName("arrow_arc_speed") val arcSpeed: Double = 1.6,
  @SerialName("arrow_arc_lifetime_seconds") val arcLifetimeSeconds: Int = 6,
  @SerialName("arrow_arc_damage_base") val arcDamageBase: Double = 3.0,
  @SerialName("arrow_arc_damage_per_level") val arcDamagePerLevel: Double = 0.05,
  @SerialName("arrow_arc_damage_max") val arcDamageMax: Double = 8.0,
  @SerialName("arrow_arc_random_effect_level_threshold") val arcEffectLevelThreshold: Int = 1,
  @SerialName("arrow_arc_random_effects_max_per_ring") val arcEffectsMaxPerRing: Int = 16,
  @SerialName("arrow_arc_effect_duration_seconds") val arcEffectDurationSeconds: Int = 6,
  @SerialName("arrow_arc_effect_amplifier_min") val arcEffectAmplifierMin: Int = 0,
  @SerialName("arrow_arc_effect_amplifier_max") val arcEffectAmplifierMax: Int = 1,
  @SerialName("arrow_arc_negative_effects") val arcNegativeEffects: List<String> = NEGATIVE_EFFECTS,

  @SerialName("arrow_rain_cooldown") val arrowRainCooldown: Long = 8000,
  @SerialName("arrow_rain_cooldown_bias") val arrowRainCooldownBias: Double = 1.1,
  @SerialName("arrow_rain_arrows_base") val rainArrowsBase: Int = 30,
  @SerialName("arrow_rain_arrows_per_level") val rainArrowsPerLevel: Double = 0.3,
  @SerialName("arrow_rain_arrows_max") val rainArrowsMax: Int = 90,
  @SerialName("arrow_rain_waves") val rainWaves: Int = 2,
  @SerialName("arrow_rain_wave_interval_ticks") val rainWaveIntervalTicks: Long = 20,
  @SerialName("arrow_rain_half_size") val rainHalfSize: Double = 8.0,
  @SerialName("arrow_rain_target_search_radius") val rainTargetSearchRadius: Double = 40.0,
  @SerialName("arrow_rain_height") val rainHeight: Double = 12.0,
  @SerialName("arrow_rain_fall_speed") val rainFallSpeed: Double = 0.9,
  @SerialName("arrow_rain_lifetime_seconds") val rainLifetimeSeconds: Int = 6,
  @SerialName("arrow_rain_damage_base") val rainDamageBase: Double = 2.6,
  @SerialName("arrow_rain_damage_per_level") val rainDamagePerLevel: Double = 0.06,
  @SerialName("arrow_rain_damage_max") val rainDamageMax: Double = 7.0,
  @SerialName("arrow_rain_random_effect_level_threshold") val rainEffectLevelThreshold: Int = 35,
  @SerialName("arrow_rain_random_effects_max_per_wave") val rainEffectsMaxPerWave: Int = 24,
  @SerialName("arrow_rain_effect_duration_seconds") val rainEffectDurationSeconds: Int = 6,
  @SerialName("arrow_rain_effect_amplifier_min") val rainEffectAmplifierMin: Int = 0,
  @SerialName("arrow_rain_effect_amplifier_max") val rainEffectAmplifierMax: Int = 1,
  @SerialName("arrow_rain_negative_effects") val rainNegativeEffects: List<String> = NEGATIVE_EFFECTS,

  /** A boss keeps no more minions alive than this; a summon only tops them up. */
  @SerialName("summoning_max_alive") val summoningMaxAlive: Int = 12,
  /** Whether minions that rolled into bosses summon their own. Off keeps a fight from snowballing. */
  @SerialName("summoning_by_mini_bosses") val summoningByMiniBosses: Boolean = false,

  @SerialName("is_shockwave_enabled") val shockwaveEnabled: Boolean = true,
  @SerialName("shockwave_cooldown") val shockwaveCooldown: Long = 9000,
  @SerialName("shockwave_cooldown_bias") val shockwaveCooldownBias: Double = 1.3,
  @SerialName("shockwave_radius") val shockwaveRadius: Double = 5.0,
  @SerialName("shockwave_damage_base") val shockwaveDamageBase: Double = 3.0,
  @SerialName("shockwave_damage_per_level") val shockwaveDamagePerLevel: Double = 0.08,
  @SerialName("shockwave_damage_max") val shockwaveDamageMax: Double = 14.0,
  @SerialName("shockwave_knockback") val shockwaveKnockback: Double = 1.2,
  @SerialName("shockwave_lift") val shockwaveLift: Double = 0.5,

  @SerialName("is_frost_nova_enabled") val frostNovaEnabled: Boolean = true,
  @SerialName("frost_nova_cooldown") val frostNovaCooldown: Long = 14000,
  @SerialName("frost_nova_cooldown_bias") val frostNovaCooldownBias: Double = 1.2,
  @SerialName("frost_nova_radius") val frostNovaRadius: Double = 6.0,
  @SerialName("frost_nova_seconds_base") val frostNovaSecondsBase: Double = 3.0,
  @SerialName("frost_nova_seconds_per_level") val frostNovaSecondsPerLevel: Double = 0.05,
  @SerialName("frost_nova_seconds_max") val frostNovaSecondsMax: Double = 8.0,
  @SerialName("frost_nova_slowness_amplifier") val frostNovaSlownessAmplifier: Int = 1,

  @SerialName("is_grapple_enabled") val grappleEnabled: Boolean = true,
  @SerialName("grapple_cooldown") val grappleCooldown: Long = 12000,
  @SerialName("grapple_cooldown_bias") val grappleCooldownBias: Double = 1.3,
  /** Pulls the farthest player in sight within this range, but only one at least min_distance away. */
  @SerialName("grapple_range") val grappleRange: Double = 24.0,
  @SerialName("grapple_min_distance") val grappleMinDistance: Double = 6.0,
  @SerialName("grapple_strength") val grappleStrength: Double = 1.2,

  @SerialName("is_enrage_enabled") val enrageEnabled: Boolean = true,
  /** Once, when the boss drops below this share of its health. */
  @SerialName("enrage_health_share") val enrageHealthShare: Double = 0.3,
  @SerialName("enrage_strength_amplifier_base") val enrageStrengthBase: Int = 0,
  @SerialName("enrage_strength_amplifier_per_level") val enrageStrengthPerLevel: Double = 0.02,
  @SerialName("enrage_strength_amplifier_max") val enrageStrengthMax: Int = 2,
  @SerialName("enrage_speed_amplifier") val enrageSpeedAmplifier: Int = 0,
) {
  companion object {
    val NEGATIVE_EFFECTS = listOf(
      "SLOWNESS", "POISON", "WEAKNESS", "MINING_FATIGUE", "BLINDNESS", "HUNGER",
      "WITHER", "LEVITATION", "NAUSEA", "DARKNESS", "UNLUCK", "BAD_OMEN",
    )

    fun parse(text: String, warn: (String) -> Unit = {}): AbilitySettings = Yamls.decode(serializer(), text, warn)
  }
}
