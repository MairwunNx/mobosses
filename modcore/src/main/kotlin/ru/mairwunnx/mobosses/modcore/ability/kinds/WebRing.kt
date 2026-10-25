package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Cobwebs in a ring around the player the boss fights, however far, gone after `web_ring_duration` or when the server stops, and slowness for players inside. */
object WebRing : Ability {
  override val key = "web_ring"
  override val message = Message.WEB_RING
  override fun enabled(settings: AbilitySettings) = settings.webRingEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.webRing
  override fun cooldown(settings: AbilitySettings) = settings.webRingCooldown to settings.webRingCooldownBias

  private val REPLACEABLE = setOf(Blocks.AIR, Blocks.CAVE_AIR, Blocks.SHORT_GRASS)

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    val world = cast.world
    val target = cast.foe() ?: return false
    val center = target.position()
    val web = Blocks.COBWEB.defaultBlockState()
    var placed = 0
    for (degrees in 0 until 360 step 10) {
      val angle = Math.toRadians(degrees.toDouble())
      val ground = Targets.ground(world, center.x + cfg.webRingRadius * cos(angle), center.y, center.z + cfg.webRingRadius * sin(angle)) ?: continue
      val above = ground.above()
      if (world.getBlockState(above).block !in REPLACEABLE || !cast.mod.blockChanges.setFor(world, above, web, cfg.webRingDuration * 20L)) continue
      if (placed++ % 3 == 0) cast.sound(SoundEvents.WOOL_PLACE, 0.3f, 0.8f, Vec3.atCenterOf(above))
    }
    if (placed > 0 && cfg.webRingPotionEnabled) {
      val seconds = min(cfg.webRingPotionBaseDuration + cast.level * cfg.webRingPotionLevelModifier, cfg.webRingPotionMaxDuration.toDouble())
      cast.players(center, cfg.webRingRadius + 1).forEach { it.addEffect(MobEffectInstance(MobEffects.SLOWNESS, (seconds * 20).toInt(), 1)) }
    }
    if (placed > 0) cast.sound(SoundEvents.BAMBOO_PLACE, 0.8f, 1.2f)
    return placed > 0
  }
}
