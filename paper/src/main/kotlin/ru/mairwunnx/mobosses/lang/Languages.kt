package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.pointer.Pointered
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * The texts of every language, by code (`en`, `ru`, `pt_br`), flattened: `items.x.name` for a nested
 * key, lines joined with new lines for a list. Besides [Message], they hold the names of classes
 * (`classes.<key>`), bosses (`bosses.<type>`), minions (`minions.<summoner>`), and loot
 * (`items.<id>.name`, `items.<id>.lore`).
 */
class Languages(private val texts: Map<String, Map<String, String>>, val default: String) {
  private val mini = MiniMessage.miniMessage()

  val codes: Set<String> get() = texts.keys

  /** The raw text, or null when no language sets it. */
  fun text(code: String, key: String): String? = texts[code]?.get(key) ?: texts[default]?.get(key) ?: texts[FALLBACK]?.get(key)

  /** A message in a language; null when its text is empty, which turns it off. */
  /** A message in a language; [target] is the player it's for, for tags that read a player. */
  fun render(code: String, message: Message, vararg resolvers: TagResolver, target: Pointered? = null): Component? = render(code, message.key, *resolvers, target = target)

  fun render(code: String, key: String, vararg resolvers: TagResolver, target: Pointered? = null): Component? =
    text(code, key)?.takeIf { it.isNotBlank() }?.let { deserialize(it, *resolvers, target = target) }

  /** A message line by line, for item lore; empty when the message is off. */
  fun lines(code: String, message: Message, vararg resolvers: TagResolver): List<Component> =
    text(code, message.key)?.takeIf { it.isNotBlank() }?.split('\n')?.map { deserialize(it, *resolvers) }.orEmpty()

  /** A name from the language files, or [key]'s last part when no language has it. */
  fun name(code: String, key: String): Component = render(code, key) ?: Component.text(key.substringAfterLast('.'))

  fun deserialize(text: String, vararg resolvers: TagResolver, target: Pointered? = null): Component = when {
    LEGACY.containsMatchIn(text) -> legacy.deserialize(text.replace('§', '&'))
    target != null -> mini.deserialize(text, target, COLORS, *resolvers)
    else -> mini.deserialize(text, COLORS, *resolvers)
  }

  companion object {
    const val FALLBACK = "en"
    val BUNDLED = listOf("en", "ru", "es", "pt", "de")

    /** 1.x's names and lore used `&` or `§` color codes; those still read as such. */
    private val LEGACY = Regex("[&§][0-9a-fk-orA-FK-OR]")
    private val legacy = LegacyComponentSerializer.legacyAmpersand()

    private fun tag(name: String, hex: String, bold: Boolean = false): TagResolver {
      val color = TextColor.fromHexString(hex)!!
      return TagResolver.resolver(name, if (bold) Tag.styling(color, TextDecoration.BOLD) else Tag.styling(color))
    }

    /** The color tags 1.x's messages used, kept so custom texts carry over. */
    val COLORS: TagResolver = TagResolver.resolver(
      tag("pri", "#00E676"),
      tag("ok", "#4CAF50"),
      tag("warn", "#FFC107"),
      tag("err", "#FF5252", bold = true),
      tag("boss", "#FF1744"),
      tag("num", "#00B0FF"),
      tag("hp", "#4CAF50"),
      tag("xp", "#7C4DFF", bold = true),
      tag("item", "#7C4DFF", bold = true),
    )
  }
}
