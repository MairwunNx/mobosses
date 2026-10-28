package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast

/**
 * The boss gathers darkness, as on Paper: ink and a low warning, then a second later blindness on every player in the
 * fight still within its radius. A player who runs `escape` blocks past the radius sheds it early, and everyone does
 * once the boss is gone; blindness from anything else stays.
 */
object Blindness : Ability {
  override val key = "blindness"
  override val message = Message.BLINDNESS
  override fun enabled(settings: AbilitySettings) = settings.blindness.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.blindness
  override fun cooldown(settings: AbilitySettings) = settings.blindness.cooldown to settings.blindness.cooldownBias

  override fun usable(cast: Cast) = inReach(cast, radius(cast)).isNotEmpty()

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.blindness
    val radius = radius(cast)
    if (inReach(cast, radius).isEmpty()) return false
    val ticks = (Rolls.linear(cfg.secondsBase, cfg.secondsPerLevel, cfg.secondsMax, cast.level) * 20).toLong()
    World.particles(cast.world, ParticleTypes.SQUID_INK, cast.at.add(0.0, cast.entity.bbHeight / 2.0, 0.0), 60, 1.2, 1.0, 1.2, 0.05)
    cast.sound(SoundEvents.WARDEN_SONIC_CHARGE, 1.2f, 0.6f)
    cast.later(cfg.windUpTicks.coerceAtLeast(0).toLong()) {
      val blinded = inReach(cast, radius).toMutableList()
      if (blinded.isEmpty()) return@later
      blinded.forEach { it.addEffect(MobEffectInstance(MobEffects.BLINDNESS, ticks.toInt(), 0, false, false, true)) }
      cast.sound(SoundEvents.ELDER_GUARDIAN_CURSE, 1f, 0.7f)
      var tick = 0L
      cast.every(10) { task ->
        tick += 10
        val boss = cast.entity
        blinded.removeAll { player ->
          val away = !boss.isAlive || player.level() != boss.level() || player.distanceTo(boss) > radius + cfg.escape
          if (away && !player.hasDisconnected()) shed(player, ticks - tick)
          player.hasDisconnected() || away || tick >= ticks
        }
        if (blinded.isEmpty()) task.cancel()
      }
    }
    return true
  }

  private fun radius(cast: Cast): Double {
    val cfg = cast.settings.blindness
    return Rolls.linear(cfg.radiusBase, cfg.radiusPerLevel, cfg.radiusMax, cast.level)
  }

  private fun inReach(cast: Cast, radius: Double) = cast.foes(radius).filter { !it.hasEffect(MobEffects.BLINDNESS) && cast.canHit(it) }

  /** Takes off the blindness this gave, which has at most [left] ticks to go; a longer one came from elsewhere and stays. */
  private fun shed(player: ServerPlayer, left: Long) {
    val effect = player.getEffect(MobEffects.BLINDNESS) ?: return
    if (effect.duration <= left + 20) player.removeEffect(MobEffects.BLINDNESS)
  }
}
