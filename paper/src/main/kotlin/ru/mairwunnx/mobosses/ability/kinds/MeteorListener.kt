package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin

/** Meteors break up on impact instead of turning into magma blocks. */
class MeteorListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  fun onLand(event: EntityChangeBlockEvent) {
    val block = event.entity as? FallingBlock ?: return
    if (block.persistentDataContainer.has(plugin.keys.meteor, PersistentDataType.BOOLEAN)) event.isCancelled = true
  }
}
