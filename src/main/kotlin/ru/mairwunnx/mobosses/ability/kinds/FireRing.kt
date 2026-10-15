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

/** Fire on the ground in a ring around the nearest player, or around the boss; never in place of water, never where protection forbids it. */
object FireRing : Ability {
  override val key = "fire_ring"
  override val message = Message.FIRE_RING
  override fun enabled(settings: AbilitySettings) = settings.fireRingEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.fireRing
  override fun cooldown(settings: AbilitySettings) = settings.fireRingCooldown to settings.fireRingCooldownBias

  private const val POINTS = 64

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val center = (cast.nearest(32.0) ?: cast.boss.entity).location
    val radius = if (cfg.fireRingMaxRadius > cfg.fireRingMinRadius) Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius) else cfg.fireRingMinRadius
    val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)
    val fire = Material.FIRE.createBlockData()
    val lit = (0 until POINTS).count { i ->
      if (Random.nextDouble() > density) return@count false
      val angle = 2 * PI * i / POINTS
      val ground = Targets.ground(center.world, center.x + radius * cos(angle), center.y, center.z + radius * sin(angle)) ?: return@count false
      val above = ground.getRelative(0, 1, 0)
      !cast.boss.entity.boundingBox.contains(above.x + 0.5, above.y.toDouble(), above.z + 0.5) && Targets.isOpen(above) && cast.plugin.blockChanges.set(above, fire)
    }
    if (lit > 0) cast.sound(Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f)
    return lit > 0
  }
}
