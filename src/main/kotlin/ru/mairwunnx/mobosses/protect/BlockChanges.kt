package ru.mairwunnx.mobosses.protect

import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.MoBossesPlugin
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

/** CoreProtect, under a user name like `#mobosses`. Touches CoreProtect's classes, so it loads only when CoreProtect runs. */
class CoreProtectLog private constructor(private val api: CoreProtectAPI, private val user: String) : BlockLog {
  override fun removed(location: Location, data: BlockData) {
    api.logRemoval(user, location, data.material, data)
  }

  override fun placed(location: Location, data: BlockData) {
    api.logPlacement(user, location, data.material, data)
  }

  companion object {
    /** API 10 is CoreProtect 22; 24.1 is API 12. */
    private const val MIN_API = 10

    fun create(user: String, log: Log): BlockLog? {
      val api = (Bukkit.getPluginManager().getPlugin("CoreProtect") as? CoreProtect)?.api ?: return null
      if (!api.isEnabled) return null.also { log.warn("CoreProtect's API is off (api-enabled in its config); block changes by bosses aren't logged") }
      if (api.APIVersion() < MIN_API) return null.also { log.warn("CoreProtect's API ${api.APIVersion()} is older than $MIN_API; block changes by bosses aren't logged") }
      return CoreProtectLog(api, user)
    }
  }
}

/**
 * Block changes by abilities: each is checked against the [Guard]s, logged before it happens, in
 * the same tick, and then made. Temporary blocks, like cobwebs, go back to what was there after a
 * while, or when the plugin stops, so a restart never leaves them behind.
 */
class BlockChanges(private val plugin: MoBossesPlugin) {
  private class Temporary(val block: Block, val placed: BlockData, val original: BlockData, var task: BukkitTask?)

  private val temporary = LinkedHashMap<Location, Temporary>()

  /** Sets a block if the guards allow it; false when they don't. */
  fun set(block: Block, data: BlockData): Boolean {
    if (!plugin.guards.canChangeBlock(block.location)) return false
    val log = plugin.blockLog
    val old = block.blockData
    if (old.material != Material.AIR) log.removed(block.location, old)
    if (data.material != Material.AIR) log.placed(block.location, data)
    block.setBlockData(data, true)
    return true
  }

  /** Sets a block for [ticks], then puts the original back if nobody changed it in between. */
  fun setFor(block: Block, data: BlockData, ticks: Long): Boolean {
    val original = temporary[block.location]?.original ?: block.blockData
    if (!set(block, data)) return false
    temporary.remove(block.location)?.task?.cancel()
    val entry = Temporary(block, data, original, null)
    entry.task = Bukkit.getScheduler().runTaskLater(plugin, Runnable { restore(entry) }, ticks.coerceAtLeast(1))
    temporary[block.location] = entry
    return true
  }

  /** Puts every temporary block back now. */
  fun restoreAll() {
    temporary.values.toList().forEach { it.task?.cancel(); restore(it) }
  }

  private fun restore(entry: Temporary) {
    temporary.remove(entry.block.location)
    if (entry.block.blockData != entry.placed) return
    val log = plugin.blockLog
    log.removed(entry.block.location, entry.placed)
    if (entry.original.material != Material.AIR) log.placed(entry.block.location, entry.original)
    entry.block.setBlockData(entry.original, true)
  }
}
