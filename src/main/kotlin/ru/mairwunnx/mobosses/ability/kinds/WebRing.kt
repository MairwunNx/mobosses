package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Cobwebs in a ring around the nearest player, gone after `web_ring_duration` or when the server stops, and slowness for players inside. */
object WebRing : Ability {
  override val key = "web_ring"
  override val message = Message.WEB_RING
  override fun enabled(settings: AbilitySettings) = settings.webRingEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.webRing
  override fun cooldown(settings: AbilitySettings) = settings.webRingCooldown to settings.webRingCooldownBias

  private val REPLACEABLE = setOf(Material.AIR, Material.CAVE_AIR, Material.SHORT_GRASS)

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val target = cast.nearest(20.0) ?: return false
    val center = target.location
    val web = Material.COBWEB.createBlockData()
    var placed = 0
    for (degrees in 0 until 360 step 10) {
      val angle = Math.toRadians(degrees.toDouble())
      val ground = Targets.ground(center.world, center.x + cfg.webRingRadius * cos(angle), center.y, center.z + cfg.webRingRadius * sin(angle)) ?: continue
      val above = ground.getRelative(0, 1, 0)
      if (above.type !in REPLACEABLE || !cast.plugin.blockChanges.setFor(above, web, cfg.webRingDuration * 20L)) continue
      if (placed++ % 3 == 0) cast.sound(Sound.BLOCK_WOOL_PLACE, 0.3f, 0.8f, above.location)
    }
    if (placed > 0 && cfg.webRingPotionEnabled) {
      val seconds = min(cfg.webRingPotionBaseDuration + cast.level * cfg.webRingPotionLevelModifier, cfg.webRingPotionMaxDuration.toDouble())
      cast.players(center, cfg.webRingRadius + 1).forEach { it.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, (seconds * 20).toInt(), 1)) }
    }
    if (placed > 0) cast.sound(Sound.BLOCK_BAMBOO_PLACE, 0.8f, 1.2f)
    return placed > 0
  }
}
