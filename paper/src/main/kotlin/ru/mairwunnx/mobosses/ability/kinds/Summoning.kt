package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.boss.WorldBosses
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * Minions from `minions.yml` at the boss's feet, topping up to `summoning_max_alive`; some may roll
 * into weaker bosses. Those mini bosses summon nothing themselves unless `summoning_by_mini_bosses`
 * is on, so a fight doesn't snowball. The dragon and the wither send a pack to each player they fight
 * instead, up to `summoning_max_alive` a player, flyers above them and the rest beside them.
 */
object Summoning : Ability {
  override val key = "summoning"
  override val message = Message.SUMMONING
  override fun enabled(settings: AbilitySettings) = settings.summoningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.summoning
  override fun cooldown(settings: AbilitySettings) = settings.summoningCooldown to settings.summoningCooldownBias

  private val BABIES = setOf(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.PIGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.HOGLIN)

  override fun usable(cast: Cast) =
    cast.plugin.catalog.minions.containsKey(cast.boss.entity.type) && (cast.boss.summoner == null || cast.settings.summoningByMiniBosses) && room(cast) > 0

  /** Minions that rolled into bosses count too. A world boss keeps `summoning_max_alive` for each player it fights. */
  private fun room(cast: Cast): Int {
    val slots = if (WorldBosses.isWorldBoss(cast.boss.type.key)) cast.foes().size.coerceAtLeast(1) else 1
    return cast.settings.summoningMaxAlive * slots - cast.plugin.minions.alive(cast.boss.uuid) - cast.plugin.bosses.all.count { it.summoner == cast.boss.uuid && it.entity.isValid }
  }

  override fun cast(cast: Cast): Boolean {
    val minion = cast.plugin.catalog.minions[cast.boss.entity.type] ?: return false
    val settings = cast.plugin.catalog.settings
    // The dragon and the wither fight from above and afar: each player they fight gets a pack of their own, at them and after them.
    val packs: List<Pair<Location, Player?>> =
      if (WorldBosses.isWorldBoss(cast.boss.type.key)) cast.foes().map { spot(it, minion.entity) to it }.ifEmpty { listOf(cast.at to null) }
      else listOf(cast.at to null)
    var room = room(cast)
    var spawned = 0
    packs.forEach { (at, foe) ->
      val rolled = (Rolls.minions(settings.formula.minions, cast.level, Random) * cast.boss.graded(settings.difficulty.weights.minions)).roundToInt().coerceAtLeast(1)
      repeat(minOf(rolled, room)) {
        val entity = at.world.spawn(at, minion.entity.entityClass!!, SpawnReason.CUSTOM) { spawned ->
          spawned.persistentDataContainer.set(cast.plugin.keys.summoner, PersistentDataType.STRING, cast.boss.uuid.toString())
          spawned.persistentDataContainer.set(cast.plugin.keys.minionOf, PersistentDataType.STRING, cast.boss.type.key)
        }
        if (entity is Ageable && entity.type in BABIES) entity.setBaby()
        if (foe != null && entity is Mob) entity.target = foe
        // Without per-player labels, the game's own name shows, in the default language, as in 1.x.
        if (!cast.plugin.catalog.settings.nameTags.perPlayer) cast.plugin.texts.minionName(cast.plugin.languages.default, minion)?.let {
          entity.customName(it)
          entity.isCustomNameVisible = true
        }
        room--
        spawned++
      }
    }
    cast.boss.type.summonSound?.let { cast.sound(it) }
    cast.sound(Sound.ENTITY_EVOKER_CAST_SPELL)
    return spawned > 0
  }

  /** Where a pack for [foe] comes in: flyers above them, the rest on the ground a few blocks off. */
  private fun spot(foe: Player, type: EntityType): Location {
    val at = foe.location
    val x = at.x + Random.nextDouble(-4.0, 4.0)
    val z = at.z + Random.nextDouble(-4.0, 4.0)
    if (type in FLYERS) return Location(at.world, x, at.y + Random.nextDouble(8.0, 12.0), z)
    return Targets.ground(at.world, x, at.y, z)?.let { it.location.add(0.5, 1.0, 0.5) } ?: at
  }

  private val FLYERS = setOf(EntityType.PHANTOM, EntityType.GHAST, EntityType.VEX)
}
