package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The zombie breathes rot: hunger, weakness, and a moment of nausea for players close by. */
object Infection : Signature("infection", Message.INFECTION) {
  override fun cast(cast: Cast): Boolean {
    val seconds = ticks(cast, 5.0, 0.06, 12.0)
    val sick = afflict(cast, 5.0, { MobEffectInstance(MobEffects.HUNGER, seconds, 1) }, { MobEffectInstance(MobEffects.WEAKNESS, seconds, 0) }, { MobEffectInstance(MobEffects.NAUSEA, 80, 0) })
    if (!sick) return false
    cast.particles(ParticleTypes.SNEEZE, cast.at.add(0.0, 1.5, 0.0), 40, 1.5, 0.5, 1.5, 0.02)
    cast.sound(SoundEvents.ZOMBIE_INFECT)
    return true
  }
}
