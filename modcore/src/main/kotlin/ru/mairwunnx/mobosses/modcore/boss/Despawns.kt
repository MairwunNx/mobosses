package ru.mairwunnx.mobosses.modcore.boss

import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Bosses nobody comes near for `despawn.after_minutes` leave the world without loot, as vanilla mobs do, instead of
 * piling up in caves for good. When a player was last near lives on the boss, so one found again long after its chunk
 * unloaded leaves as the chunk loads, before anyone reaches it. Bosses from `/mobosses spawn` stay unless
 * `keep_spawned_by_command` is off. The minutes are the server's, which stand still while a singleplayer game pauses.
 */
class Despawns(private val mod: MoBosses) {
  /** Every few seconds: a boss with a player near is seen now; the rest leave once their time is up. */
  fun tick(now: Long = mod.now()) {
    if (!mod.catalog.settings.despawn.enabled) return
    mod.bosses.all.toList().forEach { boss ->
      if (kept(boss)) return@forEach
      if (mod.players(boss.entity, mod.catalog.settings.despawn.radius.toDouble()).isNotEmpty()) boss.seen = now
      else if (stale(boss, now)) leave(boss)
    }
  }

  /** A boss whose chunk just loaded: gone if its time ran out while nobody was there. Not now: the game is adding it. */
  fun loaded(boss: Boss, now: Long = mod.now()) {
    if (!mod.catalog.settings.despawn.enabled || kept(boss)) return
    if (stale(boss, now)) mod.scheduler.later(1) { if (boss.entity.isAlive) leave(boss) }
  }

  private fun kept(boss: Boss) = boss.pinned && mod.catalog.settings.despawn.keepSpawnedByCommand

  /** A boss with no time on it starts counting now. */
  private fun stale(boss: Boss, now: Long): Boolean {
    val seen = boss.seen ?: now.also { boss.seen = it }
    // A save moved from another server may carry a time ahead of this one's clock.
    if (seen > now) boss.seen = now
    return now - seen >= mod.catalog.settings.despawn.afterMinutes.coerceAtLeast(1) * 60_000L
  }

  private fun leave(boss: Boss) {
    mod.log.debug { "A level ${boss.level} ${boss.clazz.key} ${boss.type.key} despawned at ${boss.entity.blockX} ${boss.entity.blockY} ${boss.entity.blockZ}: nobody came near" }
    mod.bosses.remove(boss)
  }
}
