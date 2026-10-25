package ru.mairwunnx.mobosses.hooks

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * `%mobosses_level%`, `%mobosses_xp%`, and `%mobosses_kills%` for scoreboards, tab lists, and chat
 * plugins. PlaceholderAPI may ask from any thread, so this reads only the progress cache, which
 * holds players who are online; anyone else reads as 0, or level 1.
 */
class Expansion(private val plugin: MoBossesPlugin) : PlaceholderExpansion() {
  override fun getIdentifier() = "mobosses"
  override fun getAuthor() = "MairwunNx"
  override fun getVersion(): String = plugin.pluginMeta.version
  override fun persist() = true

  override fun onRequest(player: OfflinePlayer?, params: String): String? {
    val id = player?.uniqueId ?: return null
    return when (params.lowercase()) {
      "level" -> plugin.progression.level(id).toString()
      "xp", "experience" -> plugin.progression.of(id).experience.toString()
      "kills" -> plugin.progression.of(id).kills.toString()
      else -> null
    }
  }
}
