package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Scheduler
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.boss.Boss

/**
 * One ability going off, as on Paper: the boss, the player whose hit, or whose pet's, set it off (none when a golem
 * did), and the helpers every ability uses.
 */
class Cast(val mod: MoBosses, val boss: Boss, val attacker: ServerPlayer?) {
  val settings get() = mod.catalog.abilities
  val level get() = boss.level
  val entity: LivingEntity get() = boss.entity
  val world: ServerLevel get() = boss.entity.level() as ServerLevel
  val at: Vec3 get() = boss.entity.position()

  fun players(center: Vec3, radius: Double) = Targets.players(world, center, radius, mod.guards)

  /** Players and the pets and golems that hit the boss lately: whom abilities that hurt everyone near hit. */
  fun victims(center: Vec3, radius: Double): List<LivingEntity> = players(center, radius) + Targets.aggressors(world, center, radius, boss.uuid, mod.now())

  /**
   * Whom the boss fights, however far in its world: the player it chases, the one who set this off, then everyone who
   * hit it in the last [FIGHTING] milliseconds, nearest first. Aimed abilities pick from these, never a bystander.
   */
  fun foes(): List<ServerPlayer> {
    val here = at
    val hitters = boss.hitWithin(FIGHTING).mapNotNull(mod.server.playerList::getPlayer).filter { it.level() == world }.sortedBy { it.position().distanceToSqr(here) }
    return (listOfNotNull((entity as? Mob)?.target as? ServerPlayer, attacker) + hitters).distinct().filter { it.level() == world && canHit(it) }
  }

  /** [foes] within [radius] of the boss. */
  fun foes(radius: Double): List<ServerPlayer> = foes().filter { it.position().distanceToSqr(at) <= radius * radius }

  /** The foe an aimed ability goes for: the first of [foes] within [radius]. */
  fun foe(radius: Double = Double.POSITIVE_INFINITY): ServerPlayer? = foes(radius).firstOrNull()

  fun canHit(player: ServerPlayer) = Targets.canHit(player, mod.guards)

  fun sound(sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f, where: Vec3 = at) = World.sound(world, where, sound, volume, pitch)

  fun particles(particle: ParticleOptions, where: Vec3, count: Int, dx: Double = 0.0, dy: Double = 0.0, dz: Double = 0.0, speed: Double = 0.0) =
    World.particles(world, particle, where, count, dx, dy, dz, speed)

  /** Throws [entity] along [velocity], as Bukkit's `setVelocity`: a player's game hears of it at once. */
  fun push(entity: Entity, velocity: Vec3) = Targets.push(entity, velocity)

  /** Runs [block] after [ticks], unless the boss is gone by then. */
  fun later(ticks: Long, block: () -> Unit): Scheduler.Task = mod.scheduler.later(ticks) { if (entity.isAlive) block() }

  /** Runs [block] every [period] ticks until it cancels its task. */
  fun every(period: Long, block: (Scheduler.Task) -> Unit) = mod.scheduler.every(period.coerceAtLeast(1), 1, block)

  private companion object {
    /** How long a hit keeps a player in the fight. */
    const val FIGHTING = 30_000L
  }
}
