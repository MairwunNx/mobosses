package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.enchantment.Enchantment
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.modcore.config.Ids

/**
 * The config files with every game id resolved, built once per load, as Paper's `Catalog`. Whatever didn't resolve
 * was logged and left out, so nothing below has to check ids again. Other mods' mobs, items, and enchantments resolve
 * like the game's own.
 */
class Catalog(val configs: Configs, ids: Ids, log: Log) {
  val settings = configs.settings
  val abilities = configs.abilities
  val classes = configs.classes
  val rewards = configs.rewards

  class BossType(
    val entity: EntityType<*>,
    val spec: BossSpec,
    val particle: ParticleOptions?,
    val deathSound: Holder<SoundEvent>?,
    val summonSound: Holder<SoundEvent>?,
    val weapons: List<Gear>,
    val armors: List<ArmorSet>,
  ) {
    /** `zombie`: what the language files and the boss's saved data call it; another mod's mob keeps its namespace. */
    val key: String = keyOf(entity)
  }

  class Gear(val minLevel: Int, val item: Item, val enchantments: Map<Holder<Enchantment>, Int>)

  class ArmorSet(val minLevel: Int, val helmet: Gear?, val chestplate: Gear?, val leggings: Gear?, val boots: Gear?)

  class Minion(val summoner: EntityType<*>, val entity: EntityType<*>, val name: String?)

  /** An item from loottable.yml; [types] empty for the shared table, else the boss types it belongs to. */
  class Loot(val spec: LootTable.Item, val item: Item, val enchantments: Map<Holder<Enchantment>, IntRange>, val types: Set<EntityType<*>>)

  class Tier(val spec: RewardSettings.Tier, val effects: List<Holder<MobEffect>>)

  val types: Map<EntityType<*>, BossType> = buildMap {
    configs.bosses.forEach { (id, spec) ->
      val where = "bosses.yml: $id"
      val entity = ids.entity(id, where) ?: return@forEach
      if (!ids.living(entity)) return@forEach log.warn("$where: ${BuiltInRegistries.ENTITY_TYPE.getKey(entity)} can't be a boss")
      fun gear(material: String, minLevel: Int, enchantments: Map<String, Int>, path: String) =
        ids.item(material, "$where.$path")?.let { Gear(minLevel, it, enchantments(enchantments, "$where.$path", ids) { level -> level }) }

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

  val minions: Map<EntityType<*>, Minion> = buildMap {
    configs.minions.minions.forEachIndexed { i, m ->
      val where = "minions.yml: minions[$i]"
      val summoner = ids.entity(m.summoner, "$where.summoner") ?: return@forEachIndexed
      val entity = ids.entity(m.entity, "$where.entity") ?: return@forEachIndexed
      if (!ids.living(entity)) return@forEachIndexed log.warn("$where.entity: ${BuiltInRegistries.ENTITY_TYPE.getKey(entity)} can't be summoned")
      putIfAbsent(summoner, Minion(summoner, entity, m.name))
    }
  }

  val loot: List<Loot> = configs.loot.items.mapIndexedNotNull { i, item ->
    val where = "loottable.yml: level_loot[$i]"
    val resolved = ids.item(item.material, "$where.material") ?: return@mapIndexedNotNull null
    if (classes.rank(item.mobclass) < 0) log.warn("$where.mobclass: \"${item.mobclass}\" isn't a class; no boss drops this")
    val types = item.types.mapIndexedNotNull { t, id -> ids.entity(id, "$where.types[$t]") }.toSet()
    // An entry whose types all failed would otherwise fall to every boss.
    if (item.types.isNotEmpty() && types.isEmpty()) return@mapIndexedNotNull null
    Loot(item, resolved, enchantments(item.enchantments, where, ids) { it }, types)
  }

  val tiers: Map<String, Tier> = rewards.tiers.mapValues { (key, tier) ->
    Tier(tier, tier.effectPool.mapIndexedNotNull { i, id -> ids.effect(id, "rewards.yml: tiers.$key.effect_pool[$i]") })
  }

  val arcEffects = abilities.arcNegativeEffects.mapIndexedNotNull { i, id -> ids.effect(id, "abilities.yml: arrow_arc_negative_effects[$i]") }
  val rainEffects = abilities.rainNegativeEffects.mapIndexedNotNull { i, id -> ids.effect(id, "abilities.yml: arrow_rain_negative_effects[$i]") }

  /** Which boss types have each signature ability. */
  val signatureTypes: Map<String, Set<EntityType<*>>> = abilities.signatures.mapValues { (key, signature) ->
    (signature.types ?: AbilitySettings.SIGNATURE_TYPES[key].orEmpty()).mapNotNull { ids.entity(it, "abilities.yml: signature.$key.types") }.toSet()
  }

  val killSound: Holder<SoundEvent>? = settings.killSound.takeIf { it.enabled }?.let { ids.sound(it.key, "config.yml: kill_sound.key") }

  /** The spawn reasons that may make bosses, by Bukkit's names, which `allowed_spawn_reasons` uses on every platform. */
  val spawnReasons: Set<String> = settings.spawn.allowedSpawnReasons.mapNotNull { name ->
    name.trim().uppercase().takeIf { it in SPAWN_REASONS } ?: null.also { log.warn("config.yml: spawn.allowed_spawn_reasons: unknown reason \"$name\"") }
  }.toSet()

  init {
    if (settings.spawn.checkVerticalZone) log.warn("config.yml: spawn.check_vertical_zone is experimental")
  }

  /** The class picked by weight; Common when every weight is zero. */
  companion object {
    /** Bukkit's spawn reasons, which a config may name though the mods only ever see some of them. */
    val SPAWN_REASONS = setOf(
      "NATURAL", "JOCKEY", "CHUNK_GEN", "SPAWNER", "TRIAL_SPAWNER", "EGG", "SPAWNER_EGG", "BUCKET", "LIGHTNING", "BUILD_SNOWMAN",
      "BUILD_IRONGOLEM", "BUILD_WITHER", "VILLAGE_DEFENSE", "VILLAGE_INVASION", "BREEDING", "SLIME_SPLIT", "REINFORCEMENTS",
      "NETHER_PORTAL", "DISPENSE_EGG", "INFECTION", "CURED", "OCELOT_BABY", "SILVERFISH_BLOCK", "MOUNT", "TRAP", "ENDER_PEARL",
      "SHOULDER_ENTITY", "DROWNED", "SHEARED", "EXPLOSION", "RAID", "PATROL", "BEEHIVE", "PIGLIN_ZOMBIFIED", "SPELL", "FROZEN",
      "METAMORPHOSIS", "DUPLICATION", "COMMAND", "ENCHANTMENT", "POTION_EFFECT", "REANIMATE", "CUSTOM", "DEFAULT",
    )

    /** What Mo’Bosses calls a mob type: the game's own by their path, another mod's with its namespace. */
    fun keyOf(type: EntityType<*>): String {
      val id = BuiltInRegistries.ENTITY_TYPE.getKey(type)
      return if (id.namespace == Identifier.DEFAULT_NAMESPACE) id.path else id.toString()
    }

    private fun <V, R> enchantments(map: Map<String, V>, where: String, ids: Ids, value: (V) -> R): Map<Holder<Enchantment>, R> =
      map.mapNotNull { (id, v) -> ids.enchantment(id, "$where.enchantments")?.let { it to value(v) } }.toMap()
  }
}
