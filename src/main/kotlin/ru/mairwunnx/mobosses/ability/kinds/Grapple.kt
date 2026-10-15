package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/**
 * The boss yanks the farthest player it can see toward itself: an answer to shooting it from a
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
    val target = cast.players(cast.at, cfg.grappleRange)
      .filter { it.location.distance(cast.at) in cfg.grappleMinDistance..cfg.grappleRange && entity.hasLineOfSight(it) }
      .maxByOrNull { it.location.distanceSquared(cast.at) } ?: return false
    val pull = cast.at.toVector().subtract(target.location.toVector())
    val distance = pull.length()
    target.velocity = pull.normalize().multiply(cfg.grappleStrength * (0.6 + distance / cfg.grappleRange)).setY(0.4 + distance / 40)
    target.world.spawnParticle(Particle.CRIT, target.location.add(0.0, 1.0, 0.0), 20, 0.3, 0.5, 0.3, 0.1)
    cast.sound(Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.6f, target.location)
    return true
  }
}
