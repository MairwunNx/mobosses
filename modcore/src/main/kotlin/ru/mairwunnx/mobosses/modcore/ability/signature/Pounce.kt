package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** Spiders leap at the player they fight and bite where they land: slowness, or poison from a cave spider. */
object Pounce : Signature("pounce", Message.POUNCE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foes(16.0).firstOrNull { it.position().distanceToSqr(cast.at) > 9 } ?: return false
    val leap = target.position().subtract(cast.at)
    val flat = leap.multiply(1.0, 0.0, 1.0)
    if (flat.lengthSqr() < 1e-4) return false
    cast.push(cast.entity, flat.normalize().scale((leap.length() / 8).coerceIn(0.6, 1.6)).add(0.0, 0.6, 0.0))
    cast.sound(SoundEvents.SPIDER_AMBIENT, 1f, 0.6f)
    cast.later(14) {
      val poison = cast.entity.type == EntityTypes.CAVE_SPIDER
      around(cast, 2.5).forEach {
        strike(cast, it, scaled(cast, 3.0, 0.06, 9.0))
        it.addEffect(MobEffectInstance(if (poison) MobEffects.POISON else MobEffects.SLOWNESS, ticks(cast, 3.0, 0.04, 7.0), if (cast.level >= 50) 1 else 0), cast.entity)
      }
    }
    return true
  }
}
