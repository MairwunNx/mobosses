package ru.mairwunnx.mobosses.spawn

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings
import java.util.UUID
import kotlin.random.Random

/**
 * Whether a spawning mob becomes a boss, and at what level. A mob of a type in `bosses.yml` rolls
 * its chance, times the world, time, and weather multipliers, when players are near; the players
 * within `group_radius` set its level together, and a minion's summoner sets a minion's.
 */
class NaturalSpawns(private val plugin: MoBossesPlugin) {
  private val limiter = SpawnLimiter(plugin)

  fun roll(entity: Mob, summoner: UUID?) {
    val catalog = plugin.catalog
    val spawn = catalog.settings.spawn
    if (!catalog.settings.enabled || !spawn.enabled || !entity.hasAI()) return
    val type = catalog.types[entity.type]?.takeIf { it.spec.enabled } ?: return
    val at = entity.location
    if (plugin.bosses.count(at.world) >= spawn.maxBossesPerWorld) return

    val nearest = players(at, spawn.spawnRadius.toDouble()).minByOrNull { it.location.distanceSquared(at) } ?: return
    if (spawn.checkVerticalZone && onSurface(nearest.location, spawn) != onSurface(at, spawn)) return
    if (Random.nextDouble() >= chance(type.spec.spawnChance, spawn, at)) return

    val group = players(at, spawn.groupRadius.toDouble()).ifEmpty { listOf(nearest) }
    if (!limiter.allows(at, group)) return plugin.log.debug { "No ${type.key} boss at ${at.blockX} ${at.blockY} ${at.blockZ}: too many bosses near, or its players' cooldown" }
    if (!plugin.guards.canSpawnBoss(at)) {
      return plugin.log.debug { "No ${type.key} boss at ${at.blockX} ${at.blockY} ${at.blockZ}: ${plugin.guards.regionsAt(at).joinToString().ifEmpty { "protected" }}" }
    }

    val parent = summoner?.let(plugin.bosses::get)
    val progression = catalog.settings.progression
    val level = if (parent != null) {
      Leveling.childLevel(parent.level, progression.miniBossesLevelCurve, Random)
    } else {
      Leveling.bossLevel(progression.variance, Leveling.groupLevel(group.map { plugin.progression.level(it.uniqueId) }), progression.levelCurve.maxLevel, Random)
    }
    plugin.bosses.make(entity, type, catalog.randomClass(Random), level, summoner).also(plugin.bosses::vanillaName)
    limiter.spawned(group)
  }

  /** The chance with the multipliers for the world, the time of day, and the weather. */
  private fun chance(base: Double, spawn: Settings.Spawn, at: Location): Double {
    val world = spawn.worldMultipliers[at.world.name] ?: 1.0
    val time = if (at.world.time in 0..12999) spawn.timeMultipliers.day else spawn.timeMultipliers.night
    val weather = when {
      at.world.isThundering -> spawn.weatherMultipliers.thunder
      at.world.hasStorm() -> spawn.weatherMultipliers.rain
      else -> spawn.weatherMultipliers.clear
    }
    return base * world * time * weather
  }

  private fun players(at: Location, radius: Double): List<Player> =
    at.world.getNearbyPlayers(at, radius, radius, radius) { it.gameMode != GameMode.SPECTATOR }.toList()

  private fun onSurface(at: Location, spawn: Settings.Spawn) = at.y >= at.world.getHighestBlockYAt(at) - spawn.verticalZoneThreshold
}
