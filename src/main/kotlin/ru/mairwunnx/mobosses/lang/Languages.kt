package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import ru.mairwunnx.mobosses.config.Settings
import java.util.Locale

/** The fixed keys a language file sets. A key missing from one language comes from the default language, then from English. */
enum class Message(val key: String) {
  RELOADED("reloaded"),
  RELOAD_FAILED("reload_failed"),
  NO_PERMISSION("no_permission"),
  USAGE("usage"),
  SPAWN_USAGE("spawn_usage"),
  SPAWNED("spawned"),
  SPAWN_FAILED("spawn_failed"),
  SPAWN_WHERE("spawn_where"),
  UNKNOWN_ENTITY("unknown_entity"),
  UNKNOWN_CLASS("unknown_class"),
  BAD_LEVEL("bad_level"),
  KILLED_ALL("killed_all"),
  KILLED_NONE("killed_none"),
  BOSS_TAG("boss_tag"),
  BOSS_BAR("boss_bar"),
  KILL("kill"),
  LOOT_RARE("loot.rare"),
  LOOT_EPIC("loot.epic"),
  LOOT_LEGENDARY("loot.legendary"),
  LOOT_MYTHIC("loot.mythic"),
  LIGHTNING("abilities.lightning"),
  SUMMONING("abilities.summoning"),
  REGENERATION("abilities.regeneration"),
  FIRE_RING("abilities.fire_ring"),
  WEB_RING("abilities.web_ring"),
  TELEPORT_STRIKE("abilities.teleport_strike"),
  METEOR_SHOWER("abilities.meteor_shower"),
  ARROW_ARC("abilities.arrow_arc"),
  ARROW_RAIN("abilities.arrow_rain"),
}

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
  fun render(code: String, message: Message, vararg resolvers: TagResolver): Component? = render(code, message.key, *resolvers)

  fun render(code: String, key: String, vararg resolvers: TagResolver): Component? =
    text(code, key)?.takeIf { it.isNotBlank() }?.let { deserialize(it, *resolvers) }

  /** A name from the language files, or [key]'s last part when no language has it. */
  fun name(code: String, key: String): Component = render(code, key) ?: Component.text(key.substringAfterLast('.'))

  fun deserialize(text: String, vararg resolvers: TagResolver): Component =
    if (LEGACY.containsMatchIn(text)) legacy.deserialize(text.replace('§', '&')) else mini.deserialize(text, COLORS, *resolvers)

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

/** Picks a player's language from their country and their game's language setting. */
class LocaleResolver(private val settings: Settings.Locales, private val available: Set<String>, defaultCode: String = settings.default) {
  private val byCountry: Map<String, String> = buildMap {
    settings.countries.forEach { (code, countries) -> countries.forEach { putIfAbsent(it.uppercase(), normalize(code)) } }
  }

  val default: String = normalize(defaultCode).takeIf { it in available } ?: Languages.FALLBACK

  fun resolve(country: String?, client: Locale?): String {
    for (source in settings.sources) {
      val code = when (source) {
        Settings.Source.IP -> country?.let { byCountry[it.uppercase()] }
        Settings.Source.CLIENT -> client?.let(::fromClient)
      }
      if (code != null && code in available) return code
    }
    return default
  }

  /** `pt_BR` matches a `pt_br` file first, then `pt`. */
  private fun fromClient(locale: Locale): String? {
    val full = normalize(locale.toString())
    val language = normalize(locale.language)
    return when {
      full in available -> full
      language in available -> language
      else -> null
    }
  }

  companion object {
    fun normalize(code: String) = code.trim().lowercase().replace('-', '_')
  }
}
