package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message

/**
 * The third step: the level, a digit per column of glass panes, as many panes as the digit, with an arrow over it
 * for one more and one under it for one less. The boss in the middle below goes on to the place.
 */
class SpawnLevelPage private constructor(
  private val plugin: MoBossesPlugin, private val type: Catalog.BossType, private val clazz: BossClass, private var level: Int,
) : Page {
  private lateinit var inventory: Inventory
  private val max = SpawnMenu.maxLevel(plugin)
  private val places = SpawnPlan.places(max)
  private val first = 4 - places / 2

  override fun getInventory() = inventory

  /** The digit's place a column shows: the leftmost the highest. */
  private fun place(column: Int) = (places - 1 - (column - first)).takeIf { column - first in 0 until places }

  override fun click(player: Player, slot: Int, click: ClickType) {
    val place = place(slot % 9)
    when {
      slot == SpawnMenu.BACK -> SpawnClassPage.open(plugin, player, type)
      slot == SpawnMenu.CLOSE -> player.closeInventory()
      slot == CHOSEN -> SpawnPlacePage.open(plugin, player, type, clazz, level)
      place != null && slot / 9 in setOf(UP_ROW, DOWN_ROW) -> {
        val next = SpawnPlan.step(level, place, up = slot / 9 == UP_ROW, max)
        if (next == level) return
        level = next
        draw(player)
      }
      else -> return
    }
    sound(player)
  }

  /** The digits and the boss they make, redrawn in place. */
  private fun draw(player: Player) {
    val code = plugin.texts.languageOf(player)
    val languages = plugin.languages
    for (i in 0 until places) {
      val column = first + i
      val place = places - 1 - i
      val step = Placeholder.unparsed("step", SpawnPlan.step(0, place, up = true, Int.MAX_VALUE).toString())
      val digit = SpawnPlan.digit(level, place)
      inventory.setItem(UP_ROW * 9 + column, Page.item(Material.ARROW, languages.lines(code, Message.SPAWN_MENU_UP, step)))
      val pane = Page.item(if (digit == 0) Material.WHITE_STAINED_GLASS_PANE else Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        languages.lines(code, Message.SPAWN_MENU_DIGIT, Placeholder.unparsed("digit", digit.toString())))
      pane.amount = digit.coerceAtLeast(1)
      inventory.setItem(DIGIT_ROW * 9 + column, pane)
      inventory.setItem(DOWN_ROW * 9 + column, Page.item(Material.SPECTRAL_ARROW, languages.lines(code, Message.SPAWN_MENU_DOWN, step)))
    }
    val egg = Bukkit.getItemFactory().getSpawnEgg(type.entity) ?: Material.ZOMBIE_SPAWN_EGG
    inventory.setItem(CHOSEN, Page.item(egg, languages.lines(code, Message.SPAWN_MENU_CHOSEN, *SpawnMenu.chosen(plugin, code, type, clazz, level))))
  }

  companion object {
    private const val UP_ROW = 1
    private const val DIGIT_ROW = 2
    private const val DOWN_ROW = 3
    private const val CHOSEN = 40

    fun open(plugin: MoBossesPlugin, player: Player, type: Catalog.BossType, clazz: BossClass, level: Int) {
      val code = plugin.texts.languageOf(player)
      val page = SpawnLevelPage(plugin, type, clazz, level.coerceIn(1, SpawnMenu.maxLevel(plugin)))
      page.inventory = Bukkit.createInventory(page, 54, plugin.languages.render(code, Message.SPAWN_MENU_LEVEL_TITLE) ?: Component.text("Mo’Bosses"))
      page.draw(player)
      SpawnMenu.footer(plugin, player, page.inventory)
      player.openInventory(page.inventory)
    }
  }
}
