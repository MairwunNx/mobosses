package ru.mairwunnx.mobosses.spawn

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Rolls
import kotlin.random.Random

/**
 * Keeps bosses from piling up: no new one while `max_nearby` are close already, and after a boss
 * spawns for a group, its players wait `player_cooldown_seconds`, stretched at random so a group's
 * bosses don't come on a beat. The wait lives on each player, so a relog or a restart doesn't reset
 * it. Mob farms and caves full of spawners stay farms, not boss factories.
 */
class SpawnLimiter(private val plugin: MoBossesPlugin) {
  fun allows(at: Location, group: List<Player>, now: Long = System.currentTimeMillis()): Boolean {
    val spawn = plugin.catalog.settings.spawn
    if (plugin.bosses.near(at, spawn.nearbyRadius.toDouble()) >= spawn.maxNearby) return false
    return group.none { (it.persistentDataContainer.get(plugin.keys.nextBoss, PersistentDataType.LONG) ?: 0L) > now }
  }

  fun spawned(group: List<Player>, now: Long = System.currentTimeMillis()) {
    val spawn = plugin.catalog.settings.spawn
    val wait = Rolls.biased(spawn.playerCooldownSeconds.coerceAtLeast(0) * 1000.0, spawn.playerCooldownBias, Random).toLong()
    group.forEach { it.persistentDataContainer.set(plugin.keys.nextBoss, PersistentDataType.LONG, now + wait) }
  }
}
