package ru.mairwunnx.mobosses.spawn

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.Bukkit
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Spawns that may become bosses, and bosses and minions coming and going with their chunks. The
 * add and remove events only read: the game forbids changing an entity while it's added.
 */
class SpawnListener(private val plugin: MoBossesPlugin) : Listener {
  val natural = NaturalSpawns(plugin)

  /** While `/mobosses spawn` makes a boss, its mob mustn't roll into another one. */
  var spawningByCommand = false

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onSpawn(event: CreatureSpawnEvent) {
    val entity = event.entity as? Mob ?: return
    // The game brings the dragon back with a reason of no use to anyone, so it rolls whatever the list says.
    val allowed = event.spawnReason in plugin.catalog.spawnReasons || entity is EnderDragon
    if (spawningByCommand || !allowed || plugin.decoys.isDecoy(entity)) return
    natural.spawned(entity, plugin.minions.summonerOf(entity), event.spawnReason)
  }

  @EventHandler
  fun onAdd(event: EntityAddToWorldEvent) {
    val entity = event.entity
    if (plugin.nameTags.isStray(entity)) return Bukkit.getScheduler().runTask(plugin, Runnable { entity.remove() }).let {}
    val boss = plugin.bosses.adopt(entity)
    if (boss != null) {
      Bukkit.getScheduler().runTask(plugin, Runnable { if (boss.entity.isValid) plugin.bosses.vanillaName(boss) })
      plugin.despawns.loaded(boss)
    } else {
      plugin.minions.adopt(entity)
    }
  }

  /** Dead, unloaded, or removed: the entity keeps its data, the plugin lets go of the rest. */
  @EventHandler
  fun onRemove(event: EntityRemoveFromWorldEvent) {
    plugin.bosses.get(event.entity)?.let(plugin.bosses::forget)
    plugin.minions.forget(event.entity)
    plugin.decoys.forget(event.entity)
  }
}
