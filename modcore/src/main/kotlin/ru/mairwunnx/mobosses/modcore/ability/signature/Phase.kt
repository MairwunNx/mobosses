package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The vex fades from sight for a few seconds and speeds up. */
object Phase : Signature("phase", Message.PHASE) {
  override fun cast(cast: Cast): Boolean {
    val duration = ticks(cast, 3.0, 0.03, 6.0)
    cast.entity.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false))
    cast.entity.addEffect(MobEffectInstance(MobEffects.SPEED, duration, 1, false, false))
    cast.particles(ParticleTypes.WITCH, cast.at.add(0.0, 0.5, 0.0), 30, 0.4, 0.4, 0.4)
    cast.sound(SoundEvents.VEX_CHARGE, 1f, 1.5f)
    return true
  }
}
