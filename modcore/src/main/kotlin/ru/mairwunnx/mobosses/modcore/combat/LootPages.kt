package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.combat.LootRules
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Catalog
import ru.mairwunnx.mobosses.modcore.net.Payloads
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The loot table as pages for a player's recipe viewer (JEI, REI, EMI): each entry as it drops, at its least amount,
 * named in the player's language, with lines for the lowest class that drops it, the boss levels, the boss types it
 * belongs to, and its odds for that class at its lowest level, by [LootRules]. A game without the mod never asks for them.
 */
class LootPages(private val mod: MoBosses) {
  fun send(player: ServerPlayer) {
    if (!Mod.loader.canSend(player, Payloads.LootPages.TYPE)) return
    Mod.loader.send(player, Payloads.LootPages(pages(mod.texts.languageOf(player))))
  }

  fun sendAll() = mod.server.playerList.players.forEach(::send)

  private fun pages(code: String): List<Payloads.LootPage> {
    val catalog = mod.catalog
    return catalog.loot.map { loot ->
      val spec = loot.spec
      val clazz = catalog.classes.byKey(spec.mobclass) ?: catalog.classes.all.first()
      val className = Placeholder.component("class", mod.texts.className(code, clazz))
      val bosses = loot.types.mapNotNull { type -> catalog.types[type]?.let { mod.texts.bossName(code, it) } }
      val lines = listOfNotNull(
        mod.languages.render(code, Message.LOOT_PAGE_CLASS, className),
        mod.languages.render(code, Message.LOOT_PAGE_LEVELS, Placeholder.unparsed("min", spec.minLevel.toString()), Placeholder.unparsed("max", spec.maxLevel.toString())),
        if (bosses.isEmpty()) mod.languages.render(code, Message.LOOT_PAGE_EVERY_BOSS)
        else mod.languages.render(code, Message.LOOT_PAGE_BOSSES, Placeholder.component("bosses", Component.join(net.kyori.adventure.text.JoinConfiguration.commas(true), bosses))),
        mod.languages.render(code, Message.LOOT_PAGE_ODDS, Placeholder.unparsed("percent", percent(loot)), className),
      )
      Payloads.LootPage(mod.loot.stack(loot, spec.minLevel, code).also { it.count = spec.amount.first.coerceIn(1, it.maxStackSize) }, lines.map(mod.texts::vanilla))
    }
  }

  /** The entry's share of the drops of a boss of its own lowest class at its lowest level, by weight, as a short number. */
  private fun percent(loot: Catalog.Loot): String {
    val catalog = mod.catalog
    val spec = loot.spec
    val type = loot.types.firstOrNull()
    val pool = catalog.loot.mapNotNull { other ->
      val theirs = other.types.isEmpty() || (type != null && type in other.types)
      LootRules.weight(other.spec, catalog.classes, spec.mobclass, spec.minLevel, theirs)
    }.sum()
    val own = LootRules.weight(spec, catalog.classes, spec.mobclass, spec.minLevel, true) ?: 0.0
    val share = if (pool > 0) own * 100 / pool else 0.0
    return if (share >= 10) share.roundToInt().toString() else "%.1f".format(Locale.ROOT, share)
  }
}
