package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Arrows
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
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
    val effects = if (cast.level >= cfg.arcEffectLevelThreshold) cast.plugin.catalog.arcEffects.filter { it != PotionEffectType.GLOWING } else emptyList()
    val tip = Arrows.Tip(effects, cfg.arcEffectDurationSeconds, cfg.arcEffectAmplifierMin, cfg.arcEffectAmplifierMax)
    val box = cast.boss.entity.boundingBox
    val height = (box.maxY - box.minY).coerceAtLeast(0.5)
    val step = 2 * PI / arrows
    // A boss high over the fight, a flyer or a wither, looses its rings down at the ground around it, not over everyone's heads.
    val above = cast.foes().minByOrNull { it.location.distanceSquared(cast.at) }?.let { cast.at.y - it.location.y } ?: 0.0
    val dip = if (above > DIP_FROM) -(above / 10.0).coerceAtMost(1.5) else 0.0

    for (ring in 0 until rings) {
      val y = box.minY + height * (ring + 0.5) / rings
      val phase = if (cfg.arcStaggered && ring % 2 == 1) step / 2 else 0.0
      val tipped = Arrows.tipped(arrows, cfg.arcEffectsMaxPerRing, effects)
      for (i in 0 until arrows) {
        val direction = Vector(cos(i * step + phase), dip, sin(i * step + phase)).normalize()
        val at = Location(cast.at.world, cast.at.x, y, cast.at.z).add(direction.clone().multiply(0.3))
        Arrows.shoot(cast, at, direction.multiply(cfg.arcSpeed), damage, !cfg.arcNoGravity, cfg.arcLifetimeSeconds, tip.takeIf { i in tipped })
      }
    }
    cast.sound(Sound.ITEM_CROSSBOW_SHOOT)
    return true
  }

  /** How many blocks above the nearest player it fights a boss starts aiming its rings down. */
  private const val DIP_FROM = 3.0
}
