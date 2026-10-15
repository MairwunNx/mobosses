package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Arrows
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/** Waves of arrows falling on a square around the nearest player; each wave follows them. */
object ArrowRain : Ability {
  private const val MIN_ROOM = 3.0

  override val key = "arrow_rain"
  override val message = Message.ARROW_RAIN
  override fun enabled(settings: AbilitySettings) = settings.arrowRainEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.arrowRain
  override fun cooldown(settings: AbilitySettings) = settings.arrowRainCooldown to settings.arrowRainCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val target = cast.nearest(cfg.rainTargetSearchRadius)?.takeIf { Targets.headroom(it.location, cfg.rainHeight) >= MIN_ROOM } ?: return false
    val arrows = Rolls.linear(cfg.rainArrowsBase, cfg.rainArrowsPerLevel, cfg.rainArrowsMax, cast.level).coerceAtLeast(1)
    val damage = Rolls.linear(cfg.rainDamageBase, cfg.rainDamagePerLevel, cfg.rainDamageMax, cast.level)
    val effects = if (cast.level >= cfg.rainEffectLevelThreshold) cast.plugin.catalog.rainEffects.filter { it != PotionEffectType.GLOWING } else emptyList()
    val tip = Arrows.Tip(effects, cfg.rainEffectDurationSeconds, cfg.rainEffectAmplifierMin, cfg.rainEffectAmplifierMax)
    val half = cfg.rainHalfSize.coerceAtLeast(0.5)
    val fall = cfg.rainFallSpeed.coerceAtLeast(0.1)
    cast.sound(Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.2f, target.location)

    repeat(cfg.rainWaves.coerceAtLeast(1)) { wave ->
      cast.later(cfg.rainWaveIntervalTicks.coerceAtLeast(1) * wave) {
        if (!target.isOnline || !cast.canHit(target)) return@later
        val center = target.location
        // Under a cave roof the arrows start below it, or the wave skips.
        val height = Targets.headroom(center, cfg.rainHeight).takeIf { it >= MIN_ROOM } ?: return@later
        val tipped = Arrows.tipped(arrows, cfg.rainEffectsMaxPerWave, effects)
        for (i in 0 until arrows) {
          val at = Location(center.world, center.x + Random.nextDouble(-half, half), center.y + height, center.z + Random.nextDouble(-half, half))
          Arrows.shoot(cast, at, Vector(Random.nextDouble(-0.05, 0.05), -fall, Random.nextDouble(-0.05, 0.05)), damage, true, cfg.rainLifetimeSeconds, tip.takeIf { i in tipped })
        }
        cast.sound(Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f, center)
        center.world.spawnParticle(Particle.CLOUD, center, 12, half * 0.2, 0.3, half * 0.2, 0.01)
      }
    }
    return true
  }
}
