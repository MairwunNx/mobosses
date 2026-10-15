package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.progress.BossCount
import ru.mairwunnx.mobosses.progress.Hunter

/**
 * The `/mobosses` menu: your card, the best hunters, the bosses by class, a button to start over, and,
 * for admins, reload and remove-all buttons. It opens once the database has answered, in the player's
 * language.
 */
class Menu private constructor(val actions: Map<Int, Action>, private val confirmReset: ItemStack?) : InventoryHolder {
  enum class Action { RESET, RELOAD, KILLALL, CLOSE }

  private lateinit var inventory: Inventory

  /** The reset button asks once more: the first click only turns it into a warning. */
  var resetArmed = false; private set

  override fun getInventory() = inventory

  fun armReset() {
    resetArmed = true
    inventory.setItem(RESET, confirmReset)
  }

  companion object {
    private const val RESET = 18
    private const val YOU = 11
    private const val TOP = 13
    private const val BOSSES = 15
    private const val RELOAD = 21
    private const val KILLALL = 23
    private const val CLOSE = 26

    fun open(plugin: MoBossesPlugin, player: Player) {
      val stats = plugin.menuStats
      stats.card(player).thenCombine(stats.top()) { card, top -> card to top }.thenCombine(stats.counts()) { (card, top), counts -> Triple(card, top, counts) }
        .whenComplete { data, error ->
          Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            if (error != null) return@Runnable plugin.log.warn("Can't open the menu for ${player.name}: ${error.message}")
            val (card, top, counts) = data
            build(plugin, player, card, top, counts)
          })
        }
    }

    private fun build(plugin: MoBossesPlugin, player: Player, card: Stats.Card, top: List<Hunter>, counts: List<BossCount>) {
      val languages = plugin.languages
      val code = plugin.texts.languageOf(player)
      val admin = player.hasPermission("mobosses.reload") || player.hasPermission("mobosses.killall")
      val canReset = player.hasPermission("mobosses.reset")
      val actions = buildMap {
        if (canReset) put(RESET, Action.RESET)
        if (player.hasPermission("mobosses.reload")) put(RELOAD, Action.RELOAD)
        if (player.hasPermission("mobosses.killall")) put(KILLALL, Action.KILLALL)
        put(CLOSE, Action.CLOSE)
      }
      val menu = Menu(actions, if (canReset) item(Material.TNT, languages.lines(code, Message.MENU_RESET_CONFIRM)) else null)
      val inventory = Bukkit.createInventory(menu, 27, languages.render(code, Message.MENU_TITLE) ?: Component.text("Mo’Bosses"))
      menu.inventory = inventory

      inventory.setItem(YOU, ItemStack(Material.PLAYER_HEAD).apply {
        editMeta(SkullMeta::class.java) { it.owningPlayer = player }
        name(this, languages.render(code, Message.MENU_YOU, *plugin.menuStats.resolvers(code, card)), languages.lines(code, Message.INFO, *plugin.menuStats.resolvers(code, card)).drop(1))
      })
      inventory.setItem(TOP, ItemStack(Material.GOLDEN_HELMET).apply {
        val lines = top.mapIndexed { i, hunter ->
          languages.render(code, Message.TOP_LINE, Placeholder.unparsed("rank", (i + 1).toString()), Placeholder.unparsed("player", plugin.menuStats.name(hunter)),
            Placeholder.unparsed("level", plugin.menuStats.level(hunter.progress).toString()), Placeholder.unparsed("kills", hunter.progress.kills.toString()))
        }.filterNotNull().ifEmpty { listOfNotNull(languages.render(code, Message.TOP_EMPTY)) }
        name(this, languages.render(code, Message.MENU_TOP), lines)
      })
      inventory.setItem(BOSSES, ItemStack(Material.ZOMBIE_HEAD).apply {
        val byClass = counts.groupBy { it.clazz }
        val lines = plugin.catalog.classes.all.mapNotNull { clazz ->
          val rows = byClass[clazz.key].orEmpty()
          languages.render(code, Message.MENU_BOSSES_LINE, Placeholder.component("class", plugin.texts.className(code, clazz)),
            Placeholder.unparsed("spawned", rows.sumOf { it.spawned }.toString()), Placeholder.unparsed("killed", rows.sumOf { it.killed }.toString()))
        }
        name(this, languages.render(code, Message.MENU_BOSSES, Placeholder.unparsed("alive", plugin.bosses.all.size.toString())), lines)
      })
      if (canReset) inventory.setItem(RESET, item(Material.RECOVERY_COMPASS, languages.lines(code, Message.MENU_RESET)))
      if (admin) {
        if (Action.RELOAD in actions.values) inventory.setItem(RELOAD, ItemStack(Material.REPEATER).apply { name(this, languages.render(code, Message.MENU_RELOAD), emptyList()) })
        if (Action.KILLALL in actions.values) inventory.setItem(KILLALL, ItemStack(Material.BARRIER).apply { name(this, languages.render(code, Message.MENU_KILLALL), emptyList()) })
      }
      inventory.setItem(CLOSE, ItemStack(Material.OAK_DOOR).apply { name(this, languages.render(code, Message.MENU_CLOSE), emptyList()) })
      player.openInventory(inventory)
    }

    /** An item named by the first line of a text, with the rest for its lore. */
    private fun item(material: Material, lines: List<Component>) = ItemStack(material).apply { name(this, lines.firstOrNull(), lines.drop(1)) }

    private fun name(item: ItemStack, name: Component?, lore: List<Component>) = item.editMeta { meta ->
      fun plain(text: Component) = text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
      name?.let { meta.displayName(plain(it)) }
      if (lore.isNotEmpty()) meta.lore(lore.map(::plain))
    }
  }
}
