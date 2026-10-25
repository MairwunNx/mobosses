package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The phantom brings the night down: darkness and a moment of blindness. */
object Terror : Signature("terror", Message.TERROR) {
  override fun cast(cast: Cast): Boolean {
    val dark = ticks(cast, 5.0, 0.05, 10.0)
    if (!afflict(cast, 12.0, { MobEffectInstance(MobEffects.DARKNESS, dark, 0) }, { MobEffectInstance(MobEffects.BLINDNESS, 30, 0) })) return false
    cast.sound(SoundEvents.PHANTOM_SWOOP, 1f, 0.6f)
    return true
  }
}
