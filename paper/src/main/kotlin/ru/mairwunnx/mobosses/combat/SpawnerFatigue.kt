package ru.mairwunnx.mobosses.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Bosses from spawners give less to a player who keeps killing them in one spot. Each such kill near
 * here counts, fading by half every `half_life_minutes`; past `free` of them, each one more takes
 * `step` off, down to `floor`. The kills live on the player, so a relog doesn't wipe them.
 */
class SpawnerFatigue(private val plugin: MoBossesPlugin) {
  /** What the reward for a spawner boss killed at [where] is multiplied by, and the kill noted. */
  fun take(player: Player, where: Location, now: Long = System.currentTimeMillis()): Double {
    val cfg = plugin.catalog.settings.spawnerFatigue
    if (!cfg.enabled) return 1.0
    val data = player.persistentDataContainer
    val (factor, kills) = Fatigue.take(cfg, data.get(plugin.keys.spawnerKills, PersistentDataType.STRING), where.world.name, where.blockX, where.blockZ, now)
    data.set(plugin.keys.spawnerKills, PersistentDataType.STRING, kills)
    return factor
  }
}
