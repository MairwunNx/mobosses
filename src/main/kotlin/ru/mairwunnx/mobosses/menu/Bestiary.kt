package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.progress.BestiaryEntry

/**
 * A player's bestiary: every boss type, with their kills and best level in each class. A type they
 * never beat shows as a gray “???”. It counts from 2.4 on, when Mo’Bosses began to keep it.
 */
class Bestiary private constructor(private val plugin: MoBossesPlugin) : Page {
  private lateinit var inventory: Inventory

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      BACK -> Menu.open(plugin, player)
      CLOSE -> player.closeInventory()
    }
  }

  companion object {
    private const val BACK = 45
    private const val PROGRESS = 49
    private const val CLOSE = 53

    fun open(plugin: MoBossesPlugin, player: Player) {
      plugin.progression.bestiary(player.uniqueId).whenComplete { entries, error ->
        Bukkit.getScheduler().runTask(plugin, Runnable {
          if (!player.isOnline) return@Runnable
          if (error != null) return@Runnable plugin.texts.send(player, Message.NO_DATABASE)
          build(plugin, player, entries)
        })
      }
    }

    private fun build(plugin: MoBossesPlugin, player: Player, entries: List<BestiaryEntry>) {
      val languages = plugin.languages
      val code = plugin.texts.languageOf(player)
      val page = Bestiary(plugin)
      val inventory = Bukkit.createInventory(page, 54, languages.render(code, Message.BESTIARY_TITLE) ?: Component.text("Mo’Bosses"))
      page.inventory = inventory

      val classes = plugin.catalog.classes.all
      val types = plugin.catalog.types.values.filter { it.spec.enabled }.take(BACK)
      val byType = entries.groupBy { it.type }
      types.forEachIndexed { slot, type ->
        val known = byType[type.key].orEmpty().associateBy { it.clazz }
        inventory.setItem(slot, if (known.isEmpty()) {
          Page.item(Material.GRAY_DYE, languages.lines(code, Message.BESTIARY_UNKNOWN))
        } else {
          val egg = Bukkit.getItemFactory().getSpawnEgg(type.entity) ?: Material.ZOMBIE_SPAWN_EGG
          val lore = classes.mapNotNull { clazz ->
            val entry = known[clazz.key]
            val name = Placeholder.component("class", plugin.texts.className(code, clazz))
            if (entry == null) languages.render(code, Message.BESTIARY_MISSING, name)
            else languages.render(code, Message.BESTIARY_LINE, name, Placeholder.unparsed("kills", entry.kills.toString()), Placeholder.unparsed("level", entry.bestLevel.toString()))
          }
          Page.item(egg, listOf(plugin.texts.bossName(code, type)) + lore)
        })
      }
      val opened = entries.count { entry -> types.any { it.key == entry.type } && classes.any { it.key == entry.clazz } }
      inventory.setItem(PROGRESS, Page.item(Material.BOOK, languages.lines(code, Message.BESTIARY_PROGRESS,
        Placeholder.unparsed("opened", opened.toString()), Placeholder.unparsed("total", (types.size * classes.size).toString()))))
      inventory.setItem(BACK, Page.item(Material.ARROW, languages.lines(code, Message.MENU_BACK)))
      inventory.setItem(CLOSE, Page.item(Material.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
      player.openInventory(inventory)
    }
  }
}
