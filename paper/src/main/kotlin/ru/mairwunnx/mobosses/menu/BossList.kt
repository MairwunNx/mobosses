package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID
import kotlin.math.roundToInt

/**
 * For admins: the bosses in loaded chunks, nearest first, then other worlds, 45 to a page. A click
 * shows a boss's details in the chat, Shift and a click takes the admin to it, and a right click asks
 * once and then removes it, without loot. Bosses in unloaded chunks don't show: the server doesn't
 * know them until their chunk loads.
 */
class BossList private constructor(private val plugin: MoBossesPlugin, private val page: Int, private val pages: Int, private val slots: List<UUID>) : Page {
  private lateinit var inventory: Inventory
  /** The slot whose boss a right click would remove now. */
  private var armed: Int? = null

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      PREVIOUS -> if (page > 0) open(plugin, player, page - 1)
      NEXT -> if (page < pages - 1) open(plugin, player, page + 1)
      BACK -> Menu.open(plugin, player)
      CLOSE -> player.closeInventory()
      in slots.indices -> {
        val boss = plugin.bosses.get(slots[slot])?.takeIf { it.entity.isValid }
          ?: return plugin.texts.send(player, Message.BOSS_LIST_GONE).also { open(plugin, player, page) }
        when {
          click.isRightClick && armed == slot -> {
            plugin.bosses.remove(boss)
            plugin.texts.send(player, Message.BOSS_LIST_REMOVED, *plugin.texts.boss(plugin.texts.languageOf(player), boss))
            open(plugin, player, page)
          }
          click.isRightClick -> {
            armed = slot
            inventory.setItem(slot, Page.item(Material.BARRIER, plugin.languages.lines(plugin.texts.languageOf(player), Message.BOSS_LIST_CONFIRM)))
          }
          click.isShiftClick -> {
            player.closeInventory()
            player.teleport(boss.entity.location)
            plugin.texts.send(player, Message.BOSS_LIST_TELEPORTED, *plugin.texts.boss(plugin.texts.languageOf(player), boss))
          }
          else -> plugin.texts.send(player, Message.BOSS_LIST_INFO, *details(plugin, player, boss))
        }
      }
    }
  }

  companion object {
    const val PERMISSION = "mobosses.bosses"
    private const val PER_PAGE = 45
    private const val PREVIOUS = 45
    private const val BACK = 47
    private const val INDEX = 49
    private const val CLOSE = 51
    private const val NEXT = 53

    fun open(plugin: MoBossesPlugin, player: Player, page: Int) {
      if (!player.hasPermission(PERMISSION)) return plugin.texts.send(player, Message.NO_PERMISSION)
      val here = player.location
      // Nearest first in the admin's world; other worlds after, by name.
      val bosses = plugin.bosses.all.filter { it.entity.isValid }.sortedWith(
        compareBy<Boss> { it.entity.world != here.world }.thenBy { if (it.entity.world == here.world) it.entity.location.distanceSquared(here) else 0.0 }.thenBy { it.entity.world.name },
      )
      val pages = ((bosses.size + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)
      val current = page.coerceIn(0, pages - 1)
      val shown = bosses.drop(current * PER_PAGE).take(PER_PAGE)
      val code = plugin.texts.languageOf(player)
      val languages = plugin.languages

      val list = BossList(plugin, current, pages, shown.map { it.uuid })
      val title = languages.render(code, Message.BOSS_LIST_TITLE, Placeholder.unparsed("page", (current + 1).toString()), Placeholder.unparsed("pages", pages.toString()),
        Placeholder.unparsed("count", bosses.size.toString())) ?: Component.text("Mo’Bosses")
      val inventory = Bukkit.createInventory(list, 54, title)
      list.inventory = inventory
      shown.forEachIndexed { slot, boss ->
        val egg = Bukkit.getItemFactory().getSpawnEgg(boss.entity.type) ?: Material.ZOMBIE_SPAWN_EGG
        val name = languages.render(code, Message.BOSS_TAG, *plugin.texts.boss(code, boss))
        inventory.setItem(slot, Page.item(egg, listOfNotNull(name) + languages.lines(code, Message.BOSS_LIST_LORE, *details(plugin, player, boss))))
      }
      if (bosses.isEmpty()) inventory.setItem(22, Page.item(Material.STRUCTURE_VOID, languages.lines(code, Message.BOSS_LIST_EMPTY)))
      if (current > 0) inventory.setItem(PREVIOUS, Page.item(Material.ARROW, languages.lines(code, Message.MENU_PREVIOUS)))
      if (current < pages - 1) inventory.setItem(NEXT, Page.item(Material.ARROW, languages.lines(code, Message.MENU_NEXT)))
      inventory.setItem(BACK, Page.item(Material.BOOK, languages.lines(code, Message.MENU_BACK)))
      inventory.setItem(INDEX, Page.item(Material.PAPER, languages.lines(code, Message.BOSS_LIST_TITLE, Placeholder.unparsed("page", (current + 1).toString()),
        Placeholder.unparsed("pages", pages.toString()), Placeholder.unparsed("count", bosses.size.toString()))))
      inventory.setItem(CLOSE, Page.item(Material.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
      player.openInventory(inventory)
    }

    /** `<health>`, `<max>`, `<world>`, `<x>`, `<y>`, `<z>`, `<distance>`, `<fighters>`, `<phase>`, `<spawner>`, and the boss's own tags. */
    private fun details(plugin: MoBossesPlugin, player: Player, boss: Boss): Array<TagResolver> {
      val code = plugin.texts.languageOf(player)
      val at = boss.entity.location
      val distance = if (at.world == player.world) at.distance(player.location).roundToInt().toString() else "—"
      return arrayOf(
        *plugin.texts.boss(code, boss),
        Placeholder.unparsed("health", boss.entity.health.roundToInt().toString()),
        Placeholder.unparsed("max", (boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0).roundToInt().toString()),
        Placeholder.unparsed("world", at.world.name),
        Placeholder.unparsed("x", at.blockX.toString()), Placeholder.unparsed("y", at.blockY.toString()), Placeholder.unparsed("z", at.blockZ.toString()),
        Placeholder.unparsed("distance", distance),
        Placeholder.unparsed("fighters", boss.damage.size.toString()),
        Placeholder.component("phase", plugin.languages.render(code, if (boss.phaseTwo) Message.WORD_YES else Message.WORD_NO) ?: Component.empty()),
        Placeholder.component("spawner", plugin.languages.render(code, if (boss.fromSpawner) Message.WORD_YES else Message.WORD_NO) ?: Component.empty()),
      )
    }
  }
}
