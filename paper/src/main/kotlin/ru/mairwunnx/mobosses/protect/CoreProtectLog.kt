package ru.mairwunnx.mobosses.protect

import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import ru.mairwunnx.mobosses.Log

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
