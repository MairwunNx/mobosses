package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
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
 * Each player's language: the one they chose with `/mobosses language`, while its file is there, or
 * else the automatic one. The country lookup runs during login, off the server thread; the automatic
 * language follows from it and from the game's language setting, as in Mo’Joins.
 */
class PlayerLanguages(private val plugin: MoBossesPlugin) : Listener {
  private val countries = ConcurrentHashMap<UUID, String>()
  private val choices by lazy { LanguageChoices(plugin.dataFolder.resolve("player-languages.yml")).also { it.load() } }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
    plugin.geoIp.country(event.address)?.let { countries[event.uniqueId] = it }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) {
    countries.remove(event.player.uniqueId)
  }

  fun of(player: Player): String = chosen(player) ?: automatic(player)

  fun of(sender: CommandSender): String = (sender as? Player)?.let(::of) ?: plugin.languages.default

  /** The player's own choice, or null when the plugin picks. */
  fun chosen(player: Player): String? = choices.of(player.uniqueId)?.takeIf { it in plugin.languages.codes }

  /** The language the plugin picks for a player who chose none. */
  fun automatic(player: Player): String = plugin.resolver.resolve(countries[player.uniqueId], player.locale())

  /** The languages to choose from, the bundled ones first. */
  fun codes(): List<String> = plugin.languages.codes.sortedWith(compareBy({ it !in Languages.BUNDLED }, { Languages.BUNDLED.indexOf(it) }, { it }))

  /** Sets the player's language, or with null hands it back to the plugin; they hear about it in the language they get. */
  fun choose(player: Player, code: String?) {
    choices.set(player.uniqueId, code)
    // The bar of the boss they fight would stay in the old language; the next hit shows it in the new one.
    plugin.bars.hide(player)
    val name = Placeholder.unparsed("language", LanguageChoices.nativeName(of(player)))
    plugin.texts.send(player, if (code == null) Message.LANGUAGE_AUTO else Message.LANGUAGE_SET, name)
  }
}
