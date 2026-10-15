package ru.mairwunnx.mobosses.hooks

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * `<papi:name>` in a message turns into PlaceholderAPI's `%name%` for the player who reads the message.
 * A tag, not raw `%…%` text, so nothing in a message changes unless it asks for a placeholder. Without
 * PlaceholderAPI the tag renders empty.
 */
object Placeholders {
  private val legacy = LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build()

  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

  fun resolver(player: Player): TagResolver = TagResolver.resolver("papi") { args, _ ->
    val name = args.popOr("<papi> needs a placeholder name, like <papi:player_health>").value()
    // Only touch PlaceholderAPI's classes when it's there; they aren't on the classpath otherwise.
    Tag.selfClosingInserting(if (available) render(player, name) else Component.empty())
  }

  private fun render(player: Player, name: String): Component =
    legacy.deserialize(PlaceholderAPI.setPlaceholders(player, "%$name%"))
}
