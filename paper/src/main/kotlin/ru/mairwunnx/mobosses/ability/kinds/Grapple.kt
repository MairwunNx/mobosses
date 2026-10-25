package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/**
 * The boss yanks the farthest player it fights and can see toward itself: an answer to shooting it from a
 * pillar or across a ravine, where a boss could never reach back.
 */
object Grapple : Ability {
  override val key = "grapple"
  override val message = Message.GRAPPLE
  override fun enabled(settings: AbilitySettings) = settings.grappleEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.grapple
  override fun cooldown(settings: AbilitySettings) = settings.grappleCooldown to settings.grappleCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val entity = cast.boss.entity
    val target = cast.foes(cfg.grappleRange)
      .filter { it.location.distance(cast.at) in cfg.grappleMinDistance..cfg.grappleRange && entity.hasLineOfSight(it) }
      .maxByOrNull { it.location.distanceSquared(cast.at) } ?: return false
    return pull(target, cast.at, cfg.grappleStrength, cfg.grappleRange)
  }

  /** Yanks [target] toward [to]; false when they stand right above or below it, with nowhere to pull. */
  fun pull(target: Player, to: Location, strength: Double, range: Double): Boolean {
    val pull = to.toVector().subtract(target.location.toVector())
    if (pull.clone().setY(0).lengthSquared() < 1e-4) return false
    val distance = pull.length()
    val above = target.location.y - to.y
    // Off a pillar the pull drags down, not up; slow falling, long enough for the height, keeps the drop from killing.
    val lift = if (above > 2) 0.1 else 0.4 + distance / 40
    target.velocity = pull.setY(0).normalize().multiply(strength * (0.6 + distance / range.coerceAtLeast(1.0)).coerceAtMost(1.6)).setY(lift)
    if (above > 2) target.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, (above * 4 + 40).toInt(), 0))
    target.world.spawnParticle(Particle.CRIT, target.location.add(0.0, 1.0, 0.0), 20, 0.3, 0.5, 0.3, 0.1)
    target.world.playSound(target.location, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.6f)
    return true
  }
}
