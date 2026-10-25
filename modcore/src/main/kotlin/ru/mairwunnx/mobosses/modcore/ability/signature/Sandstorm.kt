package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The husk raises a sandstorm: players around it can't see and get hungry. */
object Sandstorm : Signature("sandstorm", Message.SANDSTORM) {
  override fun cast(cast: Cast): Boolean {
    val blind = ticks(cast, 2.0, 0.02, 4.0)
    val hunger = ticks(cast, 6.0, 0.08, 14.0)
    if (!afflict(cast, 8.0, { MobEffectInstance(MobEffects.BLINDNESS, blind, 0) }, { MobEffectInstance(MobEffects.HUNGER, hunger, 1) })) return false
    cast.particles(ParticleTypes.WHITE_ASH, cast.at.add(0.0, 1.0, 0.0), 200, 4.0, 1.5, 4.0, 0.05)
    cast.sound(SoundEvents.WEATHER_RAIN, 1f, 0.5f)
    return true
  }
}
