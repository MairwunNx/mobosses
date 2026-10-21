package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Material
import org.bukkit.Sound
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Fire on the ground in a ring around the player the boss fights, however far, for a few seconds; never in place of water, next to anything that burns, or where protection forbids it. */
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
    val center = cast.foe()?.location ?: return false
    val radius = if (cfg.fireRingMaxRadius > cfg.fireRingMinRadius) Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius) else cfg.fireRingMinRadius
    val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)
    val fire = Material.FIRE.createBlockData()
    val lit = (0 until POINTS).count { i ->
      if (Random.nextDouble() > density) return@count false
      val angle = 2 * PI * i / POINTS
      val ground = Targets.ground(center.world, center.x + radius * cos(angle), center.y, center.z + radius * sin(angle)) ?: return@count false
      val above = ground.getRelative(0, 1, 0)
      !cast.boss.entity.boundingBox.contains(above.x + 0.5, above.y.toDouble(), above.z + 0.5) && Targets.isOpen(above) &&
        // Nothing within the fire's reach may burn: the ring scorches the ground, not a house.
        REACH.none { (dx, dy, dz) -> above.getRelative(dx, dy, dz).type.isFlammable } &&
        // Temporary, since with doFireTick off fire would never burn out.
        cast.plugin.blockChanges.setFor(above, fire, cfg.fireRingSeconds.coerceAtLeast(1) * 20L)
    }
    if (lit > 0) cast.sound(Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f)
    return lit > 0
  }
}
