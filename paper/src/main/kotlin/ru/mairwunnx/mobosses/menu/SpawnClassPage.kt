package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message

/** The second step: the boss's class, in the ladder's order. */
class SpawnClassPage private constructor(private val plugin: MoBossesPlugin, private val type: Catalog.BossType, private val classes: List<BossClass>) : Page {
  private lateinit var inventory: Inventory

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      SpawnMenu.BACK -> SpawnTypePage.open(plugin, player, 0)
      SpawnMenu.CLOSE -> player.closeInventory()
      in classes.indices -> SpawnLevelPage.open(plugin, player, type, classes[slot], DEFAULT_LEVEL)
      else -> return
    }
    sound(player)
  }

  companion object {
    private const val DEFAULT_LEVEL = 10

    fun open(plugin: MoBossesPlugin, player: Player, type: Catalog.BossType) {
      val code = plugin.texts.languageOf(player)
      val languages = plugin.languages
      val classes = plugin.catalog.classes.all.take(45)
      val page = SpawnClassPage(plugin, type, classes)
      val inventory = Bukkit.createInventory(page, 54, languages.render(code, Message.SPAWN_MENU_CLASS_TITLE, *SpawnMenu.chosen(plugin, code, type, null, null)) ?: Component.text("Mo’Bosses"))
      page.inventory = inventory
      classes.forEachIndexed { slot, clazz ->
        inventory.setItem(slot, Page.item(SpawnMenu.icon(clazz), listOf(plugin.texts.className(code, clazz)) + languages.lines(code, Message.SPAWN_MENU_CLASS)))
      }
      SpawnMenu.footer(plugin, player, inventory)
      player.openInventory(inventory)
    }
  }
}
