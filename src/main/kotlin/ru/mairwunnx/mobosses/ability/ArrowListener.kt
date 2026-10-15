package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.Entity
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * What abilities throw hits players only: arrows, tridents, fireballs, and wind charges fly through
 * everyone else, potions splash only players, and a boss's fangs bite only players. Pets, villagers,
 * horses, other mobs, and the boss's own minions stay out of it, and no minion turns on its boss.
 */
class ArrowListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onHit(event: ProjectileHitEvent) {
    val hit = event.hitEntity ?: return
    if (fromAbility(event.entity) && !hittable(hit)) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onSplash(event: PotionSplashEvent) {
    if (!fromAbility(event.entity)) return
    event.affectedEntities.filterNot(::hittable).forEach { event.setIntensity(it, 0.0) }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onFangs(event: EntityDamageByEntityEvent) {
    val fangs = event.damager as? EvokerFangs ?: return
    val owner = fangs.owner ?: return
    if (plugin.bosses.get(owner) != null && !hittable(event.entity)) event.isCancelled = true
  }

  private fun fromAbility(entity: Entity) = entity.persistentDataContainer.has(plugin.keys.arrow, PersistentDataType.BOOLEAN)

  private fun hittable(entity: Entity) = entity is Player && Targets.canHit(entity, plugin.guards)
}
