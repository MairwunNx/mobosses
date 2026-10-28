package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fire on the ground in a ring around the player the boss fights, however far, for a few seconds; never in place of water,
 * next to anything that burns, or where protection forbids it. Where that leaves less than a third of a ring, the boss
 * casts something else.
 */
object FireRing : Ability {
  override val key = "fire_ring"
  override val message = Message.FIRE_RING
  override fun enabled(settings: AbilitySettings) = settings.fireRingEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.fireRing
  override fun cooldown(settings: AbilitySettings) = settings.fireRingCooldown to settings.fireRingCooldownBias

  private const val POINTS = 64
  /** Where fire spreads from a block: a block to each side, one below, four above. */
  private val REACH = (-1..1).flatMap { dx -> (-1..4).flatMap { dy -> (-1..1).map { dz -> Triple(dx, dy, dz) } } }.filter { it != Triple(0, 0, 0) }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val world = cast.world
    val center = cast.foe()?.position() ?: return false
    val radius = if (cfg.fireRingMaxRadius > cfg.fireRingMinRadius) Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius) else cfg.fireRingMinRadius
    val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)
    val fire = Blocks.FIRE.defaultBlockState()
    val spots = (0 until POINTS).mapNotNull { i ->
      if (Random.nextDouble() > density) return@mapNotNull null
      val angle = 2 * PI * i / POINTS
      val ground = Targets.ground(world, center.x + radius * cos(angle), center.y, center.z + radius * sin(angle)) ?: return@mapNotNull null
      ground.above().takeIf { above ->
        !cast.entity.boundingBox.contains(Vec3(above.x + 0.5, above.y.toDouble(), above.z + 0.5)) && Targets.isOpen(world, above) &&
          // Nothing within the fire's reach may burn: the ring scorches the ground, not a house.
          REACH.none { (dx, dy, dz) -> world.getBlockState(above.offset(dx, dy, dz)).ignitedByLava() } && cast.mod.guards.canChangeBlock(world, above)
      }
    }
    // Grass, leaves, or a claim all around leave only a spark or two, which nobody would call a ring: something else instead.
    if (spots.size < POINTS * density / 3) return false.also { cast.mod.log.debug { "No fire ring for ${cast.boss.type.key}: room for ${spots.size} of ${POINTS * density}" } }
    // Temporary, since with fire spread off fire would never burn out.
    val lit = spots.count { cast.mod.blockChanges.setFor(world, it, fire, cfg.fireRingSeconds.coerceAtLeast(1) * 20L) }
    if (lit > 0) cast.sound(SoundEvents.BLAZE_SHOOT, 1f, 0.8f)
    return lit > 0
  }
}
