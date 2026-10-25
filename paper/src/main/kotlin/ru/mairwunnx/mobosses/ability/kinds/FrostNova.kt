package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.min

/**
 * Cold bursts out of the boss: players around it freeze as in powder snow, with the frosted screen
 * and the slow damage, and slow down. Leather armor keeps the freezing off, as in the game.
 */
object FrostNova : Ability {
  override val key = "frost_nova"
  override val message = Message.FROST_NOVA
  override fun enabled(settings: AbilitySettings) = settings.frostNovaEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.frostNova
  override fun cooldown(settings: AbilitySettings) = settings.frostNovaCooldown to settings.frostNovaCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val victims = cast.victims(cast.at, cfg.frostNovaRadius).filter { it.location.distance(cast.at) <= cfg.frostNovaRadius }
    if (victims.isEmpty()) return false
    val seconds = min(cfg.frostNovaSecondsBase + cfg.frostNovaSecondsPerLevel * cast.level, cfg.frostNovaSecondsMax)
    val ticks = (seconds * 20).toInt()
    cast.at.world.spawnParticle(Particle.SNOWFLAKE, cast.at.clone().add(0.0, 1.0, 0.0), 120, cfg.frostNovaRadius / 2, 0.6, cfg.frostNovaRadius / 2, 0.02)
    cast.sound(Sound.BLOCK_GLASS_BREAK, 1f, 0.6f)
    victims.forEach { victim ->
      // Frozen entities thaw by two ticks a tick; this keeps them fully frozen for the duration.
      victim.freezeTicks = victim.maxFreezeTicks + ticks * 2
      victim.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, ticks, cfg.frostNovaSlownessAmplifier.coerceAtLeast(0)))
    }
    return true
  }
}
