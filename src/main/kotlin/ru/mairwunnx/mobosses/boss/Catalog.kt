package ru.mairwunnx.mobosses.boss

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.Ids
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.algebra.Rolls

/**
 * The config files with every game id resolved, built once per load. Whatever didn't resolve was
 * logged and left out, so nothing below has to check ids again.
 */
class Catalog(val configs: Configs, ids: Ids, log: Log) {
  val settings = configs.settings
  val abilities = configs.abilities
  val classes = configs.classes
  val rewards = configs.rewards

  class BossType(
    val entity: EntityType,
    val spec: BossSpec,
    val particle: Particle?,
    val deathSound: Sound?,
    val summonSound: Sound?,
    val weapons: List<Gear>,
    val armors: List<ArmorSet>,
  ) {
    /** `zombie`: what the language files and the boss's saved data call it. */
    val key: String = entity.key.key
  }

  class Gear(val minLevel: Int, val material: Material, val enchantments: Map<Enchantment, Int>)

  class ArmorSet(val minLevel: Int, val helmet: Gear?, val chestplate: Gear?, val leggings: Gear?, val boots: Gear?)

  class Minion(val summoner: EntityType, val entity: EntityType, val name: String?)

  class Loot(val spec: LootTable.Item, val material: Material, val enchantments: Map<Enchantment, IntRange>)

  class Tier(val spec: RewardSettings.Tier, val effects: List<PotionEffectType>)

  val types: Map<EntityType, BossType> = buildMap {
    configs.bosses.forEach { (id, spec) ->
      val where = "bosses.yml: $id"
      val entity = ids.entity(id, where) ?: return@forEach
      if (!entity.isAlive || !entity.isSpawnable) return@forEach log.warn("$where: ${entity.key} can't be a boss")
      fun gear(material: String, minLevel: Int, enchantments: Map<String, Int>, path: String) =
        ids.material(material, "$where.$path")?.let { Gear(minLevel, it, enchantments(enchantments, "$where.$path", ids) { level -> level }) }

      val weapons = spec.equipment.weapons.mapIndexedNotNull { i, w -> gear(w.material, w.minLevel, w.enchantments, "weapons[$i]") }
      val armors = spec.equipment.armors.mapIndexed { i, set ->
        fun piece(p: BossSpec.Piece?, slot: String) = p?.let { gear(it.material, set.minLevel, it.enchantments, "armors[$i].$slot") }
        ArmorSet(set.minLevel, piece(set.helmet, "helmet"), piece(set.chestplate, "chestplate"), piece(set.leggings, "leggings"), piece(set.boots, "boots"))
      }
      val sounds = spec.effects.sounds
      put(entity, BossType(
        entity, spec,
        ids.particle(spec.effects.particles, "$where.effects.particles"),
        ids.sound(sounds.death, "$where.effects.sounds.death"),
        ids.sound(sounds.summon, "$where.effects.sounds.summon"),
        weapons, armors,
      ))
    }
  }

  val minions: Map<EntityType, Minion> = buildMap {
    configs.minions.minions.forEachIndexed { i, m ->
      val where = "minions.yml: minions[$i]"
      val summoner = ids.entity(m.summoner, "$where.summoner") ?: return@forEachIndexed
      val entity = ids.entity(m.entity, "$where.entity") ?: return@forEachIndexed
      if (!entity.isAlive || !entity.isSpawnable) return@forEachIndexed log.warn("$where.entity: ${entity.key} can't be summoned")
      putIfAbsent(summoner, Minion(summoner, entity, m.name))
    }
  }

  val loot: List<Loot> = configs.loot.items.mapIndexedNotNull { i, item ->
    val where = "loottable.yml: level_loot[$i]"
    val material = ids.material(item.material, "$where.material") ?: return@mapIndexedNotNull null
    if (classes.rank(item.mobclass) < 0) log.warn("$where.mobclass: \"${item.mobclass}\" isn't a class; no boss drops this")
    Loot(item, material, enchantments(item.enchantments, where, ids) { it })
  }

  val tiers: Map<String, Tier> = rewards.tiers.mapValues { (key, tier) ->
    Tier(tier, tier.effectPool.mapIndexedNotNull { i, id -> ids.effect(id, "rewards.yml: tiers.$key.effect_pool[$i]") })
  }

  val arcEffects = abilities.arcNegativeEffects.mapIndexedNotNull { i, id -> ids.effect(id, "abilities.yml: arrow_arc_negative_effects[$i]") }
  val rainEffects = abilities.rainNegativeEffects.mapIndexedNotNull { i, id -> ids.effect(id, "abilities.yml: arrow_rain_negative_effects[$i]") }

  /** Which boss types have each signature ability. */
  val signatureTypes: Map<String, Set<EntityType>> = abilities.signatures.mapValues { (key, signature) ->
    (signature.types ?: ru.mairwunnx.mobosses.config.AbilitySettings.SIGNATURE_TYPES[key].orEmpty())
      .mapNotNull { ids.entity(it, "abilities.yml: signature.$key.types") }.toSet()
  }

  val killSound: Sound? = settings.killSound.takeIf { it.enabled }?.let { ids.sound(it.key, "config.yml: kill_sound.key") }

  val spawnReasons: Set<SpawnReason> = settings.spawn.allowedSpawnReasons.mapNotNull { name ->
    SpawnReason.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
      ?: null.also { log.warn("config.yml: spawn.allowed_spawn_reasons: unknown reason \"$name\"") }
  }.toSet()

  init {
    if (settings.spawn.checkVerticalZone) log.warn("config.yml: spawn.check_vertical_zone is experimental")
  }

  /** The class picked by weight; Common when every weight is zero. */
  fun randomClass(random: kotlin.random.Random): BossClass = Rolls.weighted(classes.all.map { it to it.chance }, random) ?: classes.all.first()

  private companion object {
    fun <V, R> enchantments(map: Map<String, V>, where: String, ids: Ids, value: (V) -> R): Map<Enchantment, R> =
      map.mapNotNull { (id, v) -> ids.enchantment(id, "$where.enchantments")?.let { it to value(v) } }.toMap()
  }
}
