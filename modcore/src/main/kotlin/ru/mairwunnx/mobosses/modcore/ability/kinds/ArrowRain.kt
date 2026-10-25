package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Arrows
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import kotlin.random.Random

/** Waves of arrows falling on a square around the player the boss fights, however far; each wave follows them. */
object ArrowRain : Ability {
  private const val MIN_ROOM = 3.0

  override val key = "arrow_rain"
  override val message = Message.ARROW_RAIN
  override fun enabled(settings: AbilitySettings) = settings.arrowRainEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.arrowRain
  override fun cooldown(settings: AbilitySettings) = settings.arrowRainCooldown to settings.arrowRainCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val target = cast.foes().firstOrNull { Targets.headroom(cast.world, it.position(), cfg.rainHeight) >= MIN_ROOM } ?: return false
    val arrows = Rolls.linear(cfg.rainArrowsBase, cfg.rainArrowsPerLevel, cfg.rainArrowsMax, cast.level).coerceAtLeast(1)
    val damage = Rolls.linear(cfg.rainDamageBase, cfg.rainDamagePerLevel, cfg.rainDamageMax, cast.level)
    val effects = if (cast.level >= cfg.rainEffectLevelThreshold) cast.mod.catalog.rainEffects.filter { it != MobEffects.GLOWING } else emptyList()
    val tip = Arrows.Tip(effects, cfg.rainEffectDurationSeconds, cfg.rainEffectAmplifierMin, cfg.rainEffectAmplifierMax)
    val half = cfg.rainHalfSize.coerceAtLeast(0.5)
    val fall = cfg.rainFallSpeed.coerceAtLeast(0.1)
    cast.sound(SoundEvents.ARROW_SHOOT, 0.8f, 1.2f, target.position())

    repeat(cfg.rainWaves.coerceAtLeast(1)) { wave ->
      cast.later(cfg.rainWaveIntervalTicks.coerceAtLeast(1) * wave) {
        if (target.hasDisconnected() || !cast.canHit(target) || target.level() != cast.world) return@later
        val center = target.position()
        // Under a cave roof the arrows start below it, or the wave skips.
        val height = Targets.headroom(cast.world, center, cfg.rainHeight).takeIf { it >= MIN_ROOM } ?: return@later
        val tipped = Arrows.tipped(arrows, cfg.rainEffectsMaxPerWave, effects)
        for (i in 0 until arrows) {
          val at = Vec3(center.x + Random.nextDouble(-half, half), center.y + height, center.z + Random.nextDouble(-half, half))
          Arrows.shoot(cast, at, Vec3(Random.nextDouble(-0.05, 0.05), -fall, Random.nextDouble(-0.05, 0.05)), damage, true, cfg.rainLifetimeSeconds, tip.takeIf { i in tipped })
        }
        cast.sound(SoundEvents.ARROW_SHOOT, 0.6f, 1.4f, center)
        cast.particles(ParticleTypes.CLOUD, center, 12, half * 0.2, 0.3, half * 0.2, 0.01)
      }
    }
    return true
  }
}
