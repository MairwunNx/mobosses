package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/** The boss slams the ground: players around it take damage and fly back, the closer the harder. */
object Shockwave : Ability {
  override val key = "shockwave"
  override val message = Message.SHOCKWAVE
  override fun enabled(settings: AbilitySettings) = settings.shockwaveEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.shockwave
  override fun cooldown(settings: AbilitySettings) = settings.shockwaveCooldown to settings.shockwaveCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val players = cast.players(cast.at, cfg.shockwaveRadius).filter { it.location.distance(cast.at) <= cfg.shockwaveRadius }
    if (players.isEmpty()) return false
    val damage = Rolls.linear(cfg.shockwaveDamageBase, cfg.shockwaveDamagePerLevel, cfg.shockwaveDamageMax, cast.level)
    cast.at.world.spawnParticle(Particle.EXPLOSION, cast.at, 3, cfg.shockwaveRadius / 3, 0.2, cfg.shockwaveRadius / 3, 0.0)
    cast.sound(Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f)
    players.forEach { player ->
      val away = player.location.toVector().subtract(cast.at.toVector()).setY(0)
      val closeness = 1.0 - (away.length() / cfg.shockwaveRadius).coerceIn(0.0, 1.0)
      player.damage(damage * (0.5 + 0.5 * closeness), cast.boss.entity)
      val push = if (away.lengthSquared() < 1e-4) away.setX(1.0) else away.normalize()
      player.velocity = push.multiply(cfg.shockwaveKnockback * (0.5 + 0.5 * closeness)).setY(cfg.shockwaveLift)
    }
    return true
  }
}
