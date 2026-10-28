package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/**
 * The boss gathers darkness: ink and a low warning, then a second later blindness on every player in the fight still
 * within its radius, so whoever runs at once gets away. A player who runs `escape` blocks past the radius sheds it
 * early, and everyone does once the boss is gone; blindness from anything else stays.
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
    val at = cast.at.clone().add(0.0, cast.boss.entity.height / 2, 0.0)
    at.world.spawnParticle(Particle.SQUID_INK, at, 60, 1.2, 1.0, 1.2, 0.05)
    cast.sound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.2f, 0.6f)
    cast.later(cfg.windUpTicks.coerceAtLeast(0).toLong()) {
      val blinded = inReach(cast, radius).toMutableList()
      if (blinded.isEmpty()) return@later
      blinded.forEach { it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, ticks.toInt(), 0, false, false, true)) }
      cast.sound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.7f)
      var tick = 0L
      cast.every(10) { task ->
        tick += 10
        val boss = cast.boss.entity
        blinded.removeAll { player ->
          val away = !boss.isValid || boss.isDead || player.world != boss.world || player.location.distance(boss.location) > radius + cfg.escape
          if (away && player.isOnline) shed(player, ticks - tick)
          !player.isOnline || away || tick >= ticks
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

  private fun inReach(cast: Cast, radius: Double) = cast.foes(radius).filter { !it.hasPotionEffect(PotionEffectType.BLINDNESS) && cast.canHit(it) }

  /** Takes off the blindness this gave, which has at most [left] ticks to go; a longer one came from elsewhere and stays. */
  private fun shed(player: Player, left: Long) {
    val effect = player.getPotionEffect(PotionEffectType.BLINDNESS) ?: return
    if (effect.duration <= left + 20) player.removePotionEffect(PotionEffectType.BLINDNESS)
  }
}
