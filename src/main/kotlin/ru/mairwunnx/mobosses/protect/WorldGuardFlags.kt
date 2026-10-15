package ru.mairwunnx.mobosses.protect

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException
import ru.mairwunnx.mobosses.Log

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
