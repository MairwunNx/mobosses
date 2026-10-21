package ru.mairwunnx.mobosses.ability

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss

/**
 * One ability going off: the boss, the player whose hit, or whose pet's, set it off (none when a golem
 * did), and the helpers every ability uses.
 */
class Cast(val plugin: MoBossesPlugin, val boss: Boss, val attacker: Player?) {
  val settings get() = plugin.catalog.abilities
  val level get() = boss.level
  val at: Location get() = boss.entity.location

  fun players(center: Location, radius: Double) = Targets.players(center, radius, plugin.guards)

  /** Players and the pets and golems that hit the boss lately: whom abilities that hurt everyone near hit. */
  fun victims(center: Location, radius: Double): List<LivingEntity> = players(center, radius) + Targets.aggressors(center, radius, boss.uuid, plugin.keys.aggressor)

  /**
   * Whom the boss fights, however far in its world: the player it chases, the one who set this off, then
   * everyone who hit it in the last [FIGHTING] milliseconds, nearest first. Aimed abilities pick from these,
   * never a bystander, nor someone who hit it once and went home.
   */
  fun foes(): List<Player> {
    val here = at
    val hitters = boss.hitWithin(FIGHTING).mapNotNull(Bukkit::getPlayer).filter { it.world == here.world }.sortedBy { it.location.distanceSquared(here) }
    return (listOfNotNull((boss.entity as? Mob)?.target as? Player, attacker) + hitters).distinct().filter { it.world == here.world && canHit(it) }
  }

  /** [foes] within [radius] of the boss. */
  fun foes(radius: Double): List<Player> = foes().filter { it.location.distanceSquared(at) <= radius * radius }

  /** The foe an aimed ability goes for: the first of [foes] within [radius]. */
  fun foe(radius: Double = Double.POSITIVE_INFINITY): Player? = foes(radius).firstOrNull()

  fun canHit(player: Player) = Targets.canHit(player, plugin.guards)

  fun sound(sound: Sound, volume: Float = 1f, pitch: Float = 1f, where: Location = at) = where.world.playSound(where, sound, volume, pitch)

  /** Runs [block] after [ticks], unless the boss is gone by then. */
  fun later(ticks: Long, block: () -> Unit): BukkitTask =
    Bukkit.getScheduler().runTaskLater(plugin, Runnable { if (boss.entity.isValid) block() }, ticks.coerceAtLeast(0))

  /** Runs [block] every [period] ticks until it cancels its task. */
  fun every(period: Long, block: (BukkitTask) -> Unit) = Bukkit.getScheduler().runTaskTimer(plugin, block, 0L, period.coerceAtLeast(1))

  private companion object {
    /** How long a hit keeps a player in the fight. */
    const val FIGHTING = 30_000L
  }
}
