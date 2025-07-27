package ru.mobosses.managers

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor.fromHexString
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag.styling
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.resolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.mobosses.PluginUnit
import java.io.Closeable

class MessageManager(private val plugin: PluginUnit) : Closeable {
  private val audiences = BukkitAudiences.create(plugin)
  private val mm: MiniMessage = MiniMessage.builder().tags(TagResolver.standard()).tags(colorTags()).build()

  fun send(to: CommandSender, raw: String, args: Map<String, String> = emptyMap()) = audience(to).sendMessage(component(raw, args))
  fun send(to: Audience, raw: String, args: Map<String, String> = emptyMap()) = to.sendMessage(component(raw, args))
  fun send(to: CommandSender, raw: String, stringArgs: Map<String, String>, componentArgs: Map<String, Component>) = send(audience(to), raw, stringArgs, componentArgs)
  private fun send(audience: Audience, raw: String, stringArgs: Map<String, String>, componentArgs: Map<String, Component>) = audience.sendMessage(component(raw, stringArgs, componentArgs))

  fun whisper(player: Player, raw: String, args: Map<String, String> = emptyMap()) = audiences.player(player).sendMessage(component(raw, args))
  fun whisper(player: Player, raw: String, stringArgs: Map<String, String>, componentArgs: Map<String, Component>) = send(audiences.player(player), raw, stringArgs, componentArgs)

  fun broadcast(raw: String, args: Map<String, String> = emptyMap()) = audiences.all().sendMessage(component(raw, args))
  fun broadcastPlayers(raw: String, args: Map<String, String> = emptyMap()) = audiences.players().sendMessage(component(raw, args))
  fun broadcast(raw: String, stringArgs: Map<String, String>, componentArgs: Map<String, Component>) = send(audiences.all(), raw, stringArgs, componentArgs)
  fun broadcastPlayers(raw: String, stringArgs: Map<String, String>, componentArgs: Map<String, Component>) = send(audiences.players(), raw, stringArgs, componentArgs)

  fun component(
    raw: String,
    stringArgs: Map<String, String> = emptyMap(),
    componentArgs: Map<String, Component> = emptyMap()
  ): Component {
    val resolvers = buildList<TagResolver> {
      stringArgs.forEach { (k, v) -> add(unparsed(k, v)) }
      componentArgs.forEach { (k, v) -> add(component(k, v)) }
    }
    val resolver = resolver(TagResolver.standard(), *resolvers.toTypedArray())
    return mm.deserialize(raw, resolver)
  }
  fun audience(sender: CommandSender): Audience =
    if (sender is Player) audiences.player(sender) else audiences.sender(sender)

  override fun close() = audiences.close()

  private fun colorTags() = resolver(
    tag("pri",  "#00E676"),
    tag("ok",   "#4CAF50"),
    tag("warn", "#FFC107"),
    tag("err",  "#FF5252", bold = true),
    tag("boss", "#FF1744"),
    tag("num",  "#00B0FF"),
    tag("hp",   "#4CAF50"),
    tag("xp",   "#7C4DFF", bold = true),
    tag("item", "#7C4DFF", bold = true),
  )

  private fun tag(name: String, hex: String, bold: Boolean = false): TagResolver {
    val color = fromHexString(hex)!! // Точно правильный цвет, железяка
    return if (bold) {
      resolver(name, styling(color, TextDecoration.BOLD))
    } else {
      resolver(name, styling(color))
    }
  }
}