package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.lang.Message

/**
 * A boss of `phase_two_min_level` or higher, once below `phase_two_health_share` of its health, turns:
 * a few seconds nothing hurts it, it throws everyone back, and from then on it casts faster. It
 * happens once, and the boss remembers it across restarts. Resistance V keeps it safe rather than
 * invulnerability, which the game would save on the entity if the server stopped in between.
 */
object PhaseTwo {
  val message = Message.PHASE_TWO

  fun due(cast: Cast): Boolean {
    val cfg = cast.settings.phaseTwo
    if (!cfg.enabled || cast.level < cfg.minLevel || cast.boss.phaseTwo) return false
    val max = cast.boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return false
    return cast.boss.entity.health <= max * cfg.healthShare
  }

  fun cast(cast: Cast) {
    val cfg = cast.settings.phaseTwo
    val entity = cast.boss.entity
    cast.boss.phaseTwo = true
    cast.plugin.worldApi.phaseTwo(cast.boss)
    entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, cfg.invulnerableSeconds.coerceAtLeast(0) * 20, 4, false, true, true))
    entity.world.spawnParticle(Particle.EXPLOSION_EMITTER, cast.at, 1)
    entity.world.spawnParticle(Particle.SOUL_FIRE_FLAME, cast.at.clone().add(0.0, 1.0, 0.0), 80, 1.2, 1.0, 1.2, 0.05)
    cast.sound(Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.3f)
    cast.victims(cast.at, 6.0).forEach { victim ->
      val away = victim.location.toVector().subtract(cast.at.toVector()).setY(0)
      victim.velocity = (if (away.lengthSquared() < 1e-4) away.setX(1.0) else away.normalize()).multiply(1.3).setY(0.5)
    }
  }

  /** Its cooldowns from now on. */
  fun factor(settings: AbilitySettings, phaseTwo: Boolean) = if (phaseTwo) settings.phaseTwo.cooldownFactor.coerceIn(0.1, 1.0) else 1.0
}
