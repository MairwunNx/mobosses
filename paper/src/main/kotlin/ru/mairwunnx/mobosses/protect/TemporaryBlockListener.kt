package ru.mairwunnx.mobosses.protect

import io.papermc.paper.event.entity.EntityInsideBlockEvent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * A temporary block drops nothing when broken, so shears don't farm a boss's cobwebs. Bosses and their minions
 * pass through what abilities leave: their own cobwebs don't hold them, nor does their fire burn them.
 */
class TemporaryBlockListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onBreak(event: BlockBreakEvent) {
    val temporary = plugin.blockChanges.temporary
    if (!temporary.contains(event.block)) return
    event.isDropItems = false
    temporary.forget(event.block)
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onInside(event: EntityInsideBlockEvent) {
    // The game asks for every block an entity stands in: the cheap test first.
    val type = event.block.type
    if (type != Material.COBWEB && type != Material.FIRE && type != Material.SOUL_FIRE) return
    if (!plugin.blockChanges.temporary.contains(event.block)) return
    if (plugin.bosses.get(event.entity) != null || plugin.minions.typeOf(event.entity) != null) event.isCancelled = true
  }
}
