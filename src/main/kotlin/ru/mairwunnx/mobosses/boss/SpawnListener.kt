package ru.mairwunnx.mobosses.boss

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
import java.util.UUID
import kotlin.random.Random

/**
 * Turns spawning mobs into bosses, and keeps track of bosses as their chunks load and unload. A
 * mob becomes a boss by chance, near a player whose Mo’Bosses level sets the boss's level; a minion
 * a boss summoned may become a weaker boss the same way.
 */
class SpawnListener(private val plugin: MoBossesPlugin) : Listener {
  /** While `/mobosses spawn` makes a boss, its mob mustn't roll into another one. */
  var spawningByCommand = false

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onSpawn(event: CreatureSpawnEvent) {
    val entity = event.entity as? Mob ?: return
    if (spawningByCommand || event.spawnReason !in plugin.catalog.spawnReasons) return
    roll(entity)
  }

  /** A boss coming back with its chunk, or after a restart. Only reads: the event mustn't change the entity. */
  @EventHandler
  fun onAdd(event: EntityAddToWorldEvent) {
    plugin.bosses.adopt(event.entity)?.let { boss -> Bukkit.getScheduler().runTask(plugin, Runnable { if (boss.entity.isValid) plugin.bosses.nameTag(boss) }) }
  }

  /** Dead, unloaded, or removed: the entity keeps its data, the plugin lets go of the rest. */
  @EventHandler
  fun onRemove(event: EntityRemoveFromWorldEvent) {
    plugin.bosses.get(event.entity)?.let(plugin.bosses::forget)
  }

  private fun roll(entity: Mob) {
    val catalog = plugin.catalog
    val spawn = catalog.settings.spawn
    if (!catalog.settings.enabled || !spawn.enabled || !entity.hasAI()) return
    val type = catalog.types[entity.type]?.takeIf { it.spec.enabled } ?: return
    val at = entity.location
    if (plugin.bosses.count(at.world) >= spawn.maxBossesPerWorld) return

    val radius = spawn.spawnRadius.toDouble()
    val player = at.world.getNearbyPlayers(at, radius, radius, radius) { it.gameMode != GameMode.SPECTATOR }
      .minByOrNull { it.location.distanceSquared(at) } ?: return

    if (spawn.checkVerticalZone) {
      val threshold = spawn.verticalZoneThreshold
      val playerOnSurface = player.location.y >= player.world.getHighestBlockYAt(player.location) - threshold
      val mobOnSurface = at.y >= at.world.getHighestBlockYAt(at) - threshold
      // One on the surface, the other in a cave: no boss.
      if (playerOnSurface != mobOnSurface) return
    }

    val world = spawn.worldMultipliers[at.world.name] ?: 1.0
    val time = if (at.world.time in 0..12999) spawn.timeMultipliers.day else spawn.timeMultipliers.night
    val weather = when {
      at.world.isThundering -> spawn.weatherMultipliers.thunder
      at.world.hasStorm() -> spawn.weatherMultipliers.rain
      else -> spawn.weatherMultipliers.clear
    }
    val chance = type.spec.spawnChance * world * time * weather
    if (Random.nextDouble() >= chance) return
    if (!plugin.guards.canSpawnBoss(at)) {
      return plugin.log.debug { "No ${type.key} boss at ${at.blockX} ${at.blockY} ${at.blockZ}: ${plugin.guards.regionsAt(at).joinToString().ifEmpty { "protected" }}" }
    }

    val summoner = entity.persistentDataContainer.get(plugin.keys.summoner, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val parent = summoner?.let(plugin.bosses::get)
    val level = if (parent != null) Formulas.childLevel(parent.level, catalog.settings.progression.miniBossesLevelCurve, Random) else levelFor(player)
    plugin.bosses.make(entity, type, catalog.randomClass(Random), level, summoner)
  }

  /** A natural boss's level, from the nearest player's. */
  private fun levelFor(player: Player): Int {
    val progression = plugin.catalog.settings.progression
    return Formulas.bossLevel(progression.variance, plugin.progression.level(player.uniqueId), Random)
  }
}
