package ru.mairwunnx.mobosses.hooks

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.progress.HunterValues

/**
 * `%mobosses_level%`, `%mobosses_xp%`, `%mobosses_kills%`, `%mobosses_deaths%`, `%mobosses_streak%`, and
 * `%mobosses_damage%` for scoreboards, tab lists, and chat
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
    return HunterValues.value(params, plugin.progression.of(id), plugin.progression.level(id))
  }
}
