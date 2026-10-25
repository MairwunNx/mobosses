package ru.mairwunnx.mobosses.modcore.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Catalog
import ru.mairwunnx.mobosses.modcore.hooks.Placeholders
import ru.mairwunnx.mobosses.modcore.text.Chat

/**
 * The texts players get, as on Paper: messages in each one's language, and the names of bosses, classes, and minions.
 * The language files render Adventure's texts; [vanilla] makes them the game's.
 */
class Texts(private val mod: MoBosses) {
  fun languageOf(player: ServerPlayer): String = mod.playerLanguages.of(player)

  fun languageOf(source: CommandSourceStack): String = mod.playerLanguages.of(source)

  fun vanilla(text: Component): net.minecraft.network.chat.Component = Chat.vanilla(text, mod.server.registryAccess())

  fun render(code: String, message: Message, vararg resolvers: TagResolver, player: ServerPlayer? = null): Component? =
    mod.languages.render(code, message, *resolvers, *personal(player))

  fun send(to: ServerPlayer, message: Message, vararg resolvers: TagResolver) {
    render(languageOf(to), message, *resolvers, player = to)?.let { to.sendSystemMessage(vanilla(it)) }
  }

  fun send(to: CommandSourceStack, message: Message, vararg resolvers: TagResolver) {
    val player = to.player
    if (player != null) return send(player, message, *resolvers)
    render(languageOf(to), message, *resolvers)?.let { to.sendSystemMessage(vanilla(it)) }
  }

  /** Several messages as one block in the chat, line under line; the ones a language leaves empty drop out. */
  fun sendBlock(to: ServerPlayer, lines: List<Pair<Message, Array<out TagResolver>>>) {
    val code = languageOf(to)
    val rendered = lines.mapNotNull { (message, resolvers) -> render(code, message, *resolvers, player = to) }
    if (rendered.isNotEmpty()) to.sendSystemMessage(vanilla(Component.join(JoinConfiguration.newlines(), rendered)))
  }

  /** A message over the hotbar instead of in the chat, for warnings that come often, like abilities in a fight. */
  fun actionBar(to: ServerPlayer, message: Message, vararg resolvers: TagResolver) {
    render(languageOf(to), message, *resolvers, player = to)?.let { to.sendSystemMessage(vanilla(it), true) }
  }

  /** Sends a message to [players], each in their language; [resolvers] fill it per language. */
  fun sendAll(players: Collection<ServerPlayer>, message: Message, resolvers: (code: String) -> Array<TagResolver>) {
    players.forEach { player ->
      val code = languageOf(player)
      render(code, message, *resolvers(code), player = player)?.let { player.sendSystemMessage(vanilla(it)) }
    }
  }

  /** Sends a message to every player online, each in their language, and to the server's log in the default one. */
  fun broadcast(message: Message, resolvers: (code: String) -> Array<TagResolver>) {
    sendAll(mod.server.playerList.players, message, resolvers)
    val code = mod.languages.default
    render(code, message, *resolvers(code))?.let { mod.server.sendSystemMessage(vanilla(it)) }
  }

  /** `<boss_name>`, `<class>`, `<class_color>`, and `<level>` for a boss, in a language. */
  fun boss(code: String, boss: Boss): Array<TagResolver> = arrayOf(
    Placeholder.component("boss_name", bossName(code, boss.type)),
    Placeholder.component("class", className(code, boss.clazz)),
    TagResolver.resolver("class_color", Tag.styling(boss.color)),
    Placeholder.unparsed("level", boss.level.toString()),
  )

  /** Another mod's mob has no name in the language files: the game's own name for it stands in, which every client translates. */
  fun bossName(code: String, type: Catalog.BossType): Component =
    type.spec.name?.let { mod.languages.deserialize(it) }
      ?: mod.languages.render(code, "bosses.${type.key}")
      ?: Component.translatable(type.entity.descriptionId)

  fun className(code: String, clazz: BossClass): Component =
    (clazz.displayName?.let { mod.languages.deserialize(it) } ?: mod.languages.name(code, "classes.${clazz.key}")).colorIfAbsent(Boss.colorOf(clazz))

  fun minionName(code: String, minion: Catalog.Minion): Component? =
    minion.name?.let { mod.languages.deserialize(it) } ?: mod.languages.render(code, "minions.${Catalog.keyOf(minion.summoner)}")

  /** Tags that read the player who gets the message: Text Placeholder API's. */
  private fun personal(player: ServerPlayer?): Array<TagResolver> = arrayOf(Placeholders.resolver(mod, player))
}
