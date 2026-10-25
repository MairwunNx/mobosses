package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.modcore.mixin.FallingBlockAccess
import kotlin.math.max
import kotlin.random.Random

/**
 * Waves of burning meteors over the player the boss fights, however far. They break up on impact: the game would
 * turn a falling block into a block, so each one's drop is off, which on Paper `MeteorListener` saw to; and they aren't
 * saved, so a restart doesn't drop one later.
 */
object MeteorShower : Ability {
  private const val MIN_ROOM = 4.0

  override val key = "meteor_shower"
  override val message = Message.METEOR_SHOWER
  override fun enabled(settings: AbilitySettings) = settings.meteorShowerEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.meteorShower
  override fun cooldown(settings: AbilitySettings) = settings.meteorShowerCooldown to settings.meteorShowerCooldownBias

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings
    var target = cast.foes().firstOrNull { Targets.headroom(cast.world, it.position(), cfg.meteorHeight) >= MIN_ROOM } ?: return false
    val waves = Rolls.linear(cfg.meteorWavesBase, cfg.meteorWavesPerLevel, cfg.meteorWavesMax, cast.level)
    val perWave = Rolls.linear(cfg.meteorsPerWaveBase, cfg.meteorsPerWavePerLevel, cfg.meteorsPerWaveMax, cast.level)
    val damage = Rolls.linear(cfg.meteorDamageBase, cfg.meteorDamagePerLevel, cfg.meteorDamageMax, cast.level)
    cast.sound(SoundEvents.WITHER_SPAWN, 0.7f, 1.3f)

    repeat(waves) { wave ->
      cast.later(cfg.meteorWaveIntervalTicks * wave) {
        // The first target while it's still in the fight; then the next foe.
        if (!cast.canHit(target) || target.level() != cast.world) target = cast.foe() ?: return@later
        repeat(perWave) { meteor(cast, target, damage) }
      }
    }
    return true
  }

  private fun meteor(cast: Cast, target: ServerPlayer, damage: Double) {
    val cfg = cast.settings
    val at = target.position()
    // Under a cave roof the meteor starts below it.
    val height = Targets.headroom(cast.world, at, cfg.meteorHeight).takeIf { it >= MIN_ROOM } ?: return
    val meteor = FallingBlockEntity(EntityTypes.FALLING_BLOCK, cast.world)
    (meteor as FallingBlockAccess).`mobosses$block`(Blocks.MAGMA_BLOCK.defaultBlockState())
    meteor.setPos(at.x + Random.nextDouble(-0.8, 0.8), at.y + height, at.z + Random.nextDouble(-0.8, 0.8))
    meteor.dropItem = false
    meteor.disableDrop()
    Data(meteor)[Keys.METEOR] = true
    meteor.deltaMovement = Vec3(0.0, -cfg.meteorFallSpeed, 0.0)
    cast.world.addFreshEntity(meteor)
    val lifetime = cfg.meteorLifetimeSeconds.coerceAtLeast(1) * 20L
    var age = 0L
    cast.every(1) { task ->
      if (meteor.isAlive && !meteor.onGround() && ++age < lifetime) return@every
      task.cancel()
      // One that landed is gone already, where it broke up.
      val hit = meteor.position()
      meteor.discard()
      if (age >= lifetime) return@every
      cast.particles(ParticleTypes.EXPLOSION, hit, 2)
      cast.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.55f, 1.1f, hit)
      cast.victims(hit, cfg.meteorFireRadius).forEach {
        it.hurtServer(cast.world, cast.world.damageSources().mobAttack(cast.entity), damage.toFloat())
        it.remainingFireTicks = max(it.remainingFireTicks, cfg.meteorIgniteSeconds * 20)
      }
    }
  }
}
