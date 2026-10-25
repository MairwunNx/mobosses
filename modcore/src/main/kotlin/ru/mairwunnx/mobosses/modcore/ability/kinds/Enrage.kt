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
import kotlin.math.roundToInt

/**
 * Once its health drops below `enrage_health_share`, the boss hits harder and moves faster until it dies. It happens
 * once: the boss remembers it, across restarts too.
 */
object Enrage : Ability {
  override val key = "enrage"
  override val message = Message.ENRAGE
  override fun enabled(settings: AbilitySettings) = settings.enrageEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.enrage
  override fun cooldown(settings: AbilitySettings) = 0L to 1.0

  override fun usable(cast: Cast) = !cast.boss.enraged && cast.entity.health <= cast.entity.maxHealth * cast.settings.enrageHealthShare

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val strength = (cfg.enrageStrengthBase + cfg.enrageStrengthPerLevel * cast.level).roundToInt().coerceIn(0, cfg.enrageStrengthMax.coerceAtLeast(0))
    cast.entity.addEffect(MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, strength, false, true, true))
    cast.entity.addEffect(MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, cfg.enrageSpeedAmplifier.coerceAtLeast(0), false, true, true))
    cast.boss.enraged = true
    cast.particles(ParticleTypes.ANGRY_VILLAGER, cast.at.add(0.0, 1.5, 0.0), 12, 0.5, 0.5, 0.5)
    cast.sound(SoundEvents.RAVAGER_ROAR, 1f, 1.2f)
    return true
  }
}
