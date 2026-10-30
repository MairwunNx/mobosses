package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** A page of the `/mobosses` menu: it handles clicks on its own slots; nothing moves in or out. */
interface Page : InventoryHolder {
  fun click(player: Player, slot: Int, click: ClickType)

  /** A button's click, for [player] alone. Pages play it only when a button did something; filler stays silent. */
  fun sound(player: Player) = player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f)

  companion object {
    /** An item named by the first of [lines], with the rest for its lore. */
    fun item(material: Material, lines: List<Component>) = ItemStack(material).also { name(it, lines.firstOrNull(), lines.drop(1)) }

    fun name(item: ItemStack, name: Component?, lore: List<Component>) = item.editMeta { meta ->
      fun plain(text: Component) = text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
      name?.let { meta.displayName(plain(it)) }
      if (lore.isNotEmpty()) meta.lore(lore.map(::plain))
    }
  }
}
