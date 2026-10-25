package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The boss charges at the player it fights and tramples whoever stands in its way at the end. */
object Stampede : Signature("stampede", Message.STAMPEDE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foes(18.0).firstOrNull { it.position().distanceToSqr(cast.at) > 16 } ?: return false
    val dash = target.position().subtract(cast.at).multiply(1.0, 0.0, 1.0).takeIf { it.lengthSqr() > 1e-4 }?.normalize() ?: return false
    cast.push(cast.entity, dash.scale(1.8).add(0.0, 0.25, 0.0))
    cast.sound(SoundEvents.RAVAGER_ROAR, 1f, 1f)
    cast.later(10) {
      around(cast, 2.8).forEach {
        strike(cast, it, scaled(cast, 5.0, 0.1, 16.0))
        knock(cast, it, 1.3, 0.6)
      }
    }
    return true
  }
}
