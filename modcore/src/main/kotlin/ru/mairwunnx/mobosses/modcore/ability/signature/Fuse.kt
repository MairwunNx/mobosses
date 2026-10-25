package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The creeper's fuse catches early: it gets fast and hard to stop for a few seconds. */
object Fuse : Signature("fuse", Message.FUSE) {
  override fun cast(cast: Cast): Boolean {
    if (cast.foe(12.0) == null) return false
    val duration = ticks(cast, 3.0, 0.03, 6.0)
    cast.entity.addEffect(MobEffectInstance(MobEffects.SPEED, duration, 1))
    cast.entity.addEffect(MobEffectInstance(MobEffects.RESISTANCE, duration, 1))
    cast.particles(ParticleTypes.ELECTRIC_SPARK, cast.at.add(0.0, 1.0, 0.0), 30, 0.4, 0.6, 0.4, 0.1)
    cast.sound(SoundEvents.CREEPER_PRIMED, 1f, 0.8f)
    return true
  }
}
