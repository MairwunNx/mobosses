package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import kotlin.random.Random

/**
 * Regeneration and resistance for a while, with arrows burning up and players slowed in the aura. Tridents are left
 * alone: removing one would take a player's weapon.
 */
object Regeneration : Ability {
  override val key = "regeneration"
  override val message = Message.REGENERATION
  override fun enabled(settings: AbilitySettings) = settings.regenerationEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.regeneration
  override fun cooldown(settings: AbilitySettings) = settings.regenerationCooldown to settings.regenerationCooldownBias

  private val NEGATIVE = listOf(
    MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER, MobEffects.SLOWNESS,
    MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS, MobEffects.BLINDNESS, MobEffects.UNLUCK, MobEffects.DARKNESS,
  )

  /** Only a hurt boss heals: at full health it would waste the cast. */
  override fun usable(cast: Cast) = cast.entity.health < cast.entity.maxHealth * 0.8

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val entity = cast.entity
    val baseSeconds = (cfg.regenDurationSecondsBase + cfg.regenDurationSecondsPerLevel * cast.level).toInt().coerceIn(1, cfg.regenDurationSecondsMax.coerceAtLeast(1))
    val ticks = (Rolls.biased(baseSeconds.toDouble(), cfg.regenerationDurationBias, Random).coerceAtMost(cfg.regenDurationSecondsMax.toDouble()) * 20).toInt().coerceAtLeast(1)
    val amplifier = (cfg.regenAmplifierBase + cfg.regenAmplifierPerLevel * cast.level).toInt().coerceIn(0, cfg.regenAmplifierMax.coerceAtLeast(0))

    cast.sound(SoundEvents.TOTEM_USE, 1f, 1.05f)
    cast.sound(SoundEvents.BEACON_AMBIENT, 0.6f, 1.6f)
    cast.particles(ParticleTypes.TOTEM_OF_UNDYING, cast.at, 40, 0.6, 1.0, 0.6, 0.1)
    if (cfg.regenCleanseNegatives) {
      NEGATIVE.forEach(entity::removeEffect)
      entity.clearFire()
    }
    entity.addEffect(MobEffectInstance(MobEffects.REGENERATION, ticks, amplifier, true, true, true))
    if (cfg.regenExtraResistanceSeconds > 0) entity.addEffect(MobEffectInstance(MobEffects.RESISTANCE, cfg.regenExtraResistanceSeconds * 20, 0, true, true, true))
    entity.addEffect(MobEffectInstance(MobEffects.RESISTANCE, ticks, amplifier.coerceAtMost(2), true, true, true))

    var tick = 0
    cast.every(1) { task ->
      if (!entity.isAlive) return@every task.cancel()
      val base = entity.position()
      if (tick % 2 == 0) cast.particles(ParticleTypes.HAPPY_VILLAGER, base.add(0.0, 1.0, 0.0), 6, 0.6, 0.6, 0.6)
      if (tick % 8 == 0) cast.particles(ParticleTypes.HEART, base.add(0.0, 1.2, 0.0), 2, 0.2, 0.3, 0.2)
      if (tick % 10 == 0) aura(cast, base)
      // Its effects run out by themselves: removing resistance here would end phase two's too.
      if (++tick >= ticks) {
        cast.sound(SoundEvents.BEACON_DEACTIVATE, 0.9f, 1f, entity.position())
        cast.particles(ParticleTypes.END_ROD, entity.position(), 24, 0.5, 1.0, 0.5, 0.02)
        task.cancel()
      }
    }
    return true
  }

  private fun aura(cast: Cast, at: Vec3) {
    val cfg = cast.settings
    val r = cfg.regenAuraRadius
    cast.world.getEntitiesOfClass(Entity::class.java, AABB.ofSize(at, r * 2, r * 2, r * 2)).forEach {
      if (cfg.regenAntiProjectiles && it is AbstractArrow && it !is ThrownTrident) {
        cast.particles(ParticleTypes.SMOKE, it.position(), 4, 0.1, 0.1, 0.1, 0.02)
        cast.sound(SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.6f, it.position())
        it.discard()
      } else if (cfg.regenSlowPlayers && it is ServerPlayer && cast.canHit(it)) {
        it.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 20, 0, false, false, true))
      }
    }
  }
}
