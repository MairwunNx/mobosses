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
import ru.mairwunnx.mobosses.lang.Message

/** The first step of spawning a boss: its mob, 45 to a page, by the language's names. */
class SpawnTypePage private constructor(private val plugin: MoBossesPlugin, private val page: Int, private val pages: Int, private val types: List<Catalog.BossType>) : Page {
  private lateinit var inventory: Inventory

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      SpawnMenu.PREVIOUS -> if (page > 0) open(plugin, player, page - 1) else return
      SpawnMenu.NEXT -> if (page < pages - 1) open(plugin, player, page + 1) else return
      SpawnMenu.BACK -> Menu.open(plugin, player)
      SpawnMenu.CLOSE -> player.closeInventory()
      in types.indices -> SpawnClassPage.open(plugin, player, types[slot])
      else -> return
    }
    sound(player)
  }

  companion object {
    private const val PER_PAGE = 45

    fun open(plugin: MoBossesPlugin, player: Player, page: Int) {
      val code = plugin.texts.languageOf(player)
      val languages = plugin.languages
      val all = plugin.catalog.types.values.filter { it.spec.enabled }.sortedBy { it.key }
      val pages = ((all.size + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)
      val current = page.coerceIn(0, pages - 1)
      val shown = all.drop(current * PER_PAGE).take(PER_PAGE)
      val counts = arrayOf(Placeholder.unparsed("page", (current + 1).toString()), Placeholder.unparsed("pages", pages.toString()))

      val list = SpawnTypePage(plugin, current, pages, shown)
      val inventory = Bukkit.createInventory(list, 54, languages.render(code, Message.SPAWN_MENU_TYPE_TITLE, *counts) ?: Component.text("Mo’Bosses"))
      list.inventory = inventory
      shown.forEachIndexed { slot, type ->
        val egg = Bukkit.getItemFactory().getSpawnEgg(type.entity) ?: Material.ZOMBIE_SPAWN_EGG
        inventory.setItem(slot, Page.item(egg, listOf(plugin.texts.bossName(code, type)) + languages.lines(code, Message.SPAWN_MENU_TYPE)))
      }
      if (current > 0) inventory.setItem(SpawnMenu.PREVIOUS, Page.item(Material.ARROW, languages.lines(code, Message.MENU_PREVIOUS)))
      if (current < pages - 1) inventory.setItem(SpawnMenu.NEXT, Page.item(Material.ARROW, languages.lines(code, Message.MENU_NEXT)))
      SpawnMenu.footer(plugin, player, inventory)
      player.openInventory(inventory)
    }
  }
}
