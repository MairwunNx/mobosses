package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Bolts
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.min
import kotlin.random.Random

/**
 * A bolt on every player the boss fights, however far, the rest scattered around them, now and then right into one of
 * them, up to three a player ([Bolts]); a lightning rod or copper next to a player draws that player's bolts. With no
 * one to strike, around the boss. The bolt is only the flash, so fire is the ability's own: by chance, for a few
 * seconds, and never near a claim.
 */
object Lightning : Ability {
  override val key = "lightning"
  override val message = Message.LIGHTNING
  override fun enabled(settings: AbilitySettings) = settings.lightningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.lightning
  override fun cooldown(settings: AbilitySettings) = settings.lightningCooldown to settings.lightningCooldownBias

  /** What draws lightning, as in 1.x: the rod, copper blocks, and cut copper. */
  private val COPPER = setOf(
    Material.COPPER_BLOCK, Material.CUT_COPPER, Material.EXPOSED_COPPER, Material.EXPOSED_CUT_COPPER,
    Material.OXIDIZED_COPPER, Material.OXIDIZED_CUT_COPPER, Material.WAXED_COPPER_BLOCK, Material.WAXED_CUT_COPPER,
    Material.WAXED_EXPOSED_COPPER, Material.WAXED_EXPOSED_CUT_COPPER, Material.WAXED_OXIDIZED_COPPER, Material.WAXED_OXIDIZED_CUT_COPPER,
  )

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val radius = min(cfg.lightningRadius + cast.level * cfg.lightningRadiusModifier, cfg.lightningRadiusMax)
    val strikes = Rolls.linear(cfg.lightningStrikes, cfg.lightningStrikesModifier, cfg.lightningStrikesMax, cast.level)
    val damage = Rolls.linear(cfg.lightningDamage, cfg.lightningDamageModifier, cfg.lightningDamageMax, cast.level)
    val foes = cast.foes()
    val centers = foes.map { it.location }.ifEmpty { listOf(cast.at) }.map { attractor(it, min(5.0, radius)) ?: it }
    val points = centers + Bolts.aim(foes.size, strikes - centers.size, cfg.lightningAimed).map { aimed ->
      aimed?.let { centers[it].clone() } ?: run {
        val center = centers.random()
        val x = center.x + Random.nextDouble(-radius, radius)
        val z = center.z + Random.nextDouble(-radius, radius)
        Location(center.world, x, Targets.ground(center.world, x, center.y, z)?.let { it.y + 1.0 } ?: center.y, z)
      }
    }
    val fire = cfg.lightningFire
    points.forEach { point ->
      point.world.strikeLightningEffect(point)
      cast.victims(point, 2.5).forEach { it.damage(damage, cast.boss.entity) }
      if (Random.nextDouble() < fire.chance) ignite(cast, point.block, fire)
    }
    cast.sound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER)
    return true
  }

  /** Fire where a bolt struck, as the game's lightning sets, but only for a while and never within reach of a claim. */
  private fun ignite(cast: Cast, block: Block, fire: AbilitySettings.LightningFire) {
    val below = block.getRelative(0, -1, 0)
    if (!Targets.isOpen(block) || !below.type.isSolid || !clearOfClaims(cast, block, fire.claimDistance)) return
    cast.plugin.blockChanges.setFor(block, Material.FIRE.createBlockData(), fire.seconds.coerceAtLeast(1) * 20L)
  }

  /** Whether every block within [distance] of [block], every other one, may change: fire spreads, so it stays off a claim's edge. */
  private fun clearOfClaims(cast: Cast, block: Block, distance: Int): Boolean {
    val d = distance.coerceAtLeast(0)
    for (dx in -d..d step 2) for (dz in -d..d step 2) {
      if (!cast.plugin.guards.canChangeBlock(block.getRelative(dx, 0, dz).location)) return false
    }
    return true
  }

  /** The block that draws lightning within [radius] around [at]'s height: a rod first, then copper, nearest first. */
  private fun attractor(at: Location, radius: Double): Location? {
    val r = radius.toInt()
    val blocks = (-r..r).flatMap { dx -> (-r..r).flatMap { dz -> (-3..3).map { dy -> Triple(dx, dy, dz) } } }
      .filter { (dx, _, dz) -> dx * dx + dz * dz <= radius * radius }
      .map { (dx, dy, dz) -> at.world.getBlockAt(at.blockX + dx, at.blockY + dy, at.blockZ + dz) }
      .filter { it.type == Material.LIGHTNING_ROD || it.type in COPPER }
    val best = blocks.minWithOrNull(compareBy({ it.type != Material.LIGHTNING_ROD }, { it.location.distanceSquared(at) })) ?: return null
    return best.location.add(0.5, 1.0, 0.5)
  }
}
