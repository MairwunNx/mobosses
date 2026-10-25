package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.projectile.EvokerFangs
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import kotlin.math.atan2

/** The evoker sends a line of fangs along the ground toward the player it fights. */
object FangLine : Signature("fang_line", Message.FANG_LINE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(20.0) ?: return false
    val flat = target.position().subtract(cast.at).multiply(1.0, 0.0, 1.0)
    if (flat.lengthSqr() < 1e-4) return false
    val step = flat.normalize().scale(1.25)
    val facing = atan2(step.z, step.x).toFloat()
    val count = (8 + cast.level / 15).coerceAtMost(16)
    for (i in 1..count) {
      cast.later(i.toLong()) {
        val point = cast.at.add(step.scale(i.toDouble()))
        val ground = Targets.ground(cast.world, point.x, point.y, point.z, 4) ?: return@later
        cast.world.addFreshEntity(EvokerFangs(cast.world, point.x, ground.y + 1.0, point.z, facing, 0, cast.entity))
      }
    }
    cast.sound(SoundEvents.EVOKER_PREPARE_ATTACK)
    return true
  }
}
