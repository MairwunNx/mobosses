package ru.mairwunnx.mobosses.modcore.spawn

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.modcore.boss.Keys

/**
 * Spawns that may become bosses, and bosses and minions coming and going with their chunks, as Paper's
 * `SpawnListener`. Nothing is removed while the game adds an entity: that waits for the next tick.
 */
class Spawns(private val mod: MoBosses) {
  val natural = NaturalSpawns(mod)

  /** While `/mobosses spawn` makes a boss, its mob mustn't roll into another one. */
  var spawningByCommand = false

  /** A mob new to the world, with its gear on. */
  fun spawned(entity: Entity) {
    val mob = entity as? Mob ?: return
    val reason = SpawnReasons.of(mob)
    // The game brings the dragon back with a reason of no use to anyone, so it rolls whatever the list says.
    val allowed = reason in mod.catalog.spawnReasons || mob is EnderDragon
    if (spawningByCommand || !allowed || mod.decoys.isDecoy(mob) || mod.bosses.get(mob) != null) return
    natural.spawned(mob, mod.minions.summonerOf(mob), reason)
  }

  /**
   * A mob a structure places as the world generates, like a bastion's brutes, before it enters the world: no spawn tells
   * of it, so it waits to be seen as `CHUNK_GEN`. Off the server thread, so it only marks the mob.
   */
  fun generated(entity: Entity) {
    val mob = entity as? Mob ?: return
    val catalog = mod.catalog
    if ("CHUNK_GEN" !in catalog.spawnReasons || !catalog.settings.spawn.onSight.enabled) return
    if (catalog.types[mob.type]?.spec?.enabled == true) Data(mob)[Keys.PENDING] = "CHUNK_GEN"
  }

  fun loaded(entity: Entity) {
    val boss = mod.bosses.adopt(entity)
    if (boss != null) {
      mod.scheduler.later(1) { if (boss.entity.isAlive) mod.bosses.vanillaName(boss) }
      mod.despawns.loaded(boss)
    } else {
      mod.minions.adopt(entity)
    }
  }

  /** Dead, unloaded, or removed: the entity keeps its data, Mo’Bosses lets go of the rest. */
  fun unloaded(entity: Entity) {
    mod.bosses.get(entity)?.let(mod.bosses::forget)
    mod.minions.forget(entity)
    mod.decoys.forget(entity)
    mod.deaths.forget(entity)
  }
}
