package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Catalog

/**
 * The menu's way to spawn a boss, as on Paper, for admins with `mobosses.spawn`: [SpawnTypePage] picks the mob,
 * [SpawnClassPage] its class, [SpawnLevelPage] its level, and [SpawnPlacePage] the chunk, the last step. Each page's
 * bottom row is the same: back a step, and close.
 */
object SpawnMenu {
  const val PREVIOUS = 45
  const val BACK = 47
  const val CLOSE = 51
  const val NEXT = 53

  fun open(mod: MoBosses, player: ServerPlayer) {
    if (!Perms.check(player.createCommandSourceStack(), Permissions.SPAWN)) return mod.texts.send(player, Message.NO_PERMISSION)
    SpawnTypePage.open(mod, player, 0)
  }

  /** The highest level the menu offers, as `/mobosses spawn` takes. */
  fun maxLevel(mod: MoBosses) = maxOf(100, mod.catalog.settings.progression.levelCurve.maxLevel)

  fun footer(page: Page) {
    val code = page.mod.texts.languageOf(page.player)
    page.set(BACK, page.item(Items.BOOK, page.mod.languages.lines(code, Message.MENU_BACK)))
    page.set(CLOSE, page.item(Items.OAK_DOOR, page.mod.languages.lines(code, Message.MENU_CLOSE)))
  }

  /** `<boss_name>`, `<class>`, `<class_color>`, and `<level>` for a boss not spawned yet. */
  fun chosen(mod: MoBosses, code: String, type: Catalog.BossType, clazz: BossClass?, level: Int?): Array<TagResolver> = arrayOf(
    Placeholder.component("boss_name", mod.texts.bossName(code, type)),
    *listOfNotNull(
      clazz?.let { Placeholder.component("class", mod.texts.className(code, it)) },
      clazz?.let { TagResolver.resolver("class_color", Tag.styling(Boss.colorOf(it))) },
      level?.let { Placeholder.unparsed("level", it.toString()) },
    ).toTypedArray(),
  )

  /** A class's picture: the class ladder's dyes, as in the achievements. */
  fun icon(clazz: BossClass): Item = when (clazz.key.lowercase()) {
    "common" -> Items.DYE.lightGray()
    "uncommon" -> Items.DYE.yellow()
    "rare" -> Items.DYE.lightBlue()
    "epic" -> Items.DYE.purple()
    "legendary" -> Items.GOLDEN_APPLE
    "mythic" -> Items.ENCHANTED_GOLDEN_APPLE
    else -> Items.PAPER
  }
}
