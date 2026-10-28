package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.BlockTags
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fire in a ring around each player the boss fights, however far, for a few seconds, as on Paper: on the ground, next
 * to grass and trees as `fire_ring_near` allows, and on water as flames dancing on it that burn whoever wades through.
 * Never next to anything else that burns, like a house, nor where protection forbids it. A ring with room for less
 * than a third of its fire doesn't burn, and with none burning the boss casts something else.
 */
object FireRing : Ability {
  override val key = "fire_ring"
  override val message = Message.FIRE_RING
  override fun enabled(settings: AbilitySettings) = settings.fireRingEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.fireRing
  override fun cooldown(settings: AbilitySettings) = settings.fireRingCooldown to settings.fireRingCooldownBias

  private const val POINTS = 64
  /** At most this many rings, around the nearest players the boss fights. */
  private const val RINGS = 4
  /** Where fire spreads from a block: a block to each side, one below, four above. */
  private val REACH = (-1..1).flatMap { dx -> (-1..4).flatMap { dy -> (-1..1).map { dz -> Triple(dx, dy, dz) } } }.filter { it != Triple(0, 0, 0) }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val world = cast.world
    val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)
    val ticks = cfg.fireRingSeconds.coerceAtLeast(1) * 20L
    val fire = Blocks.FIRE.defaultBlockState()
    val water = ArrayList<Vec3>()
    val rings = cast.foes().take(RINGS).count { foe ->
      val center = foe.position()
      val radius = if (cfg.fireRingMaxRadius > cfg.fireRingMinRadius) Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius) else cfg.fireRingMinRadius
      val spots = ArrayList<BlockPos>()
      val pools = ArrayList<Vec3>()
      for (i in 0 until POINTS) {
        if (Random.nextDouble() > density) continue
        val angle = 2 * PI * i / POINTS
        val x = center.x + radius * cos(angle)
        val z = center.z + radius * sin(angle)
        val above = Targets.ground(world, x, center.y, z)?.above()
        if (above != null && fits(cast, above)) spots += above
        else if (cfg.fireRingNear.water) Targets.water(world, x, center.y, z)?.let { pools += Vec3(it.x + 0.5, it.y + 1.0, it.z + 0.5) }
      }
      // A house, a claim, or a cliff all around leaves only a spark or two, which nobody would call a ring.
      if (spots.size + pools.size < POINTS * density / 3) return@count false.also { cast.mod.log.debug { "No fire ring around ${foe.plainTextName}: room for ${spots.size + pools.size} of ${POINTS * density}" } }
      // Temporary, since with fire spread off fire would never burn out.
      val lit = spots.count { cast.mod.blockChanges.setFor(world, it, fire, ticks) }
      water += pools
      lit + pools.size > 0
    }
    if (water.isNotEmpty()) burnWater(cast, water, ticks)
    if (rings > 0) cast.sound(SoundEvents.BLAZE_SHOOT, 1f, 0.8f)
    return rings > 0
  }

  private fun fits(cast: Cast, above: BlockPos): Boolean {
    val world = cast.world
    return !cast.entity.boundingBox.contains(Vec3(above.x + 0.5, above.y.toDouble(), above.z + 0.5)) && Targets.isOpen(world, above) &&
      REACH.all { (dx, dy, dz) -> mayBurnNear(cast.settings.fireRingNear, world.getBlockState(above.offset(dx, dy, dz))) } &&
      cast.mod.guards.canChangeBlock(world, above)
  }

  /** Whether fire may burn next to [state]: it doesn't burn, or it's a plant or a tree `fire_ring_near` lets burn. */
  private fun mayBurnNear(near: AbilitySettings.FireRingNear, state: BlockState): Boolean =
    !state.ignitedByLava() ||
      (near.plants && (state.`is`(BlockTags.REPLACEABLE) || state.`is`(BlockTags.FLOWERS) || state.`is`(BlockTags.SAPLINGS))) ||
      (near.trees && (state.`is`(BlockTags.LOGS) || state.`is`(BlockTags.LEAVES)))

  /** Flames dancing on the water for [ticks]: twice a second they burn whoever wades through, unless fire can't hurt them. */
  private fun burnWater(cast: Cast, points: List<Vec3>, ticks: Long) {
    val world = cast.world
    var tick = 0L
    cast.every(5) { task ->
      tick += 5
      if (tick > ticks) return@every task.cancel()
      points.forEach { World.particles(world, ParticleTypes.FLAME, it, 2, 0.25, 0.05, 0.25, 0.01) }
      if (tick % 10 != 0L) return@every
      points.forEach { World.particles(world, ParticleTypes.SMOKE, it, 1, 0.2, 0.2, 0.2, 0.0) }
      val burnt = HashSet<UUID>()
      points.forEach { at ->
        cast.victims(at, 1.2).forEach { victim ->
          if (burnt.add(victim.uuid) && !victim.hasEffect(MobEffects.FIRE_RESISTANCE)) victim.hurtServer(world, world.damageSources().mobAttack(cast.entity), 1f)
        }
      }
    }
  }
}
