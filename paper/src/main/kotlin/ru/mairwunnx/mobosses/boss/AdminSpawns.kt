package ru.mairwunnx.mobosses.boss

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.config.BossClass

/**
 * Bosses an admin places, by `/mobosses spawn` or the menu: pinned, so they don't despawn, and with [abilities]
 * the only ones they cast, if given.
 */
class AdminSpawns(private val plugin: MoBossesPlugin) {
  sealed interface Result {
    data class Spawned(val boss: Boss) : Result
    data class Failed(val reason: String) : Result
  }

  fun spawn(type: Catalog.BossType, clazz: BossClass, level: Int, at: Location, abilities: Set<String>? = null): Result {
    if (!plugin.catalog.settings.protection.adminSpawnBypass && !plugin.guards.canSpawnBoss(at)) {
      return Result.Failed(plugin.guards.regionsAt(at).joinToString().ifEmpty { "protected" })
    }
    // The console may name a spot nobody stands near; since 26.1 even the spawn chunks load only on demand.
    at.world.getChunkAt(at)
    val entity = try {
      plugin.spawns.spawningByCommand = true
      at.world.spawn(at, type.entity.entityClass!!) as? LivingEntity
    } catch (e: IllegalArgumentException) {
      return Result.Failed(e.message ?: "?")
    } finally {
      plugin.spawns.spawningByCommand = false
    }
    if (entity == null || !entity.isValid) return Result.Failed("the spawn was cancelled")
    val boss = plugin.bosses.make(entity, type, clazz, level).also(plugin.bosses::vanillaName)
    boss.pinned = true
    boss.abilities = abilities
    return Result.Spawned(boss)
  }
}
