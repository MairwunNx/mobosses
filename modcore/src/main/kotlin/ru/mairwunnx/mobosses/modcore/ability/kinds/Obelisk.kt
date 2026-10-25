package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The boss raises an end crystal a few blocks away that heals it through a beam, as the dragon's do: a share of its
 * max health each second, by level, while it stays in range. Players break it with a few hits; nothing else harms it,
 * and it never explodes (`CrystalMixin`). It isn't saved, so a restart takes it.
 */
object Obelisk : Ability {
  override val key = "obelisk"
  override val message = Message.OBELISK
  override fun enabled(settings: AbilitySettings) = settings.obelisk.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.obelisk
  override fun cooldown(settings: AbilitySettings) = settings.obelisk.cooldown to settings.obelisk.cooldownBias

  /** The standing obelisk of each boss. */
  private val standing = HashMap<UUID, EndCrystal>()

  /** A hurt boss without one standing. */
  override fun usable(cast: Cast) = cast.entity.health < cast.entity.maxHealth * 0.9 && standing[cast.boss.uuid]?.isAlive != true

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.obelisk
    val spot = spot(cast.world, cast.at) ?: return false
    val hits = Rolls.linear(cfg.hitsBase, cfg.hitsPerLevel, cfg.hitsMax, cast.level).coerceAtLeast(1)
    val crystal = EndCrystal(cast.world, spot.x, spot.y, spot.z)
    crystal.setShowBottom(true)
    crystal.setBeamTarget(beamAt(cast.boss))
    Data(crystal).apply {
      set(Keys.OBELISK, cast.boss.uuid.toString())
      set(Keys.OBELISK_HITS, hits)
    }
    cast.world.addFreshEntity(crystal)
    standing[cast.boss.uuid] = crystal
    cast.particles(ParticleTypes.REVERSE_PORTAL, spot.add(0.0, 1.0, 0.0), 60, 0.4, 1.0, 0.4, 0.05)
    cast.sound(SoundEvents.END_PORTAL_FRAME_FILL, 1f, 0.6f, spot)
    cast.sound(SoundEvents.BEACON_ACTIVATE, 1f, 0.8f, spot)

    val share = Rolls.linear(cfg.healBase, cfg.healPerLevel, cfg.healMax, cast.level)
    var seconds = 0
    cast.every(20) { task ->
      val boss = cast.entity
      if (!crystal.isAlive || !boss.isAlive || ++seconds > cfg.seconds) {
        task.cancel()
        if (crystal.isAlive) fade(crystal)
        standing.remove(cast.boss.uuid, crystal)
        return@every
      }
      if (boss.level() != crystal.level() || boss.distanceToSqr(crystal) > cfg.range * cfg.range) {
        crystal.setBeamTarget(null)
        return@every
      }
      val max = boss.maxHealth
      crystal.setBeamTarget(beamAt(cast.boss))
      if (boss.health < max) {
        boss.heal((max * share).toFloat())
        cast.mod.bars.update(cast.boss)
        cast.particles(ParticleTypes.HEART, boss.position().add(0.0, boss.bbHeight + 0.3, 0.0), 2, 0.3, 0.2, 0.3)
        beam(cast.world, crystal.position().add(0.0, 1.0, 0.0), boss.position().add(0.0, boss.bbHeight / 2.0, 0.0))
      }
    }
    return true
  }

  /** A player's hit on an obelisk: one fewer left, and it breaks at none. */
  fun hit(mod: MoBosses, crystal: EndCrystal, player: ServerPlayer) {
    val level = crystal.level() as? ServerLevel ?: return
    val data = Data(crystal)
    val at = crystal.position()
    val left = (data.int(Keys.OBELISK_HITS) ?: 1) - 1
    World.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 1f, 0.6f + 0.1f * left.coerceAtLeast(0))
    World.particles(level, ParticleTypes.CRIT, at.add(0.0, 1.0, 0.0), 12, 0.3, 0.5, 0.3, 0.2)
    if (left > 0) return data.set(Keys.OBELISK_HITS, left)

    val boss = data.string(Keys.OBELISK)?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let(mod.bosses::get)
    World.particles(level, ParticleTypes.EXPLOSION, at.add(0.0, 1.0, 0.0), 2, 0.0, 0.0, 0.0, 0.0)
    World.sound(level, at, SoundEvents.GLASS_BREAK, 1.2f, 0.7f)
    World.sound(level, at, SoundEvents.ENDER_DRAGON_HURT, 0.8f, 1.4f)
    crystal.discard()
    mod.achievements.grant(player, Achievement.OBELISK_BREAKER)
    if (boss != null) {
      standing.remove(boss.uuid, crystal)
      val near = (boss.damage.keys.mapNotNull(mod.server.playerList::getPlayer) + player).distinct().filter { it.level() == level }
      mod.texts.sendAll(near, Message.OBELISK_BROKEN) { code -> arrayOf(Placeholder.unparsed("player", player.plainTextName), *mod.texts.boss(code, boss)) }
    }
  }

  /** Every obelisk, for shutdown. */
  fun removeAll() {
    standing.values.filter { it.isAlive }.forEach { it.discard() }
    standing.clear()
  }

  /** Solid ground four to six blocks from the boss, where a crystal fits. */
  private fun spot(level: ServerLevel, at: Vec3): Vec3? {
    repeat(8) {
      val angle = Random.nextDouble(0.0, Math.PI * 2)
      val distance = Random.nextDouble(4.0, 6.0)
      val x = at.x + cos(angle) * distance
      val z = at.z + sin(angle) * distance
      val ground = Targets.ground(level, x, at.y, z, 4) ?: return@repeat
      if (Targets.isOpen(level, ground.above(2))) return Vec3(ground.x + 0.5, ground.y + 1.0, ground.z + 0.5)
    }
    return null
  }

  private fun beamAt(boss: Boss) = BlockPos.containing(boss.entity.position().add(0.0, boss.entity.bbHeight / 2.0, 0.0))

  /** Sparks along the beam, so the healing shows even where the beam is hard to see. */
  private fun beam(level: ServerLevel, from: Vec3, to: Vec3) {
    val step = to.subtract(from)
    val points = (step.length() * 2).toInt().coerceIn(2, 32)
    val each = step.scale(1.0 / points)
    var at = from
    repeat(points) {
      at = at.add(each)
      World.particles(level, ParticleTypes.HAPPY_VILLAGER, at, 1, 0.0, 0.0, 0.0, 0.0)
    }
  }

  private fun fade(crystal: EndCrystal) {
    val level = crystal.level() as? ServerLevel ?: return crystal.discard()
    World.particles(level, ParticleTypes.REVERSE_PORTAL, crystal.position().add(0.0, 1.0, 0.0), 40, 0.4, 0.8, 0.4, 0.05)
    World.sound(level, crystal.position(), SoundEvents.BEACON_DEACTIVATE, 1f, 0.8f)
    crystal.discard()
  }
}
