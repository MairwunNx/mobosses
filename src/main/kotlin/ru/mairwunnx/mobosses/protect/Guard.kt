package ru.mairwunnx.mobosses.protect

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Settings

/**
 * What a protection plugin lets bosses do. One adapter per plugin, each loaded only when that
 * plugin runs; Lands, Towny, or Residence would join [Guards.load] as another adapter. With none,
 * everything is allowed.
 */
interface Guard {
  val name: String

  /** Whether a boss may spawn here naturally. `/mobosses spawn` asks only when `protection.admin_spawn_bypass` is off. */
  fun canSpawnBoss(location: Location): Boolean = true

  /** Whether an ability may break, place, or ignite the block here. */
  fun canChangeBlock(location: Location): Boolean = true

  /** Whether an ability may hurt this player here. */
  fun canHurt(player: Player): Boolean = true

  /** The protected areas here, for the debug log. */
  fun regionsAt(location: Location): List<String> = emptyList()
}

/** Every adapter at once: an action goes ahead only when all of them allow it. */
class Guards(val adapters: List<Guard>) : Guard {
  override val name = adapters.joinToString { it.name }.ifEmpty { "none" }

  override fun canSpawnBoss(location: Location) = adapters.all { it.canSpawnBoss(location) }
  override fun canChangeBlock(location: Location) = adapters.all { it.canChangeBlock(location) }
  override fun canHurt(player: Player) = adapters.all { it.canHurt(player) }
  override fun regionsAt(location: Location) = adapters.flatMap { it.regionsAt(location) }

  companion object {
    /** Adapters for the protection plugins that run and that the config switches on. */
    fun load(settings: Settings.Protection, log: Log): Guards {
      val plugins = Bukkit.getPluginManager()
      val adapters = buildList {
        if (settings.griefprevention.enabled && plugins.isPluginEnabled("GriefPrevention")) {
          runCatching { GriefPreventionGuard(settings.griefprevention) }
            .onSuccess(::add)
            .onFailure { log.warn("GriefPrevention is here, but its API doesn't match; claims aren't protected from bosses: $it") }
        }
        if (settings.worldguard.enabled && plugins.isPluginEnabled("WorldGuard") && WorldGuardFlags.registered) {
          runCatching { WorldGuardGuard(settings.worldguard) }
            .onSuccess(::add)
            .onFailure { log.warn("WorldGuard is here, but its API doesn't match; regions aren't protected from bosses: $it") }
        }
      }
      return Guards(adapters)
    }
  }
}
