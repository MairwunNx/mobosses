package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/**
 * The blaze fires a burst of small fireballs at the player it fights; they burn players, not blocks
 * ([ru.mairwunnx.mobosses.modcore.mixin.FireballMixin] keeps an ability's fireball from lighting the ground).
 */
object FireballBarrage : Signature("fireball_barrage", Message.FIREBALL_BARRAGE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(24.0) ?: return false
    val count = (3 + cast.level / 25).coerceAtMost(7)
    repeat(count) { i ->
      cast.later(i * 4L) {
        if (!cast.canHit(target) || target.level() != cast.world) return@later
        val direction = aim(cast, target, 0.08)
        launch(cast, SmallFireball(cast.world, cast.entity, direction), direction)
        cast.sound(SoundEvents.BLAZE_SHOOT)
      }
    }
    return true
  }
}
