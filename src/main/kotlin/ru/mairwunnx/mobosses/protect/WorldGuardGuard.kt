package ru.mairwunnx.mobosses.protect

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.domains.Association
import com.sk89q.worldguard.protection.association.Associables
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException
import org.bukkit.Location
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Settings

/**
 * Mo’Bosses' own WorldGuard flags. WorldGuard takes new flags only while plugins load, so they
 * register from `onLoad`, and only when WorldGuard is there: this object touches its classes.
 */
object WorldGuardFlags {
  /** `mobosses-spawn`: allow or deny natural boss spawns in a region. */
  var spawn: StateFlag? = null; private set
  /** `mobosses-abilities`: allow or deny abilities that change blocks in a region. */
  var abilities: StateFlag? = null; private set

  val registered get() = spawn != null && abilities != null

  fun register(log: Log) {
    try {
      val registry = WorldGuard.getInstance().flagRegistry
      fun flag(name: String): StateFlag = try {
        StateFlag(name, true).also(registry::register)
      } catch (_: FlagConflictException) {
        // Registered already, by an earlier load of this plugin.
        registry.get(name) as? StateFlag ?: error("another plugin registered $name as another kind of flag")
      }
      spawn = flag("mobosses-spawn")
      abilities = flag("mobosses-abilities")
    } catch (e: Throwable) {
      log.warn("Can't register the WorldGuard flags; regions aren't protected from bosses: $e")
    }
  }
}

/**
 * WorldGuard: natural boss spawns honor `mobosses-spawn` and `mob-spawning`; abilities that change
 * blocks honor `mobosses-abilities` and `build`; ability damage to players honors `mob-damage`.
 */
class WorldGuardGuard(private val settings: Settings.WorldGuard) : Guard {
  private val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
  private val spawn = WorldGuardFlags.spawn!!
  private val abilities = WorldGuardFlags.abilities!!

  override val name = "WorldGuard"

  override fun canSpawnBoss(location: Location) = !settings.spawnFlags || query.testState(BukkitAdapter.adapt(location), NOBODY, spawn, Flags.MOB_SPAWNING)

  override fun canChangeBlock(location: Location): Boolean {
    if (!settings.abilityFlags) return true
    val at = BukkitAdapter.adapt(location)
    // A boss is no member of any region, so `build` stops it wherever members alone may build.
    return query.testState(at, NOBODY, abilities) && query.queryState(at, NOBODY, Flags.BUILD) != StateFlag.State.DENY
  }

  override fun canHurt(player: Player) =
    !settings.damageFlags || query.testState(BukkitAdapter.adapt(player.location), WorldGuardPlugin.inst().wrapPlayer(player), Flags.MOB_DAMAGE)

  override fun regionsAt(location: Location): List<String> =
    query.getApplicableRegions(BukkitAdapter.adapt(location)).regions.map { "region ${it.id}" }

  private companion object {
    /** How WorldGuard sees pistons and other non-player actors: a member of no region. */
    val NOBODY = Associables.constant(Association.NON_MEMBER)
  }
}
