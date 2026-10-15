package ru.mobosses.events

import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.managers.BossBarManager
import ru.mobosses.managers.BossManager

class WorldEventHandler(
  private val plugin: PluginUnit,
  private val bossManager: BossManager,
  private val bossBarManager: BossBarManager,
) : Listener {

  private val general = plugin.configuration[GeneralConfigurationModel::class.java]

  @EventHandler
  fun onCreatureSpawn(event: CreatureSpawnEvent) {
    val allowedReasons = general.spawn.allowedSpawnReasons
    if (event.spawnReason.name in allowedReasons) {
      plugin.logger.debug { "Processing creature spawn: ${event.entity.type} at ${event.location.world?.name} (${event.spawnReason})" }
      bossManager.handleMonsterSpawn(event.entity)
    }
  }

  @EventHandler
  fun onPlayerQuit(event: PlayerQuitEvent) {
    plugin.logger.debug { "Hiding boss bars for quitting player ${event.player.name}" }
    bossBarManager.hideBossBarFromPlayer(event.player)
  }

  @EventHandler
  fun onEntitiesUnload(event: EntitiesUnloadEvent) {
    event.entities.forEach { entity ->
      if (entity is LivingEntity && bossManager.isBoss(entity)) {
        val boss = bossManager.getBoss(entity)
        if (boss != null) {
          plugin.logger.debug { "Boss ${boss.bossType.name} despawned (unloaded), removing boss bar and boss data" }
          // removeBoss уже вызывает removeBossBar внутри себя
          bossManager.removeBoss(entity)
        }
      }
    }
  }

  @EventHandler
  fun onEntityRemove(event: EntityRemoveEvent) {
    val entity = event.entity
    if (entity is LivingEntity && bossManager.isBoss(entity)) {
      val boss = bossManager.getBoss(entity)
      if (boss != null) {
        // removeBoss уже вызывает removeBossBar внутри себя
        bossManager.removeBoss(entity)
        plugin.logger.debug { "Boss ${boss.bossType.name} successfully removed from world" }
      }
    }
  }
}