package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.roundToInt

/**
 * Once its health drops below `enrage_health_share`, the boss hits harder and moves faster until it
 * dies. It happens once: the boss remembers it, across restarts too.
 */
object Enrage : Ability {
  override val key = "enrage"
  override val message = Message.ENRAGE
  override fun enabled(settings: AbilitySettings) = settings.enrageEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.enrage
  override fun cooldown(settings: AbilitySettings) = 0L to 1.0

  override fun usable(cast: Cast): Boolean {
    val entity = cast.boss.entity
    val max = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return false
    return !cast.boss.enraged && entity.health <= max * cast.settings.enrageHealthShare
  }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val strength = (cfg.enrageStrengthBase + cfg.enrageStrengthPerLevel * cast.level).roundToInt().coerceIn(0, cfg.enrageStrengthMax.coerceAtLeast(0))
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, strength, false, true, true))
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, cfg.enrageSpeedAmplifier.coerceAtLeast(0), false, true, true))
    cast.boss.enraged = true
    cast.at.world.spawnParticle(Particle.ANGRY_VILLAGER, cast.at.clone().add(0.0, 1.5, 0.0), 12, 0.5, 0.5, 0.5, 0.0)
    cast.sound(Sound.ENTITY_RAVAGER_ROAR, 1f, 1.2f)
    return true
  }
}
