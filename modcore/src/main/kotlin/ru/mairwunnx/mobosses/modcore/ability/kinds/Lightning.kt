package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import kotlin.math.min
import kotlin.random.Random

/**
 * A bolt on every player the boss fights, however far, the rest scattered around them; a lightning rod or copper next
 * to a player draws that player's bolt. With no one to strike, around the boss. Only the flash: a real bolt would set
 * fires.
 */
object Lightning : Ability {
  override val key = "lightning"
  override val message = Message.LIGHTNING
  override fun enabled(settings: AbilitySettings) = settings.lightningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.lightning
  override fun cooldown(settings: AbilitySettings) = settings.lightningCooldown to settings.lightningCooldownBias

  /** What draws lightning, as in 1.x: copper blocks and cut copper, plain, exposed, and oxidized, waxed or not. */
  private val COPPER: Set<Block> = listOf(Blocks.COPPER_BLOCK, Blocks.CUT_COPPER).flatMap { family ->
    listOf(family.weathering(), family.waxed()).flatMap { listOf(it.unaffected(), it.exposed(), it.oxidized()) }
  }.toSet()

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val world = cast.world
    val radius = min(cfg.lightningRadius + cast.level * cfg.lightningRadiusModifier, cfg.lightningRadiusMax)
    val strikes = Rolls.linear(cfg.lightningStrikes, cfg.lightningStrikesModifier, cfg.lightningStrikesMax, cast.level)
    val damage = Rolls.linear(cfg.lightningDamage, cfg.lightningDamageModifier, cfg.lightningDamageMax, cast.level)
    val centers = cast.foes().map { it.position() }.ifEmpty { listOf(cast.at) }.map { attractor(world, it, min(5.0, radius)) ?: it }
    val points = centers + List((strikes - centers.size).coerceAtLeast(0)) {
      val center = centers.random()
      val x = center.x + Random.nextDouble(-radius, radius)
      val z = center.z + Random.nextDouble(-radius, radius)
      Vec3(x, Targets.ground(world, x, center.y, z)?.let { it.y + 1.0 } ?: center.y, z)
    }
    points.forEach { point ->
      EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED)?.let { bolt ->
        bolt.snapTo(point)
        bolt.setVisualOnly(true)
        world.addFreshEntity(bolt)
      }
      cast.victims(point, 2.5).forEach { it.hurtServer(world, world.damageSources().mobAttack(cast.entity), damage.toFloat()) }
    }
    cast.sound(SoundEvents.LIGHTNING_BOLT_THUNDER)
    return true
  }

  /** The block that draws lightning within [radius] around [at]'s height: a rod first, then copper, nearest first. */
  private fun attractor(level: ServerLevel, at: Vec3, radius: Double): Vec3? {
    val r = radius.toInt()
    val base = BlockPos.containing(at)
    val blocks = (-r..r).flatMap { dx -> (-r..r).flatMap { dz -> (-3..3).map { dy -> Triple(dx, dy, dz) } } }
      .filter { (dx, _, dz) -> dx * dx + dz * dz <= radius * radius }
      .map { (dx, dy, dz) -> base.offset(dx, dy, dz) }
      .filter { level.getBlockState(it).let { state -> rod(state.block) || state.block in COPPER } }
    val best = blocks.minWithOrNull(compareBy({ !rod(level.getBlockState(it).block) }, { Vec3.atCenterOf(it).distanceToSqr(at) })) ?: return null
    return Vec3(best.x + 0.5, best.y + 1.0, best.z + 0.5)
  }

  /** Every lightning rod draws, weathered or not. */
  private fun rod(block: Block) = block.defaultBlockState().`is`(BlockTags.LIGHTNING_RODS)
}
