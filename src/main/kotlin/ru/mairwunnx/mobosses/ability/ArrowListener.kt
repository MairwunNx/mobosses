package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Ability arrows fly through everyone but players an ability may hit: pets, villagers, horses, other
 * mobs, and the boss's own minions stay out of it, and no minion turns on its boss over a stray arrow.
 */
class ArrowListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onHit(event: ProjectileHitEvent) {
    val arrow = event.entity as? Arrow ?: return
    val hit = event.hitEntity ?: return
    if (!arrow.persistentDataContainer.has(plugin.keys.arrow, PersistentDataType.BOOLEAN)) return
    if (hit !is Player || !Targets.canHit(hit, plugin.guards)) event.isCancelled = true
  }
}
