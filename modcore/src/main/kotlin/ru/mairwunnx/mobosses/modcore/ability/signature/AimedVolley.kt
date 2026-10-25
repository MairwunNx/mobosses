package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** Archers loose a fan of arrows at the player they fight; the arrows hit players only. */
object AimedVolley : Signature("aimed_volley", Message.AIMED_VOLLEY) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(32.0) ?: return false
    val damage = scaled(cast, 3.0, 0.06, 9.0)
    val count = (5 + cast.level / 20).coerceAtMost(10)
    repeat(count) { arrow(cast, aim(cast, target, 0.12).scale(2.2), damage, 6) }
    cast.sound(SoundEvents.CROSSBOW_SHOOT, 1f, 1.2f)
    return true
  }
}
