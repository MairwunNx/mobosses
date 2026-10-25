package ru.mairwunnx.mobosses.protect

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/** A temporary block drops nothing when broken, so shears don't farm a boss's cobwebs. */
class TemporaryBlockListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onBreak(event: BlockBreakEvent) {
    val temporary = plugin.blockChanges.temporary
    if (!temporary.contains(event.block)) return
    event.isDropItems = false
    temporary.forget(event.block)
  }
}
