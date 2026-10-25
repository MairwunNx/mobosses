package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.kinds.TeleportStrike
import kotlin.math.atan2

/** The enderman trades places with the player it fights, blinding them for a moment. */
object Swap : Signature("swap", Message.SWAP) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foes(24.0).firstOrNull { it.position().distanceToSqr(cast.at) > 16 } ?: return false
    val bossAt = cast.at
    val playerAt = target.position()
    if (!cast.entity.hasLineOfSight(target) || !TeleportStrike.fits(cast, playerAt)) return false
    if (!cast.entity.teleportTo(cast.world, playerAt.x, playerAt.y, playerAt.z, emptySet(), target.yRot, cast.entity.xRot, true)) return false
    // The player lands where the boss stood, facing where they were.
    val look = playerAt.subtract(bossAt)
    target.teleportTo(cast.world, bossAt.x, bossAt.y, bossAt.z, emptySet(), Math.toDegrees(atan2(-look.x, look.z)).toFloat(), target.xRot, true)
    target.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 30, 0), cast.entity)
    listOf(bossAt, playerAt).forEach { cast.particles(ParticleTypes.PORTAL, it, 40, 0.5, 1.0, 0.5, 0.2) }
    cast.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f)
    return true
  }
}
