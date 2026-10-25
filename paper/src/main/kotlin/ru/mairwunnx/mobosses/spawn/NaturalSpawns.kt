package ru.mairwunnx.mobosses.spawn

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.config.Settings
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import ru.mairwunnx.mobosses.algebra.Rolls
import java.util.UUID
import kotlin.random.Random

/**
 * Whether a mob becomes a boss, and at what level. A mob of a type in `bosses.yml` rolls its
 * chance, times the world, time, and weather multipliers and the nearest player's survival bonus
 * and a full moon, once in its life: with `on_sight`, the first time a player sees it, else as it
 * spawns near one. Walking off and coming back never rolls it again. The players within
 * `group_radius` set its level together, and a minion's summoner sets a minion's.
 */
class NaturalSpawns(private val plugin: MoBossesPlugin) {
  private val limiter = SpawnLimiter(plugin)

  /** A mob just spawned: minions, and every mob without `on_sight`, roll now; the rest wait for a player to see them. */
  fun spawned(entity: Mob, summoner: UUID?, reason: SpawnReason) {
    val catalog = plugin.catalog
    if (summoner != null || !catalog.settings.spawn.onSight.enabled) return roll(entity, summoner, reason, null).let {}
    if (catalog.types[entity.type]?.spec?.enabled == true) entity.persistentDataContainer.set(plugin.keys.pending, PersistentDataType.STRING, reason.name)
  }

  /**
   * Twice a second: waiting mobs a player can see roll, and stop waiting whatever comes out. A mob
   * that got a name, an owner, or a leash meanwhile is someone's and never rolls.
   */
  fun tick() {
    val sight = plugin.catalog.settings.spawn.onSight
    if (!sight.enabled) return
    val radius = sight.radius.toDouble()
    Bukkit.getOnlinePlayers().forEach { player ->
      if (player.gameMode == GameMode.SPECTATOR || player.isDead) return@forEach
      player.getNearbyEntities(radius, radius, radius).forEach { entity ->
        val mob = entity as? Mob ?: return@forEach
        val reason = mob.persistentDataContainer.get(plugin.keys.pending, PersistentDataType.STRING) ?: return@forEach
        if (sight.lineOfSight && !player.hasLineOfSight(mob)) return@forEach
        mob.persistentDataContainer.remove(plugin.keys.pending)
        if (mob.customName() != null || (mob as? Tameable)?.isTamed == true || mob.isLeashed) return@forEach
        val boss = roll(mob, null, runCatching { SpawnReason.valueOf(reason) }.getOrDefault(SpawnReason.DEFAULT), player) ?: return@forEach
        val at = boss.entity.location.add(0.0, boss.entity.height / 2, 0.0)
        at.world.spawnParticle(Particle.LARGE_SMOKE, at, 24, 0.3, boss.entity.height / 3, 0.3, 0.02)
      }
    }
  }

  /** Rolls [entity] into a boss for the player who saw it, or for the nearest one within `spawn_radius`. */
  private fun roll(entity: Mob, summoner: UUID?, reason: SpawnReason, seer: Player?): Boss? {
    val catalog = plugin.catalog
    val spawn = catalog.settings.spawn
    if (!catalog.settings.enabled || !spawn.enabled || !entity.hasAI()) return null
    val type = catalog.types[entity.type]?.takeIf { it.spec.enabled } ?: return null
    val at = entity.location
    if (plugin.bosses.count(at.world) >= spawn.maxBossesPerWorld) return null
    val difficulty = plugin.bosses.difficulty(at.world) ?: return null

    val nearest = seer ?: players(at, spawn.spawnRadius.toDouble()).minByOrNull { it.location.distanceSquared(at) } ?: return null
    if (spawn.checkVerticalZone && onSurface(nearest.location, spawn) != onSurface(at, spawn)) return null
    val graded = Difficulty.part(difficulty, catalog.settings.difficulty.weights.spawnChance)
    if (Random.nextDouble() >= chance(type.spec.spawnChance, spawn, at) * graded * plugin.survival.multiplier(nearest) * plugin.fullMoon.chance(at.world)) return null

    val group = players(at, spawn.groupRadius.toDouble()).ifEmpty { listOf(nearest) }
    if (!limiter.allows(at, group)) return null.also { plugin.log.debug { "No ${type.key} boss at ${at.blockX} ${at.blockY} ${at.blockZ}: too many bosses near, or its players' cooldown" } }
    if (!plugin.guards.canSpawnBoss(at)) {
      return null.also { plugin.log.debug { "No ${type.key} boss at ${at.blockX} ${at.blockY} ${at.blockZ}: ${plugin.guards.regionsAt(at).joinToString().ifEmpty { "protected" }}" } }
    }

    val parent = summoner?.let(plugin.bosses::get)
    val progression = catalog.settings.progression
    val level = if (parent != null) {
      Leveling.childLevel(parent.level, progression.miniBossesLevelCurve, Random)
    } else {
      Leveling.bossLevel(progression.variance, Leveling.groupLevel(group.map { plugin.progression.level(it.uniqueId) }), progression.levelCurve.maxLevel, Random)
    }
    val clazz = Rolls.weighted(catalog.classes.all.map { it to plugin.fullMoon.weight(at.world, it) }, Random) ?: catalog.classes.all.first()
    val boss = plugin.bosses.make(entity, type, clazz, level, summoner).also(plugin.bosses::vanillaName)
    if (reason == SpawnReason.SPAWNER || reason == SpawnReason.TRIAL_SPAWNER) boss.fromSpawner = true
    limiter.spawned(group)
    announce(boss)
    return boss
  }

  /** Players near a new boss of `broadcast.spawns_from` or rarer hear about it, with a warning sound. */
  private fun announce(boss: Boss) {
    val broadcast = plugin.catalog.settings.broadcast
    val from = plugin.catalog.classes.rank(broadcast.spawnsFrom)
    if (from < 0 || plugin.catalog.classes.rank(boss.clazz.key) < from) return
    val near = players(boss.entity.location, broadcast.spawnsRadius.toDouble())
    plugin.texts.sendAll(near, Message.BOSS_NEARBY) { code -> plugin.texts.boss(code, boss) }
    near.forEach { it.playSound(it.location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.4f) }
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
