package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message

/**
 * The menu's way to spawn a boss, for admins with `mobosses.spawn`: [SpawnTypePage] picks the mob, [SpawnClassPage]
 * its class, [SpawnLevelPage] its level, and [SpawnPlacePage] the chunk, the last step. Each page's bottom row is the
 * same: back a step, and close.
 */
object SpawnMenu {
  const val PERMISSION = "mobosses.spawn"
  const val PREVIOUS = 45
  const val BACK = 47
  const val CLOSE = 51
  const val NEXT = 53

  fun open(plugin: MoBossesPlugin, player: Player) {
    if (!player.hasPermission(PERMISSION)) return plugin.texts.send(player, Message.NO_PERMISSION)
    SpawnTypePage.open(plugin, player, 0)
  }

  /** The highest level the menu offers, as `/mobosses spawn` takes. */
  fun maxLevel(plugin: MoBossesPlugin) = maxOf(100, plugin.catalog.settings.progression.levelCurve.maxLevel)

  fun footer(plugin: MoBossesPlugin, player: Player, inventory: Inventory) {
    val code = plugin.texts.languageOf(player)
    inventory.setItem(BACK, Page.item(Material.BOOK, plugin.languages.lines(code, Message.MENU_BACK)))
    inventory.setItem(CLOSE, Page.item(Material.OAK_DOOR, plugin.languages.lines(code, Message.MENU_CLOSE)))
  }

  /** `<boss_name>`, `<class>`, `<class_color>`, and `<level>` for a boss not spawned yet. */
  fun chosen(plugin: MoBossesPlugin, code: String, type: Catalog.BossType, clazz: BossClass?, level: Int?): Array<TagResolver> = arrayOf(
    Placeholder.component("boss_name", plugin.texts.bossName(code, type)),
    *listOfNotNull(
      clazz?.let { Placeholder.component("class", plugin.texts.className(code, it)) },
      clazz?.let { TagResolver.resolver("class_color", Tag.styling(Boss.colorOf(it))) },
      level?.let { Placeholder.unparsed("level", it.toString()) },
    ).toTypedArray(),
  )

  /** A class's picture: the class ladder's dyes, as in the achievements. */
  fun icon(clazz: BossClass): Material = when (clazz.key.lowercase()) {
    "common" -> Material.LIGHT_GRAY_DYE
    "uncommon" -> Material.YELLOW_DYE
    "rare" -> Material.LIGHT_BLUE_DYE
    "epic" -> Material.PURPLE_DYE
    "legendary" -> Material.GOLDEN_APPLE
    "mythic" -> Material.ENCHANTED_GOLDEN_APPLE
    else -> Material.PAPER
  }
}
