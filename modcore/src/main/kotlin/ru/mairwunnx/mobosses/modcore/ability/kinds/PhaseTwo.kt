package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/**
 * A boss of `phase_two_min_level` or higher, once below `phase_two_health_share` of its health, turns: a few seconds
 * nothing hurts it, it throws everyone back, and from then on it casts faster. It happens once, and the boss
 * remembers it across restarts. Resistance V keeps it safe rather than invulnerability, which the game would save on
 * the entity if the server stopped in between.
 */
object PhaseTwo {
  val message = Message.PHASE_TWO

  fun due(cast: Cast): Boolean {
    val cfg = cast.settings.phaseTwo
    if (!cfg.enabled || cast.level < cfg.minLevel || cast.boss.phaseTwo) return false
    return cast.entity.health <= cast.entity.maxHealth * cfg.healthShare
  }

  fun cast(cast: Cast) {
    val cfg = cast.settings.phaseTwo
    cast.boss.phaseTwo = true
    cast.mod.worldApi.phaseTwo(cast.boss)
    cast.entity.addEffect(MobEffectInstance(MobEffects.RESISTANCE, cfg.invulnerableSeconds.coerceAtLeast(0) * 20, 4, false, true, true))
    cast.particles(ParticleTypes.EXPLOSION_EMITTER, cast.at, 1)
    cast.particles(ParticleTypes.SOUL_FIRE_FLAME, cast.at.add(0.0, 1.0, 0.0), 80, 1.2, 1.0, 1.2, 0.05)
    cast.sound(SoundEvents.WITHER_SPAWN, 0.8f, 1.3f)
    cast.victims(cast.at, 6.0).forEach { victim ->
      val away = victim.position().subtract(cast.at).multiply(1.0, 0.0, 1.0)
      cast.push(victim, (if (away.lengthSqr() < 1e-4) Vec3(1.0, 0.0, 0.0) else away.normalize()).scale(1.3).add(0.0, 0.5, 0.0))
    }
  }

  /** Its cooldowns from now on. */
  fun factor(settings: AbilitySettings, phaseTwo: Boolean) = if (phaseTwo) settings.phaseTwo.cooldownFactor.coerceIn(0.1, 1.0) else 1.0
}
