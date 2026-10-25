package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import kotlin.math.min

/**
 * Cold bursts out of the boss: players around it freeze as in powder snow, with the frosted screen and the slow
 * damage, and slow down. Leather armor keeps the freezing off, as in the game.
 */
object FrostNova : Ability {
  override val key = "frost_nova"
  override val message = Message.FROST_NOVA
  override fun enabled(settings: AbilitySettings) = settings.frostNovaEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.frostNova
  override fun cooldown(settings: AbilitySettings) = settings.frostNovaCooldown to settings.frostNovaCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val victims = cast.victims(cast.at, cfg.frostNovaRadius).filter { it.position().distanceTo(cast.at) <= cfg.frostNovaRadius }
    if (victims.isEmpty()) return false
    val seconds = min(cfg.frostNovaSecondsBase + cfg.frostNovaSecondsPerLevel * cast.level, cfg.frostNovaSecondsMax)
    val ticks = (seconds * 20).toInt()
    cast.particles(ParticleTypes.SNOWFLAKE, cast.at.add(0.0, 1.0, 0.0), 120, cfg.frostNovaRadius / 2, 0.6, cfg.frostNovaRadius / 2, 0.02)
    cast.sound(SoundEvents.GLASS_BREAK, 1f, 0.6f)
    victims.forEach { victim ->
      // Frozen entities thaw by two ticks a tick; this keeps them fully frozen for the duration.
      victim.ticksFrozen = victim.ticksRequiredToFreeze + ticks * 2
      victim.addEffect(MobEffectInstance(MobEffects.SLOWNESS, ticks, cfg.frostNovaSlownessAmplifier.coerceAtLeast(0)))
    }
    return true
  }
}
