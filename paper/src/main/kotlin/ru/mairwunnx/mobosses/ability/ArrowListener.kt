package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.Entity
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * What abilities throw hits players only, and the pets and golems that hit the boss lately: arrows,
 * tridents, fireballs, and wind charges fly through everyone else, potions splash only them, and a
 * boss's fangs bite only them. Other pets, villagers, horses, other mobs, and the boss's own minions
 * stay out of it, and no minion turns on its boss.
 */
class ArrowListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onHit(event: ProjectileHitEvent) {
    val hit = event.hitEntity ?: return
    if (fromAbility(event.entity) && !hittable(hit, shooter(event.entity))) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onSplash(event: PotionSplashEvent) {
    if (!fromAbility(event.entity)) return
    event.affectedEntities.filterNot { hittable(it, shooter(event.entity)) }.forEach { event.setIntensity(it, 0.0) }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onFangs(event: EntityDamageByEntityEvent) {
    val fangs = event.damager as? EvokerFangs ?: return
    val owner = fangs.owner ?: return
    if (plugin.bosses.get(owner) != null && !hittable(event.entity, owner)) event.isCancelled = true
  }

  private fun fromAbility(entity: Entity) = entity.persistentDataContainer.has(plugin.keys.arrow, PersistentDataType.BOOLEAN)

  /** Players, and the pets and golems that hit [boss] lately. */
  private fun hittable(entity: Entity, boss: Entity?) =
    entity is Player && Targets.canHit(entity, plugin.guards) || boss != null && Targets.isAggressor(entity, boss.uniqueId, plugin.keys.aggressor)

  private fun shooter(projectile: Projectile) = projectile.shooter as? Entity
}
