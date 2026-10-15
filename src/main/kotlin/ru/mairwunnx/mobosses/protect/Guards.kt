package ru.mairwunnx.mobosses.protect

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Settings

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
