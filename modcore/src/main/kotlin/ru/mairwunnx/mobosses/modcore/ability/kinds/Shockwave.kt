package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The boss roars, then slams the ground: players still around it take damage and fly back, the closer the harder. */
object Shockwave : Ability {
  override val key = "shockwave"
  override val message = Message.SHOCKWAVE
  override fun enabled(settings: AbilitySettings) = settings.shockwaveEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.shockwave
  override fun cooldown(settings: AbilitySettings) = settings.shockwaveCooldown to settings.shockwaveCooldownBias

  /** Ticks between the wind-up and the slam: long enough to step back, if you watch the boss. */
  private const val WIND_UP = 15L

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    if (inReach(cast).isEmpty()) return false
    cast.particles(ParticleTypes.CLOUD, cast.at, 30, cfg.shockwaveRadius / 3, 0.1, cfg.shockwaveRadius / 3)
    cast.sound(SoundEvents.RAVAGER_ROAR, 1f, 0.8f)
    cast.later(WIND_UP) { slam(cast) }
    return true
  }

  private fun inReach(cast: Cast) = cast.victims(cast.at, cast.settings.shockwaveRadius).filter { it.position().distanceTo(cast.at) <= cast.settings.shockwaveRadius }

  private fun slam(cast: Cast) {
    val cfg = cast.settings
    val world = cast.world
    val damage = Rolls.linear(cfg.shockwaveDamageBase, cfg.shockwaveDamagePerLevel, cfg.shockwaveDamageMax, cast.level)
    cast.particles(ParticleTypes.EXPLOSION, cast.at, 3, cfg.shockwaveRadius / 3, 0.2, cfg.shockwaveRadius / 3)
    cast.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.7f)
    inReach(cast).forEach { victim ->
      val away = victim.position().subtract(cast.at).multiply(1.0, 0.0, 1.0)
      val closeness = 1.0 - (away.length() / cfg.shockwaveRadius).coerceIn(0.0, 1.0)
      victim.hurtServer(world, world.damageSources().mobAttack(cast.entity), (damage * (0.5 + 0.5 * closeness)).toFloat())
      val push = if (away.lengthSqr() < 1e-4) Vec3(1.0, 0.0, 0.0) else away.normalize()
      cast.push(victim, push.scale(cfg.shockwaveKnockback * (0.5 + 0.5 * closeness)).add(0.0, cfg.shockwaveLift, 0.0))
    }
  }
}
