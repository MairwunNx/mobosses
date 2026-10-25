package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The wither skeleton gives off a withering cloud. */
object WitherAura : Signature("wither_aura", Message.WITHER_AURA) {
  override fun cast(cast: Cast): Boolean {
    val ticks = ticks(cast, 4.0, 0.05, 9.0)
    if (!afflict(cast, 6.0, { MobEffectInstance(MobEffects.WITHER, ticks, if (cast.level >= 60) 1 else 0) })) return false
    cast.particles(ParticleTypes.LARGE_SMOKE, cast.at.add(0.0, 1.0, 0.0), 60, 3.0, 0.8, 3.0, 0.01)
    cast.sound(SoundEvents.WITHER_AMBIENT, 0.7f, 1.4f)
    return true
  }
}
