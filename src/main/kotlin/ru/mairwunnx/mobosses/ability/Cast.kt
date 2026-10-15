package ru.mairwunnx.mobosses.ability

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
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

  fun nearest(radius: Double) = Targets.nearest(at, radius, plugin.guards)

  fun canHit(player: Player) = Targets.canHit(player, plugin.guards)

  fun sound(sound: Sound, volume: Float = 1f, pitch: Float = 1f, where: Location = at) = where.world.playSound(where, sound, volume, pitch)

  /** Runs [block] after [ticks], unless the boss is gone by then. */
  fun later(ticks: Long, block: () -> Unit): BukkitTask =
    Bukkit.getScheduler().runTaskLater(plugin, Runnable { if (boss.entity.isValid) block() }, ticks.coerceAtLeast(0))

  /** Runs [block] every [period] ticks until it cancels its task. */
  fun every(period: Long, block: (BukkitTask) -> Unit) = Bukkit.getScheduler().runTaskTimer(plugin, block, 0L, period.coerceAtLeast(1))
}
