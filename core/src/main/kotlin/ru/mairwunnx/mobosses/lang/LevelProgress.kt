package ru.mairwunnx.mobosses.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings

/** A card's `<next_level>`: how far a player is on the way to their next level, or that they reached the top. */
object LevelProgress {
  fun resolver(languages: Languages, code: String, curve: Settings.LevelCurve, experience: Long): TagResolver {
    val way = Leveling.toNext(curve, experience)
    val line = if (way == null) languages.render(code, Message.INFO_TOP) else {
      val (gathered, asked) = way
      languages.render(
        code, Message.INFO_NEXT,
        Placeholder.unparsed("next", (Leveling.playerLevel(curve, experience) + 1).toString()),
        Placeholder.unparsed("progress", grouped(gathered)),
        Placeholder.unparsed("needed", grouped(asked)),
        Placeholder.unparsed("percent", (gathered * 100 / asked).coerceIn(0, 99).toString()),
      )
    }
    return Placeholder.component("next_level", line ?: Component.empty())
  }

  /** Thousands apart, as 1 047 042: the cards' numbers run into millions. */
  fun grouped(number: Long): String = number.toString().reversed().chunked(3).joinToString("\u00A0").reversed()
}
