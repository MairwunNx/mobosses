package ru.mairwunnx.mobosses.modcore.spawn

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.raid.Raider

/**
 * The game's spawn reasons by Bukkit's names, which `spawn.allowed_spawn_reasons` uses on every platform. A mob that
 * enters the world without one came from code, as Paper's `CUSTOM`; a structure's spawn that late is a portal's.
 */
object SpawnReasons {
  fun bukkit(entity: Entity, reason: EntitySpawnReason?): String = when (reason) {
    null -> "CUSTOM"
    EntitySpawnReason.NATURAL -> "NATURAL"
    EntitySpawnReason.CHUNK_GENERATION -> "CHUNK_GEN"
    EntitySpawnReason.SPAWNER -> "SPAWNER"
    EntitySpawnReason.STRUCTURE -> "NETHER_PORTAL"
    EntitySpawnReason.BREEDING -> "BREEDING"
    EntitySpawnReason.MOB_SUMMONED -> "SPELL"
    EntitySpawnReason.JOCKEY -> "JOCKEY"
    // Paper names the dragon's comeback after nothing in particular.
    EntitySpawnReason.EVENT -> if (entity is Raider) "RAID" else if (entity is EnderDragon) "DEFAULT" else "VILLAGE_INVASION"
    EntitySpawnReason.CONVERSION -> when (entity.type) {
      EntityTypes.DROWNED -> "DROWNED"
      EntityTypes.ZOMBIE_VILLAGER -> "INFECTION"
      EntityTypes.VILLAGER -> "CURED"
      EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.ZOGLIN -> "PIGLIN_ZOMBIFIED"
      else -> "METAMORPHOSIS"
    }
    EntitySpawnReason.REINFORCEMENT -> "REINFORCEMENTS"
    EntitySpawnReason.TRIGGERED -> if (entity is WitherBoss) "BUILD_WITHER" else "TRAP"
    EntitySpawnReason.BUCKET -> "BUCKET"
    EntitySpawnReason.SPAWN_ITEM_USE -> "SPAWNER_EGG"
    EntitySpawnReason.COMMAND -> "COMMAND"
    EntitySpawnReason.DISPENSER -> "DISPENSE_EGG"
    EntitySpawnReason.PATROL -> "PATROL"
    EntitySpawnReason.TRIAL_SPAWNER -> "TRIAL_SPAWNER"
    EntitySpawnReason.LOAD, EntitySpawnReason.DIMENSION_TRAVEL -> "DEFAULT"
  }

  fun of(entity: Entity): String = bukkit(entity, (entity as? SpawnReasonHolder)?.`mobosses$reason`())
}
