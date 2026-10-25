package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.kinds.Obelisk

/**
 * What abilities leave in the world: obelisks, which only players' hits break and which never
 * explode, and a boss's copies, which drop only what their mob would and 1 experience.
 */
class AbilityEntities(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onObeliskHurt(event: EntityDamageEvent) {
    val crystal = event.entity as? EnderCrystal ?: return
    if (!isObelisk(crystal)) return
    event.isCancelled = true
    val by = (event as? EntityDamageByEntityEvent)?.damager ?: return
    val player = by as? Player ?: (by as? Projectile)?.shooter as? Player ?: return
    if (Targets.canHit(player, plugin.guards)) Obelisk.hit(plugin, crystal, player)
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onObeliskBlast(event: ExplosionPrimeEvent) {
    if (event.entity is EnderCrystal && isObelisk(event.entity)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGH)
  fun onCopyDeath(event: EntityDeathEvent) {
    if (!plugin.decoys.isDecoy(event.entity)) return
    event.droppedExp = 1
    plugin.decoys.forget(event.entity)
  }

  private fun isObelisk(entity: org.bukkit.entity.Entity) = entity.persistentDataContainer.has(plugin.keys.obelisk, PersistentDataType.STRING)
}
