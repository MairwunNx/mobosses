package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.hooks.MiniPlaceholderTags
import ru.mairwunnx.mobosses.hooks.Placeholders

/** The texts players get: messages in each one's language, and the names of bosses, classes, and minions. */
class Texts(private val plugin: MoBossesPlugin) {
  fun languageOf(player: Player): String = plugin.playerLanguages.of(player)

  fun languageOf(sender: CommandSender): String = plugin.playerLanguages.of(sender)

  fun send(to: CommandSender, message: Message, vararg resolvers: TagResolver) {
    val code = languageOf(to)
    plugin.languages.render(code, message, *resolvers, *personal(to), target = to as? Player)?.let(to::sendMessage)
  }

  /** Sends a message to every player online, each in their language, and to the console in the default one. */
  fun broadcast(message: Message, resolvers: (code: String) -> Array<TagResolver>) {
    Bukkit.getOnlinePlayers().forEach { player ->
      val code = languageOf(player)
      plugin.languages.render(code, message, *resolvers(code), *personal(player), target = player)?.let(player::sendMessage)
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

  /** Tags that read the player who gets the message: PlaceholderAPI's and MiniPlaceholders'. */
  private fun personal(to: CommandSender): Array<TagResolver> = if (to is Player) arrayOf(Placeholders.resolver(to), MiniPlaceholderTags.resolver()) else emptyArray()
}
