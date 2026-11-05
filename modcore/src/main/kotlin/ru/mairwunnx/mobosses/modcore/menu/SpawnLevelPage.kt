package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Catalog

/**
 * The third step, as on Paper: the level, a digit per column of glass panes, as many panes as the digit, with an arrow
 * over it for one more and one under it for one less. The boss in the middle below goes on to the place.
 */
class SpawnLevelPage private constructor(
  mod: MoBosses, player: ServerPlayer, private val type: Catalog.BossType, private val clazz: BossClass, private var level: Int,
) : Page(mod, player, 6) {
  private val max = SpawnMenu.maxLevel(mod)
  private val places = SpawnPlan.places(max)
  private val first = 4 - places / 2

  /** The digit's place a column shows: the leftmost the highest. */
  private fun place(column: Int) = (places - 1 - (column - first)).takeIf { column - first in 0 until places }

  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    val place = place(slot % 9)
    when {
      slot == SpawnMenu.BACK -> SpawnClassPage.open(mod, player, type)
      slot == SpawnMenu.CLOSE -> close()
      slot == CHOSEN -> SpawnPlacePage.open(mod, player, type, clazz, level)
      place != null && slot / 9 in setOf(UP_ROW, DOWN_ROW) -> {
        val next = SpawnPlan.step(level, place, up = slot / 9 == UP_ROW, max)
        if (next == level) return
        level = next
        draw()
      }
      else -> return
    }
    sound()
  }

  /** The digits and the boss they make, redrawn in place. */
  private fun draw() {
    val code = mod.texts.languageOf(player)
    val languages = mod.languages
    for (i in 0 until places) {
      val column = first + i
      val place = places - 1 - i
      val step = Placeholder.unparsed("step", SpawnPlan.step(0, place, up = true, Int.MAX_VALUE).toString())
      val digit = SpawnPlan.digit(level, place)
      set(UP_ROW * 9 + column, item(Items.ARROW, languages.lines(code, Message.SPAWN_MENU_UP, step)))
      val pane = item(if (digit == 0) Items.STAINED_GLASS_PANE.white() else Items.STAINED_GLASS_PANE.lightBlue(),
        languages.lines(code, Message.SPAWN_MENU_DIGIT, Placeholder.unparsed("digit", digit.toString())))
      pane.count = digit.coerceAtLeast(1)
      set(DIGIT_ROW * 9 + column, pane)
      set(DOWN_ROW * 9 + column, item(Items.SPECTRAL_ARROW, languages.lines(code, Message.SPAWN_MENU_DOWN, step)))
    }
    set(CHOSEN, item(egg(type.entity), languages.lines(code, Message.SPAWN_MENU_CHOSEN, *SpawnMenu.chosen(mod, code, type, clazz, level))))
  }

  companion object {
    private const val UP_ROW = 1
    private const val DIGIT_ROW = 2
    private const val DOWN_ROW = 3
    private const val CHOSEN = 40

    fun open(mod: MoBosses, player: ServerPlayer, type: Catalog.BossType, clazz: BossClass, level: Int) {
      val code = mod.texts.languageOf(player)
      with(SpawnLevelPage(mod, player, type, clazz, level.coerceIn(1, SpawnMenu.maxLevel(mod)))) {
        draw()
        SpawnMenu.footer(this)
        open(mod.languages.render(code, Message.SPAWN_MENU_LEVEL_TITLE) ?: Component.text("Mo’Bosses"))
      }
    }
  }
}
