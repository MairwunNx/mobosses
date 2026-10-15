package ru.mairwunnx.mobosses.boss

import org.bukkit.Bukkit
import org.bukkit.GameMode
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Bosses nobody comes near for `despawn.after_minutes` leave the world without loot, as vanilla mobs
 * do, instead of piling up in caves for good. When a player was last near lives on the boss, so one
 * found again long after its chunk unloaded leaves as the chunk loads, before anyone reaches it.
 * Bosses from `/mobosses spawn` stay unless `keep_spawned_by_command` is off.
 */
class Despawns(private val plugin: MoBossesPlugin) {
  /** Every few seconds: a boss with a player near is seen now; the rest leave once their time is up. */
  fun tick(now: Long = System.currentTimeMillis()) {
    if (!plugin.catalog.settings.despawn.enabled) return
    plugin.bosses.all.toList().forEach { boss ->
      if (kept(boss)) return@forEach
      if (playerNear(boss)) boss.seen = now
      else if (stale(boss, now)) leave(boss)
    }
  }

  /** A boss whose chunk just loaded: gone if its time ran out while nobody was there. Not now: the game forbids removing an entity while it's added. */
  fun loaded(boss: Boss, now: Long = System.currentTimeMillis()) {
    if (!plugin.catalog.settings.despawn.enabled || kept(boss)) return
    if (stale(boss, now)) Bukkit.getScheduler().runTask(plugin, Runnable { if (boss.entity.isValid) leave(boss) })
  }

  private fun kept(boss: Boss) = boss.pinned && plugin.catalog.settings.despawn.keepSpawnedByCommand

  /** A boss from before 2.3 has no time on it; it starts counting now. */
  private fun stale(boss: Boss, now: Long): Boolean {
    val seen = boss.seen ?: now.also { boss.seen = it }
    return now - seen >= plugin.catalog.settings.despawn.afterMinutes.coerceAtLeast(1) * 60_000L
  }

  private fun playerNear(boss: Boss): Boolean {
    val radius = plugin.catalog.settings.despawn.radius.toDouble()
    return boss.entity.world.getNearbyPlayers(boss.entity.location, radius) { it.gameMode != GameMode.SPECTATOR }.isNotEmpty()
  }

  private fun leave(boss: Boss) {
    plugin.log.debug { "A level ${boss.level} ${boss.clazz.key} ${boss.type.key} despawned at ${boss.entity.world.name} ${boss.entity.location.blockX} ${boss.entity.location.blockY} ${boss.entity.location.blockZ}: nobody came near" }
    plugin.bosses.remove(boss)
  }
}
