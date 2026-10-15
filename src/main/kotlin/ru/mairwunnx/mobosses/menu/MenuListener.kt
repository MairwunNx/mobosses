package ru.mairwunnx.mobosses.menu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/** Clicks in the menu run its buttons; nothing moves in or out of it. */
class MenuListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onClick(event: InventoryClickEvent) {
    val menu = event.inventory.holder as? Menu ?: return
    event.isCancelled = true
    val player = event.whoClicked as? Player ?: return
    if (event.clickedInventory?.holder !== menu) return
    when (menu.actions[event.rawSlot]) {
      Menu.Action.RESET -> if (!menu.resetArmed) menu.armReset() else {
        player.closeInventory()
        player.performCommand("mobosses reset confirm")
      }
      Menu.Action.RELOAD -> {
        player.closeInventory()
        player.performCommand("mobosses reload")
      }
      Menu.Action.KILLALL -> {
        player.closeInventory()
        player.performCommand("mobosses killall")
      }
      Menu.Action.CLOSE -> player.closeInventory()
      null -> {}
    }
  }

  @EventHandler(ignoreCancelled = true)
  fun onDrag(event: InventoryDragEvent) {
    if (event.inventory.holder is Menu) event.isCancelled = true
  }
}
