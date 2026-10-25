package ru.mairwunnx.mobosses.modcore.spawn

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.random.Random

/**
 * Keeps bosses from piling up, as on Paper: no new one while `max_nearby` are close already, and after a boss spawns
 * for a group, its players wait `player_cooldown_seconds`, stretched at random so a group's bosses don't come on a
 * beat. The wait lives on each player, so a relog or a restart doesn't reset it.
 */
class SpawnLimiter(private val mod: MoBosses) {
  fun allows(level: ServerLevel, at: Vec3, group: List<ServerPlayer>, now: Long = mod.now()): Boolean {
    val spawn = mod.catalog.settings.spawn
    if (mod.bosses.near(level, at, spawn.nearbyRadius.toDouble()) >= spawn.maxNearby) return false
    return group.none { (Data(it).long(Keys.NEXT_BOSS) ?: 0L) > now }
  }

  fun spawned(group: List<ServerPlayer>, now: Long = mod.now()) {
    val spawn = mod.catalog.settings.spawn
    val wait = Rolls.biased(spawn.playerCooldownSeconds.coerceAtLeast(0) * 1000.0, spawn.playerCooldownBias, Random).toLong()
    group.forEach { Data(it)[Keys.NEXT_BOSS] = now + wait }
  }
}
