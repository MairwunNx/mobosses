package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.hooks.Placeholders
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Each player's language and the texts they get. The country lookup runs during login, off the
 * server thread; the language follows from it and from the game's language setting, as in Mo’Joins.
 */
class Texts(private val plugin: MoBossesPlugin) : Listener {
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

  fun country(player: Player): String? = countries[player.uniqueId]

  fun languageOf(player: Player): String = plugin.resolver.resolve(countries[player.uniqueId], player.locale())

  fun languageOf(sender: CommandSender): String = (sender as? Player)?.let(::languageOf) ?: plugin.languages.default

  fun send(to: CommandSender, message: Message, vararg resolvers: TagResolver) {
    val code = languageOf(to)
    plugin.languages.render(code, message, *resolvers, *personal(to))?.let(to::sendMessage)
  }

  /** Sends a message to every player online, each in their language, and to the console in the default one. */
  fun broadcast(message: Message, resolvers: (code: String) -> Array<TagResolver>) {
    Bukkit.getOnlinePlayers().forEach { player ->
      val code = languageOf(player)
      plugin.languages.render(code, message, *resolvers(code), *personal(player))?.let(player::sendMessage)
    }
    val code = plugin.languages.default
    plugin.languages.render(code, message, *resolvers(code))?.let(Bukkit.getConsoleSender()::sendMessage)
  }

  /** `<boss_name>`, `<class>`, `<class_color>`, and `<level>` for a boss, in a language. */
  fun boss(code: String, boss: Boss): Array<TagResolver> = arrayOf(
    Placeholder.component("boss_name", bossName(code, boss.type)),
    Placeholder.component("class", className(code, boss.clazz)),
    TagResolver.resolver("class_color", Tag.styling(boss.color)),
    Placeholder.unparsed("level", boss.level.toString()),
  )

  fun bossName(code: String, type: Catalog.BossType): Component =
    type.spec.name?.let { plugin.languages.deserialize(it) } ?: plugin.languages.name(code, "bosses.${type.key}")

  fun className(code: String, clazz: BossClass): Component =
    (clazz.displayName?.let { plugin.languages.deserialize(it) } ?: plugin.languages.name(code, "classes.${clazz.key}")).colorIfAbsent(Boss.colorOf(clazz))

  fun minionName(code: String, minion: Catalog.Minion): Component? =
    minion.name?.let { plugin.languages.deserialize(it) } ?: plugin.languages.render(code, "minions.${minion.summoner.key.key}")

  /** Tags that read the player who gets the message. */
  private fun personal(to: CommandSender): Array<TagResolver> = if (to is Player) arrayOf(Placeholders.resolver(to)) else emptyArray()
}
