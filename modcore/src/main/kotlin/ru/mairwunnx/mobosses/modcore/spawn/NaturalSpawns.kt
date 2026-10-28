package ru.mairwunnx.mobosses.modcore.spawn

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import ru.mairwunnx.mobosses.boss.WorldBosses
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID
import kotlin.random.Random

/**
 * Whether a mob becomes a boss, and at what level, as on Paper. A mob of a type in `bosses.yml` rolls its chance,
 * times the world, time, weather, and difficulty multipliers and the nearest player's survival bonus and a full moon,
 * once in its life: with `on_sight`, the first time a player sees it, else as it spawns near one. The players within
 * `group_radius` set its level together, and a minion's summoner sets a minion's.
 */
class NaturalSpawns(private val mod: MoBosses) {
  private val limiter = SpawnLimiter(mod)

  /** A mob just spawned: minions, and every mob without `on_sight`, roll now; the rest wait for a player to see them. */
  fun spawned(entity: Mob, summoner: UUID?, reason: String) {
    val catalog = mod.catalog
    if (summoner != null || !catalog.settings.spawn.onSight.enabled) return roll(entity, summoner, reason, null).let {}
    if (catalog.types[entity.type]?.spec?.enabled == true) Data(entity)[Keys.PENDING] = reason
  }

  /**
   * Twice a second: waiting mobs a player can see roll, and stop waiting whatever comes out. A mob that got a name,
   * an owner, or a leash meanwhile is someone's and never rolls.
   */
  fun tick() {
    val sight = mod.catalog.settings.spawn.onSight
    if (!sight.enabled) return
    val radius = sight.radius.toDouble()
    mod.server.playerList.players.forEach { player ->
      if (player.isSpectator || player.isDeadOrDying) return@forEach
      val level = player.level()
      level.getEntitiesOfClass(Mob::class.java, player.boundingBox.inflate(radius)) { Data(it).has(Keys.PENDING) }.forEach { mob ->
        if (sight.lineOfSight && !player.hasLineOfSight(mob)) return@forEach
        val data = Data(mob)
        val reason = data.string(Keys.PENDING) ?: return@forEach
        data.remove(Keys.PENDING)
        if (mob.customName != null || (mob as? OwnableEntity)?.owner != null || (mob as? Leashable)?.isLeashed == true) return@forEach
        val boss = roll(mob, null, reason, player) ?: return@forEach
        World.particles(level, ParticleTypes.LARGE_SMOKE, boss.entity.position().add(0.0, boss.entity.bbHeight / 2.0, 0.0), 24, 0.3, boss.entity.bbHeight / 3.0, 0.3, 0.02)
      }
    }
  }

