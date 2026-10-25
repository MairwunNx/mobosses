package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.protect.Guard
import kotlin.math.floor

/** The boss steps out of thin air two blocks behind the player it fights, within 15 blocks, on solid ground it fits on. Spiders don't: they climb instead. */
object TeleportStrike : Ability {
  override val key = "teleport_strike"
  override val message = Message.TELEPORT_STRIKE
  override fun enabled(settings: AbilitySettings) = settings.teleportStrikeEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.teleportStrike
  override fun cooldown(settings: AbilitySettings) = settings.teleportStrikeCooldown to settings.teleportStrikeCooldownBias

  // A cave spider is a spider too.
  override fun usable(cast: Cast) = cast.entity !is Spider

  fun fits(cast: Cast, at: Vec3): Boolean = fits(cast.entity, cast.world, cast.mod.guards, at)

  /**
   * Somewhere the boss can stand: solid ground under it, nothing in the way of its whole body, no water or lava, and
   * no protected area it couldn't spawn in. Not over a ravine, not in a wall, not on the other side of one.
   */
  fun fits(entity: LivingEntity, level: ServerLevel, guard: Guard, at: Vec3): Boolean {
    val pos = BlockPos.containing(at)
    if (!Targets.solid(level, pos.below()) || !level.getFluidState(pos).isEmpty || !level.getFluidState(pos.above()).isEmpty) return false
    val box = entity.boundingBox.move(at.subtract(entity.position()))
    return level.noCollision(entity, box) && guard.canSpawnBoss(level, pos)
  }

  /** A block's center at its floor, where an entity stands on it. */
  fun center(at: Vec3) = Vec3(floor(at.x) + 0.5, floor(at.y), floor(at.z) + 0.5)

  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(15.0) ?: return false
    val facing = target.lookAngle.multiply(1.0, 0.0, 1.0)
    if (facing.lengthSqr() < 1e-6) return false
    val behind = target.position().subtract(facing.normalize().scale(2.0))
    val to = (-2..3).map { center(behind.add(0.0, it.toDouble(), 0.0)) }
      .firstOrNull { fits(cast, it) && sees(cast, target.eyePosition, it) } ?: return false
    cast.particles(ParticleTypes.PORTAL, cast.at, 30, 0.5, 1.0, 0.5, 0.1)
    cast.sound(SoundEvents.ENDERMAN_TELEPORT)
    val look = target.position().subtract(to)
    val yaw = (Math.toDegrees(kotlin.math.atan2(-look.x, look.z))).toFloat()
    // Its name labels ride along: they live in the players' games, on the mob.
    cast.entity.teleportTo(cast.world, to.x, to.y, to.z, emptySet(), yaw, 0f, true)
    cast.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f, to)
    return true
  }

  /** Whether [eye] sees a spot one block above [point]: no block in between, as Bukkit's `hasLineOfSight` to a place. */
  fun sees(cast: Cast, eye: Vec3, point: Vec3): Boolean =
    cast.world.clip(ClipContext(eye, point.add(0.0, 1.0, 0.0), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cast.entity)).type == HitResult.Type.MISS
}
