package ru.mairwunnx.mobosses.modcore.ability.kinds

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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Rings of arrows out of the boss in every direction; from `arrow_arc_random_effect_level_threshold`, some carry a negative effect. */
object ArrowArc : Ability {
  override val key = "arrow_arc"
  override val message = Message.ARROW_ARC
  override fun enabled(settings: AbilitySettings) = settings.arrowArcEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.arrowArc
  override fun cooldown(settings: AbilitySettings) = settings.arrowArcCooldown to settings.arrowArcCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val arrows = Rolls.linear(cfg.arcArrowsBase, cfg.arcArrowsPerLevel, cfg.arcArrowsMax, cast.level).coerceAtLeast(1)
    val damage = Rolls.linear(cfg.arcDamageBase, cfg.arcDamagePerLevel, cfg.arcDamageMax, cast.level)
    val rings = cfg.arcRings.coerceAtLeast(1)
    val effects = if (cast.level >= cfg.arcEffectLevelThreshold) cast.mod.catalog.arcEffects.filter { it != MobEffects.GLOWING } else emptyList()
    val tip = Arrows.Tip(effects, cfg.arcEffectDurationSeconds, cfg.arcEffectAmplifierMin, cfg.arcEffectAmplifierMax)
    val box = cast.entity.boundingBox
    val height = (box.maxY - box.minY).coerceAtLeast(0.5)
    val step = 2 * PI / arrows

    for (ring in 0 until rings) {
      val y = box.minY + height * (ring + 0.5) / rings
      val phase = if (cfg.arcStaggered && ring % 2 == 1) step / 2 else 0.0
      val tipped = Arrows.tipped(arrows, cfg.arcEffectsMaxPerRing, effects)
      for (i in 0 until arrows) {
        val direction = Vec3(cos(i * step + phase), 0.0, sin(i * step + phase))
        val at = Vec3(cast.at.x, y, cast.at.z).add(direction.scale(0.3))
        Arrows.shoot(cast, at, direction.scale(cfg.arcSpeed), damage, !cfg.arcNoGravity, cfg.arcLifetimeSeconds, tip.takeIf { i in tipped })
      }
    }
    cast.sound(SoundEvents.CROSSBOW_SHOOT)
    return true
  }
}
