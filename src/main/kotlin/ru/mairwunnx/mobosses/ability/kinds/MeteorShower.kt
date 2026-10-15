package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.max
import kotlin.random.Random

/**
 * Waves of burning meteors over the nearest player in range. They break up on impact: the game would
 * turn a falling block into a block, so [MeteorListener] stops that, and they aren't saved, so a
 * restart doesn't drop one later.
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
    val radius = Rolls.linear(cfg.meteorRadiusBase, cfg.meteorRadiusPerLevel, cfg.meteorRadiusMax, cast.level)
    var target = cast.nearest(radius)?.takeIf { Targets.headroom(it.location, cast.settings.meteorHeight) >= MIN_ROOM } ?: return false
    val waves = Rolls.linear(cfg.meteorWavesBase, cfg.meteorWavesPerLevel, cfg.meteorWavesMax, cast.level)
    val perWave = Rolls.linear(cfg.meteorsPerWaveBase, cfg.meteorsPerWavePerLevel, cfg.meteorsPerWaveMax, cast.level)
    val damage = Rolls.linear(cfg.meteorDamageBase, cfg.meteorDamagePerLevel, cfg.meteorDamageMax, cast.level)
    cast.sound(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.3f)

    repeat(waves) { wave ->
      cast.later(cfg.meteorWaveIntervalTicks * wave) {
        // The first target while it's still in the fight; then whoever is nearest.
        if (!cast.canHit(target) || target.world != cast.at.world) target = cast.nearest(radius) ?: return@later
        repeat(perWave) { meteor(cast, target, damage) }
      }
    }
    return true
  }

  private fun meteor(cast: Cast, target: Player, damage: Double) {
    val cfg = cast.settings
    val at = target.location
    // Under a cave roof the meteor starts below it.
    val height = Targets.headroom(at, cfg.meteorHeight).takeIf { it >= MIN_ROOM } ?: return
    val spawn = Location(at.world, at.x + Random.nextDouble(-0.8, 0.8), at.y + height, at.z + Random.nextDouble(-0.8, 0.8))
    val meteor = at.world.spawn(spawn, FallingBlock::class.java) { block ->
      block.blockData = Material.MAGMA_BLOCK.createBlockData()
      block.dropItem = false
      block.isPersistent = false
      block.setHurtEntities(false)
      block.persistentDataContainer.set(cast.plugin.keys.meteor, PersistentDataType.BOOLEAN, true)
      block.velocity = Vector(0.0, -cfg.meteorFallSpeed, 0.0)
    }
    val lifetime = cfg.meteorLifetimeSeconds.coerceAtLeast(1) * 20L
    var age = 0L
    cast.every(1) { task ->
      if (meteor.isValid && !meteor.isOnGround && ++age < lifetime) return@every
      task.cancel()
      val hit = meteor.location
      meteor.remove()
      if (age >= lifetime) return@every
      hit.world.spawnParticle(Particle.EXPLOSION, hit, 2)
      hit.world.playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.1f)
      cast.players(hit, cfg.meteorFireRadius).forEach {
        it.damage(damage, cast.boss.entity)
        it.fireTicks = max(it.fireTicks, cfg.meteorIgniteSeconds * 20)
      }
    }
  }
}
