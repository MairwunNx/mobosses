package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Catalog

/** The second step, as on Paper: the boss's class, in the ladder's order. */
class SpawnClassPage private constructor(mod: MoBosses, player: ServerPlayer, private val type: Catalog.BossType, private val classes: List<BossClass>) : Page(mod, player, 6) {
  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      SpawnMenu.BACK -> SpawnTypePage.open(mod, player, 0)
      SpawnMenu.CLOSE -> close()
      in classes.indices -> SpawnLevelPage.open(mod, player, type, classes[slot], DEFAULT_LEVEL)
      else -> return
    }
    sound()
  }

  companion object {
    private const val DEFAULT_LEVEL = 10

    fun open(mod: MoBosses, player: ServerPlayer, type: Catalog.BossType) {
      val code = mod.texts.languageOf(player)
      val languages = mod.languages
      val classes = mod.catalog.classes.all.take(45)
      with(SpawnClassPage(mod, player, type, classes)) {
        classes.forEachIndexed { slot, clazz -> set(slot, item(SpawnMenu.icon(clazz), listOf(mod.texts.className(code, clazz)) + languages.lines(code, Message.SPAWN_MENU_CLASS))) }
        SpawnMenu.footer(this)
        open(languages.render(code, Message.SPAWN_MENU_CLASS_TITLE, *SpawnMenu.chosen(mod, code, type, null, null)) ?: Component.text("Mo’Bosses"))
      }
    }
  }
}
