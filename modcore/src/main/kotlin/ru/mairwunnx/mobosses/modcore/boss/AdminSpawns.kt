package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Bosses an admin places, by `/mobosses spawn` or the menu: pinned, so they don't despawn, and with abilities
 * the only ones they cast, if given. Paper's `AdminSpawns`.
 */
class AdminSpawns(private val mod: MoBosses) {
  sealed interface Result {
    data class Spawned(val boss: Boss) : Result
    data class Failed(val reason: String) : Result
  }

  fun spawn(type: Catalog.BossType, clazz: BossClass, level: Int, world: ServerLevel, at: Vec3, abilities: Set<String>? = null): Result {
    val pos = BlockPos.containing(at)
    if (!mod.catalog.settings.protection.adminSpawnBypass && !mod.guards.canSpawnBoss(world, pos)) return Result.Failed("protected")
    val entity = try {
      mod.spawns.spawningByCommand = true
      type.entity.spawn(world, pos, EntitySpawnReason.COMMAND) as? LivingEntity
    } finally {
      mod.spawns.spawningByCommand = false
    }
    if (entity == null || !entity.isAlive) return Result.Failed("the spawn was cancelled")
    entity.snapTo(at.x, at.y, at.z, entity.yRot, entity.xRot)
    val boss = mod.bosses.make(entity, type, clazz, level).also(mod.bosses::vanillaName)
    boss.pinned = true
    boss.abilities = abilities
    return Result.Spawned(boss)
  }
}
