package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/**
 * Regeneration and resistance for a while, with arrows burning up and players slowed in the aura.
 * Tridents are left alone: removing one would take a player's weapon.
 */
object Regeneration : Ability {
  override val key = "regeneration"
  override val message = Message.REGENERATION
  override fun enabled(settings: AbilitySettings) = settings.regenerationEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.regeneration
  override fun cooldown(settings: AbilitySettings) = settings.regenerationCooldown to settings.regenerationCooldownBias

  private val NEGATIVE = listOf(
    PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER, PotionEffectType.SLOWNESS,
    PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.UNLUCK, PotionEffectType.DARKNESS,
  )

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val entity = cast.boss.entity
    val world = entity.world
    val baseSeconds = (cfg.regenDurationSecondsBase + cfg.regenDurationSecondsPerLevel * cast.level).toInt().coerceIn(1, cfg.regenDurationSecondsMax.coerceAtLeast(1))
    val ticks = (Rolls.biased(baseSeconds.toDouble(), cfg.regenerationDurationBias, Random).coerceAtMost(cfg.regenDurationSecondsMax.toDouble()) * 20).toInt().coerceAtLeast(1)
    val amplifier = (cfg.regenAmplifierBase + cfg.regenAmplifierPerLevel * cast.level).toInt().coerceIn(0, cfg.regenAmplifierMax.coerceAtLeast(0))

    cast.sound(Sound.ITEM_TOTEM_USE, 1f, 1.05f)
    cast.sound(Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.6f)
    world.spawnParticle(Particle.TOTEM_OF_UNDYING, cast.at, 40, 0.6, 1.0, 0.6, 0.1)
    if (cfg.regenCleanseNegatives) {
      NEGATIVE.forEach(entity::removePotionEffect)
      entity.fireTicks = 0
    }
    entity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, ticks, amplifier, true, true, true))
    if (cfg.regenExtraResistanceSeconds > 0) entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, cfg.regenExtraResistanceSeconds * 20, 0, true, true, true))
    entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, ticks, amplifier.coerceAtMost(2), true, true, true))

    var tick = 0
    cast.every(1) { task ->
      if (!entity.isValid || entity.isDead) return@every task.cancel()
      val base = entity.location
      if (tick % 2 == 0) world.spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(0.0, 1.0, 0.0), 6, 0.6, 0.6, 0.6, 0.0)
      if (tick % 8 == 0) world.spawnParticle(Particle.HEART, base.clone().add(0.0, 1.2, 0.0), 2, 0.2, 0.3, 0.2, 0.0)
      if (tick % 10 == 0) aura(cast, base)
      if (++tick >= ticks) {
        entity.removePotionEffect(PotionEffectType.REGENERATION)
        entity.removePotionEffect(PotionEffectType.RESISTANCE)
        cast.sound(Sound.BLOCK_BEACON_DEACTIVATE, 0.9f, 1f, entity.location)
        world.spawnParticle(Particle.END_ROD, entity.location, 24, 0.5, 1.0, 0.5, 0.02)
        task.cancel()
      }
    }
    return true
  }

  private fun aura(cast: Cast, at: Location) {
    val cfg = cast.settings
    val r = cfg.regenAuraRadius
    at.world.getNearbyEntities(at, r, r, r).forEach {
      if (cfg.regenAntiProjectiles && it is AbstractArrow && it !is Trident) {
        at.world.spawnParticle(Particle.SMOKE, it.location, 4, 0.1, 0.1, 0.1, 0.02)
        at.world.playSound(it.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.6f)
        it.remove()
      } else if (cfg.regenSlowPlayers && it is Player && cast.canHit(it)) {
        it.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false, true))
      }
    }
  }
}
