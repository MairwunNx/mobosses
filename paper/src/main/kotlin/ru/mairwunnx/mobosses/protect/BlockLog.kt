package ru.mairwunnx.mobosses.protect

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Settings

/** Where a block change by a boss gets written down, so an admin can look it up and undo it. */
interface BlockLog {
  fun removed(location: Location, data: BlockData) {}
  fun placed(location: Location, data: BlockData) {}

  companion object {
    /** CoreProtect when it runs with a known API and the config allows; otherwise nothing. */
    fun load(settings: Settings.CoreProtect, log: Log): BlockLog {
      if (!settings.enabled || !Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) return object : BlockLog {}
      return runCatching { CoreProtectLog.create(settings.user, log) }
        .onFailure { log.warn("CoreProtect is here, but its API doesn't match; block changes by bosses aren't logged: $it") }
        .getOrNull() ?: object : BlockLog {}
    }
  }
}