  /** Rolls [entity] into a boss for the player who saw it, or for the nearest one within `spawn_radius`. */
  private fun roll(entity: Mob, summoner: UUID?, reason: String, seer: ServerPlayer?): Boss? {
    val catalog = mod.catalog
    val spawn = catalog.settings.spawn
    if (!catalog.settings.enabled || !spawn.enabled || entity.isNoAi) return null
    val type = catalog.types[entity.type]?.takeIf { it.spec.enabled } ?: return null
    val level = entity.level() as? ServerLevel ?: return null
    // A world's first dragon stays the game's own: it becomes a boss only once it's been beaten and comes back.
    if (entity is EnderDragon && level.dragonFight?.hasPreviouslyKilledDragon() != true) return null
    // The dragon and the wither are fights someone sets up: no cooldown or crowd keeps them ordinary, and they start none.
    val worldBoss = WorldBosses.isWorldBoss(type.key)
    val at = entity.position()
    if (mod.bosses.count(level) >= spawn.maxBossesPerWorld) return null
    val difficulty = mod.bosses.difficulty(level) ?: return null

    val nearest = seer ?: World.players(level, at, spawn.spawnRadius.toDouble()).minByOrNull { it.position().distanceToSqr(at) } ?: return null
    if (spawn.checkVerticalZone && onSurface(level, nearest.position(), spawn) != onSurface(level, at, spawn)) return null
    val graded = Difficulty.part(difficulty, catalog.settings.difficulty.weights.spawnChance)
    if (Random.nextDouble() >= chance(type.spec.spawnChance, spawn, level) * graded * mod.survival.multiplier(nearest) * mod.fullMoon.chance(level, at)) return null

    val group = World.players(level, at, spawn.groupRadius.toDouble()).ifEmpty { listOf(nearest) }
    val where = "${entity.blockX} ${entity.blockY} ${entity.blockZ}"
    if (!worldBoss && !limiter.allows(level, at, group)) return null.also { mod.log.debug { "No ${type.key} boss at $where: too many bosses near, or its players' cooldown" } }
    if (!mod.guards.canSpawnBoss(level, entity.blockPosition())) return null.also { mod.log.debug { "No ${type.key} boss at $where: protected" } }

    val parent = summoner?.let(mod.bosses::get)
    val progression = catalog.settings.progression
    val bossLevel = if (parent != null) {
      Leveling.childLevel(parent.level, progression.miniBossesLevelCurve, Random)
    } else {
      Leveling.bossLevel(progression.variance, Leveling.groupLevel(group.map { mod.progression.level(it.uuid) }), progression.levelCurve.maxLevel, Random)
    }
    val clazz = Rolls.weighted(catalog.classes.all.map { it to mod.fullMoon.weight(level, at, it) }, Random) ?: catalog.classes.all.first()
    val boss = mod.bosses.make(entity, type, clazz, bossLevel, summoner).also(mod.bosses::vanillaName)
    if (reason == "SPAWNER" || reason == "TRIAL_SPAWNER") boss.fromSpawner = true
    if (!worldBoss) limiter.spawned(group)
    announce(boss, level)
    return boss
  }

  /** Players near a new boss of `broadcast.spawns_from` or rarer hear about it, with a warning sound. */
  private fun announce(boss: Boss, level: ServerLevel) {
    val broadcast = mod.catalog.settings.broadcast
    val from = mod.catalog.classes.rank(broadcast.spawnsFrom)
    if (from < 0 || mod.catalog.classes.rank(boss.clazz.key) < from) return
    val near = World.players(level, boss.entity.position(), broadcast.spawnsRadius.toDouble())
    mod.texts.sendAll(near, Message.BOSS_NEARBY) { code -> mod.texts.boss(code, boss) }
    near.forEach { World.sound(it, SoundEvents.ELDER_GUARDIAN_CURSE, volume = 0.6f, pitch = 1.4f) }
  }

  /** The chance with the multipliers for the world, the time of day, and the weather. */
  private fun chance(base: Double, spawn: Settings.Spawn, level: ServerLevel): Double {
    val world = worldMultiplier(spawn, level)
    val time = if (level.defaultClockTime % 24000L in 0L..12999L) spawn.timeMultipliers.day else spawn.timeMultipliers.night
    val weather = when {
      level.isThundering -> spawn.weatherMultipliers.thunder
      level.isRaining -> spawn.weatherMultipliers.rain
      else -> spawn.weatherMultipliers.clear
    }
    return base * world * time * weather
  }

  private fun onSurface(level: ServerLevel, at: Vec3, spawn: Settings.Spawn) =
    at.y >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.x.toInt(), at.z.toInt()) - 1 - spawn.verticalZoneThreshold

  companion object {
    /**
     * `world_multipliers` by the dimension's name, `overworld` or `the_nether`, another mod's as `namespace:path`, or
     * by Paper's folder names, `world`, `world_nether`, `world_the_end`, so a Paper config works unchanged.
     */
    fun worldMultiplier(spawn: Settings.Spawn, level: ServerLevel): Double {
      val id = level.dimension().identifier()
      val paper = when (level.dimension()) {
        Level.OVERWORLD -> "world"
        Level.NETHER -> "world_nether"
        Level.END -> "world_the_end"
        else -> null
      }
      val names = listOfNotNull(if (id.namespace == Identifier.DEFAULT_NAMESPACE) id.path else null, id.toString(), paper)
      return names.firstNotNullOfOrNull(spawn.worldMultipliers::get) ?: 1.0
    }
  }
}
