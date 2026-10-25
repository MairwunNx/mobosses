package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets

/**
 * The boss yanks the farthest player it fights and can see toward itself: an answer to shooting it from a pillar or
 * across a ravine, where a boss could never reach back.
 */
object Grapple : Ability {
  override val key = "grapple"
  override val message = Message.GRAPPLE
  override fun enabled(settings: AbilitySettings) = settings.grappleEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.grapple
  override fun cooldown(settings: AbilitySettings) = settings.grappleCooldown to settings.grappleCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val target = cast.foes(cfg.grappleRange)
      .filter { it.position().distanceTo(cast.at) in cfg.grappleMinDistance..cfg.grappleRange && cast.entity.hasLineOfSight(it) }
      .maxByOrNull { it.position().distanceToSqr(cast.at) } ?: return false
    return pull(target, cast.at, cfg.grappleStrength, cfg.grappleRange)
  }

  /** Yanks [target] toward [to]; false when they stand right above or below it, with nowhere to pull. */
  fun pull(target: ServerPlayer, to: Vec3, strength: Double, range: Double): Boolean {
    val pull = to.subtract(target.position())
    val flat = Vec3(pull.x, 0.0, pull.z)
    if (flat.lengthSqr() < 1e-4) return false
    val distance = pull.length()
    val above = target.y - to.y
    // Off a pillar the pull drags down, not up; slow falling, long enough for the height, keeps the drop from killing.
    val lift = if (above > 2) 0.1 else 0.4 + distance / 40
    val speed = strength * (0.6 + distance / range.coerceAtLeast(1.0)).coerceAtMost(1.6)
    Targets.push(target, flat.normalize().scale(speed).add(0.0, lift, 0.0))
    if (above > 2) target.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, (above * 4 + 40).toInt(), 0))
    val level = target.level() as? ServerLevel ?: return true
    World.particles(level, ParticleTypes.CRIT, target.position().add(0.0, 1.0, 0.0), 20, 0.3, 0.5, 0.3, 0.1)
    World.sound(level, target.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 1f, 0.6f)
    return true
  }
}
