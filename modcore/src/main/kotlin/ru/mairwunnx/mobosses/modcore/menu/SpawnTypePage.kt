package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Catalog

/** The first step of spawning a boss, as on Paper: its mob, 45 to a page, by the language's names. */
class SpawnTypePage private constructor(mod: MoBosses, player: ServerPlayer, private val page: Int, private val pages: Int, private val types: List<Catalog.BossType>) :
  Page(mod, player, 6) {
  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      SpawnMenu.PREVIOUS -> if (page > 0) open(mod, player, page - 1) else return
      SpawnMenu.NEXT -> if (page < pages - 1) open(mod, player, page + 1) else return
      SpawnMenu.BACK -> Menu.open(mod, player)
      SpawnMenu.CLOSE -> close()
      in types.indices -> SpawnClassPage.open(mod, player, types[slot])
      else -> return
    }
    sound()
  }

  companion object {
    private const val PER_PAGE = 45

    fun open(mod: MoBosses, player: ServerPlayer, page: Int) {
      val code = mod.texts.languageOf(player)
      val languages = mod.languages
      val all = mod.catalog.types.values.filter { it.spec.enabled }.sortedBy { it.key }
      val pages = ((all.size + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)
      val current = page.coerceIn(0, pages - 1)
      val shown = all.drop(current * PER_PAGE).take(PER_PAGE)
      val counts = arrayOf(Placeholder.unparsed("page", (current + 1).toString()), Placeholder.unparsed("pages", pages.toString()))

      with(SpawnTypePage(mod, player, current, pages, shown)) {
        shown.forEachIndexed { slot, type -> set(slot, item(egg(type.entity), listOf(mod.texts.bossName(code, type)) + languages.lines(code, Message.SPAWN_MENU_TYPE))) }
        if (current > 0) set(SpawnMenu.PREVIOUS, item(Items.ARROW, languages.lines(code, Message.MENU_PREVIOUS)))
        if (current < pages - 1) set(SpawnMenu.NEXT, item(Items.ARROW, languages.lines(code, Message.MENU_NEXT)))
        SpawnMenu.footer(this)
        open(languages.render(code, Message.SPAWN_MENU_TYPE_TITLE, *counts) ?: Component.text("Mo’Bosses"))
      }
    }
  }
}
