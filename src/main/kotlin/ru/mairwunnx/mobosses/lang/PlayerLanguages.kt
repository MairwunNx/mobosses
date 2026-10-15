package ru.mairwunnx.mobosses.lang

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Each player's language. The country lookup runs during login, off the server thread; the language
 * follows from it and from the game's language setting, as in Mo’Joins.
 */
class PlayerLanguages(private val plugin: MoBossesPlugin) : Listener {
  private val countries = ConcurrentHashMap<UUID, String>()

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
    plugin.geoIp.country(event.address)?.let { countries[event.uniqueId] = it }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) {
    countries.remove(event.player.uniqueId)
  }

  fun of(player: Player): String = plugin.resolver.resolve(countries[player.uniqueId], player.locale())

  fun of(sender: CommandSender): String = (sender as? Player)?.let(::of) ?: plugin.languages.default
}
