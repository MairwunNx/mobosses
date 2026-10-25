package ru.mairwunnx.mobosses.menu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Clicks in the menu's pages run their buttons; nothing moves in or out of them. */
class MenuListener : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onClick(event: InventoryClickEvent) {
    val page = event.inventory.holder as? Page ?: return
    event.isCancelled = true
    val player = event.whoClicked as? Player ?: return
    if (event.clickedInventory?.holder !== page) return
    page.click(player, event.rawSlot, event.click)
  }

  @EventHandler(ignoreCancelled = true)
  fun onDrag(event: InventoryDragEvent) {
    if (event.inventory.holder is Page) event.isCancelled = true
  }
}
