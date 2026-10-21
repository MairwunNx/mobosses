package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.min
import kotlin.random.Random

/**
 * A bolt on every player the boss fights, however far, the rest scattered around them; a lightning rod or
 * copper next to a player draws that player's bolt. With no one to strike, around the boss. Only the flash:
 * a real bolt would set fires.
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
    val centers = cast.foes().map { it.location }.ifEmpty { listOf(cast.at) }.map { attractor(it, min(5.0, radius)) ?: it }
    val points = centers + List((strikes - centers.size).coerceAtLeast(0)) {
      val center = centers.random()
      val x = center.x + Random.nextDouble(-radius, radius)
      val z = center.z + Random.nextDouble(-radius, radius)
      Location(center.world, x, Targets.ground(center.world, x, center.y, z)?.let { it.y + 1.0 } ?: center.y, z)
    }
    points.forEach { point ->
      point.world.strikeLightningEffect(point)
      cast.victims(point, 2.5).forEach { it.damage(damage, cast.boss.entity) }
    }
    cast.sound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER)
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
