package ru.mairwunnx.mobosses.spawn

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.Bukkit
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID

/**
 * Spawns that may become bosses, and bosses and minions coming and going with their chunks. The
 * add and remove events only read: the game forbids changing an entity while it's added. Mobs a
 * structure places as the world generates, like a bastion's brutes, come with no spawn event: they
 * count as `CHUNK_GEN` when they first enter the world.
 */
class SpawnListener(private val plugin: MoBossesPlugin) : Listener {
  val natural = NaturalSpawns(plugin)

  /** While `/mobosses spawn` makes a boss, its mob mustn't roll into another one. */
  var spawningByCommand = false

  /** Mobs a spawn event told of this tick, which [onAdd] tells from those the world generated with. */
  private val announced = HashSet<UUID>()

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onSpawn(event: CreatureSpawnEvent) {
    val entity = event.entity as? Mob ?: return
    val id = entity.uniqueId
    announced += id
    Bukkit.getScheduler().runTask(plugin, Runnable { announced -= id })
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
      if (!announced.remove(entity.uniqueId)) generated(entity)
    }
  }

  /** A mob that never lived a tick and came with no spawn event: a structure placed it as the world generated. */
  private fun generated(entity: Entity) {
    val mob = entity as? Mob ?: return
    val data = mob.persistentDataContainer
    if (mob.ticksLived > 0 || SpawnReason.CHUNK_GEN !in plugin.catalog.spawnReasons || data.has(plugin.keys.pending) || data.has(plugin.keys.summoner)) return
    Bukkit.getScheduler().runTask(plugin, Runnable { if (mob.isValid && plugin.bosses.get(mob) == null) natural.spawned(mob, null, SpawnReason.CHUNK_GEN) })
  }

  /** Dead, unloaded, or removed: the entity keeps its data, the plugin lets go of the rest. */
  @EventHandler
  fun onRemove(event: EntityRemoveFromWorldEvent) {
    plugin.bosses.get(event.entity)?.let(plugin.bosses::forget)
    plugin.minions.forget(event.entity)
    plugin.decoys.forget(event.entity)
  }
}
