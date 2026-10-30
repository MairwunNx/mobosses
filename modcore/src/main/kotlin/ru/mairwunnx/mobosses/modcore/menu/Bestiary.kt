package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.progress.BestiaryEntry

/**
 * A player's bestiary, as on Paper: every boss type, with their kills and best level in each class. A type they never
 * beat shows as a gray “???”.
 */
class Bestiary private constructor(mod: MoBosses, player: ServerPlayer) : Page(mod, player, 6) {
  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      BACK -> Menu.open(mod, player)
      CLOSE -> close()
      else -> return
    }
    sound()
  }

  companion object {
    private const val BACK = 45
    private const val PROGRESS = 49
    private const val CLOSE = 53

    fun open(mod: MoBosses, player: ServerPlayer) {
      mod.progression.bestiary(player.uuid).whenComplete { entries, error ->
        mod.server.execute {
          if (player.hasDisconnected()) return@execute
          if (error != null) return@execute mod.texts.send(player, Message.NO_DATABASE)
          build(mod, player, entries)
        }
      }
    }

    private fun build(mod: MoBosses, player: ServerPlayer, entries: List<BestiaryEntry>) {
      val languages = mod.languages
      val code = mod.texts.languageOf(player)
      val page = Bestiary(mod, player)
      val classes = mod.catalog.classes.all
      val types = mod.catalog.types.values.filter { it.spec.enabled }.take(BACK)
      val byType = entries.groupBy { it.type }
      types.forEachIndexed { slot, type ->
        val known = byType[type.key].orEmpty().associateBy { it.clazz }
        page.set(slot, if (known.isEmpty()) {
          page.item(Items.DYE.gray(), languages.lines(code, Message.BESTIARY_UNKNOWN))
        } else {
          val lore = classes.mapNotNull { clazz ->
            val entry = known[clazz.key]
            val name = Placeholder.component("class", mod.texts.className(code, clazz))
            if (entry == null) languages.render(code, Message.BESTIARY_MISSING, name)
            else languages.render(code, Message.BESTIARY_LINE, name, Placeholder.unparsed("kills", entry.kills.toString()), Placeholder.unparsed("level", entry.bestLevel.toString()))
          }
          page.item(Page.egg(type.entity), listOf(mod.texts.bossName(code, type)) + lore)
        })
      }
      val opened = entries.count { entry -> types.any { it.key == entry.type } && classes.any { it.key == entry.clazz } }
      page.set(PROGRESS, page.item(Items.BOOK, languages.lines(code, Message.BESTIARY_PROGRESS,
        Placeholder.unparsed("opened", opened.toString()), Placeholder.unparsed("total", (types.size * classes.size).toString()))))
      page.set(BACK, page.item(Items.ARROW, languages.lines(code, Message.MENU_BACK)))
      page.set(CLOSE, page.item(Items.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
      page.open(languages.render(code, Message.BESTIARY_TITLE) ?: Component.text("Mo’Bosses"))
    }
  }
}
