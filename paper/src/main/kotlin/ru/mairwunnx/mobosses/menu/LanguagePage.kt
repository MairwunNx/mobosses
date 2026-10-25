package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Message

/**
 * The languages Mo’Bosses speaks, each by its own name, for the player to pick one, or Automatic to
 * have the plugin pick by country and game language. The current one glints; a pick goes back to the menu.
 */
class LanguagePage private constructor(private val plugin: MoBossesPlugin, private val codes: List<String>) : Page {
  private lateinit var inventory: Inventory

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      AUTO -> picked(player, null)
      BACK -> Menu.open(plugin, player)
      CLOSE -> player.closeInventory()
      in SLOTS -> codes.getOrNull(SLOTS.indexOf(slot))?.let { picked(player, it) }
    }
  }

  /** Back to the menu, already in the new language. */
  private fun picked(player: Player, code: String?) {
    plugin.playerLanguages.choose(player, code)
    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f)
    Menu.open(plugin, player)
  }

  companion object {
    private const val AUTO = 10
    private const val BACK = 18
    private const val CLOSE = 26
    private val SLOTS = listOf(12, 13, 14, 15, 16, 20, 21, 22, 23, 24, 25)

    fun open(plugin: MoBossesPlugin, player: Player) {
      val languages = plugin.languages
      val code = plugin.texts.languageOf(player)
      val chosen = plugin.playerLanguages.chosen(player)
      val page = LanguagePage(plugin, plugin.playerLanguages.codes().take(SLOTS.size))
      val inventory = Bukkit.createInventory(page, 27, languages.render(code, Message.LANGUAGE_TITLE) ?: Component.text("Mo’Bosses"))
      page.inventory = inventory

      val current = languages.render(code, Message.LANGUAGE_CURRENT)
      val pick = languages.render(code, Message.LANGUAGE_PICK)
      fun state(on: Boolean) = listOfNotNull(Component.empty(), if (on) current else pick)

      val auto = languages.lines(code, Message.LANGUAGE_AUTOMATIC)
      inventory.setItem(AUTO, Page.item(Material.COMPASS, auto + state(chosen == null)).glint(chosen == null))
      page.codes.forEachIndexed { i, language ->
        val name = Component.text(LanguageChoices.nativeName(language), NamedTextColor.YELLOW)
        val lore = listOf(Component.text(language, NamedTextColor.DARK_GRAY)) + state(language == chosen)
        inventory.setItem(SLOTS[i], Page.item(Material.PAPER, listOf(name) + lore).glint(language == chosen))
      }
      inventory.setItem(BACK, Page.item(Material.ARROW, languages.lines(code, Message.MENU_BACK)))
      inventory.setItem(CLOSE, Page.item(Material.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
      player.openInventory(inventory)
    }

    private fun ItemStack.glint(on: Boolean) = apply { if (on) editMeta { it.setEnchantmentGlintOverride(true) } }
  }
}
