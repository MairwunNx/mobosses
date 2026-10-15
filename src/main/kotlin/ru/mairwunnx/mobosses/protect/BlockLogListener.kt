package ru.mairwunnx.mobosses.protect

import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Blocks bosses and minions change on their own, like an enderman picking one up, a zombie breaking a
 * door, or a creeper blowing up, go to CoreProtect under the bosses' name too, so one rollback undoes
 * a whole fight.
 */
class BlockLogListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onChangeBlock(event: EntityChangeBlockEvent) {
    if (!fromBosses(event.entity)) return
    val block = event.block
    if (!block.type.isAir) plugin.blockLog.removed(block.location, block.blockData)
    if (!event.blockData.material.isAir) plugin.blockLog.placed(block.location, event.blockData)
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onExplode(event: EntityExplodeEvent) {
    if (!fromBosses(event.entity)) return
    event.blockList().forEach { plugin.blockLog.removed(it.location, it.blockData) }
  }

  private fun fromBosses(entity: Entity) = plugin.bosses.get(entity) != null || plugin.minions.summonerOf(entity) != null
}
