package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.boss.WorldBosses
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Minions from `minions.yml` at the boss's feet, topping up to `summoning_max_alive`; some may roll into weaker
 * bosses. Those mini bosses summon nothing themselves unless `summoning_by_mini_bosses` is on, so a fight doesn't
 * snowball. The dragon and the wither send a pack to each player they fight instead, up to `summoning_max_alive` a
 * player, flyers above them and the rest beside them.
 */
object Summoning : Ability {
  override val key = "summoning"
  override val message = Message.SUMMONING
  override fun enabled(settings: AbilitySettings) = settings.summoningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.summoning
  override fun cooldown(settings: AbilitySettings) = settings.summoningCooldown to settings.summoningCooldownBias

  private val BABIES = setOf(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.PIGLIN, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HOGLIN)

  override fun usable(cast: Cast) =
    cast.mod.catalog.minions.containsKey(cast.entity.type) && (cast.boss.summoner == null || cast.settings.summoningByMiniBosses) && room(cast) > 0

  /** Minions that rolled into bosses count too. A world boss keeps `summoning_max_alive` for each player it fights. */
  private fun room(cast: Cast): Int {
    val slots = if (WorldBosses.isWorldBoss(cast.boss.type.key)) cast.foes().size.coerceAtLeast(1) else 1
    return cast.settings.summoningMaxAlive * slots - cast.mod.minions.alive(cast.boss.uuid) - cast.mod.bosses.all.count { it.summoner == cast.boss.uuid && it.entity.isAlive }
  }

  override fun cast(cast: Cast): Boolean {
    val mod = cast.mod
    val minion = mod.catalog.minions[cast.entity.type] ?: return false
    val settings = mod.catalog.settings
    // The dragon and the wither fight from above and afar: each player they fight gets a pack of their own, at them and after them.
    val packs: List<Pair<Vec3, ServerPlayer?>> =
      if (WorldBosses.isWorldBoss(cast.boss.type.key)) cast.foes().map { spot(cast, it, minion.entity) to it }.ifEmpty { listOf(cast.at to null) }
      else listOf(cast.at to null)
    var room = room(cast)
    var spawned = 0
    packs.forEach { (at, foe) ->
      val rolled = (Rolls.minions(settings.formula.minions, cast.level, Random) * cast.boss.graded(settings.difficulty.weights.minions)).roundToInt().coerceAtLeast(1)
      repeat(minOf(rolled, room)) {
        // No finalizeSpawn, so it enters the world as Paper's CUSTOM and rolls by its summoner.
        val entity = minion.entity.create(cast.world, EntitySpawnReason.MOB_SUMMONED) ?: return@repeat
        entity.snapTo(at.x, at.y, at.z, cast.entity.yRot, 0f)
        Data(entity)[Keys.SUMMONER] = cast.boss.uuid.toString()
        Data(entity)[Keys.MINION_OF] = cast.boss.type.key
        if (entity is Mob && entity.type in BABIES) entity.setBaby(true)
        // Without per-player labels, the game's own name shows, in the default language, as in 1.x.
        if (!settings.nameTags.perPlayer) mod.texts.minionName(mod.languages.default, minion)?.let {
          entity.customName = mod.texts.vanilla(it)
          entity.isCustomNameVisible = true
        }
        if (cast.world.addFreshEntity(entity)) {
          if (foe != null && entity is Mob) entity.setTarget(foe)
          room--
          spawned++
        }
      }
    }
    cast.boss.type.summonSound?.let { World.sound(cast.world, cast.at, it) }
    cast.sound(SoundEvents.EVOKER_CAST_SPELL)
    return spawned > 0
  }

  /** Where a pack for [foe] comes in: flyers above them, the rest on the ground a few blocks off. */
  private fun spot(cast: Cast, foe: ServerPlayer, type: EntityType<*>): Vec3 {
    val at = foe.position()
    val x = at.x + Random.nextDouble(-4.0, 4.0)
    val z = at.z + Random.nextDouble(-4.0, 4.0)
    if (type in FLYERS) return Vec3(x, at.y + Random.nextDouble(8.0, 12.0), z)
    return Targets.ground(cast.world, x, at.y, z)?.let { Vec3(it.x + 0.5, it.y + 1.0, it.z + 0.5) } ?: at
  }

  private val FLYERS = setOf<EntityType<*>>(EntityTypes.PHANTOM, EntityTypes.GHAST, EntityTypes.VEX)
}
