@file:UseSerializers(RangeSerializer::class)

package ru.mairwunnx.mobosses.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * `bosses.yml`: which mobs can become bosses, keyed by entity type (`ZOMBIE` or `minecraft:zombie`),
 * with their spawn chance, gear by level, particles, and sounds.
 */
@Serializable
data class BossSpec(
  /** 1.x's name for the boss, shown to everyone. Leave it out to use `bosses.<type>` from the language files. */
  val name: String? = null,
  val enabled: Boolean = true,
  /** The chance, 0 to 1, that a mob of this type spawning near a player becomes a boss. */
  @SerialName("spawn_chance") val spawnChance: Double = 0.0,
  @SerialName("exp_modifier") val expModifier: Double = 1.0,
  /** Creepers only: a charged creeper. */
  @SerialName("is_powered") val isPowered: Boolean? = null,
  val equipment: Equipment = Equipment(),
  val effects: Effects = Effects(),
) {
  @Serializable
  data class Equipment(
    val weapons: List<Weapon> = emptyList(),
    val armors: List<ArmorSet> = emptyList(),
  )

  @Serializable
  data class Weapon(
    val material: String = "",
    @SerialName("min_lvl") val minLevel: Int = 1,
    val enchantments: Map<String, Int> = emptyMap(),
  )

  @Serializable
  data class ArmorSet(
    @SerialName("min_lvl") val minLevel: Int = 1,
    val helmet: Piece? = null,
    val chestplate: Piece? = null,
    val leggings: Piece? = null,
    val boots: Piece? = null,
  )

  @Serializable
  data class Piece(
    val material: String = "",
    val enchantments: Map<String, Int> = emptyMap(),
  )

  @Serializable
  data class Effects(
    val particles: String = "minecraft:flame",
    val sounds: Sounds = Sounds(),
  )

  @Serializable
  data class Sounds(
    val spawn: String = "",
    val death: String = "minecraft:entity.generic.death",
    val summon: String = "",
  )

  companion object {
    val MAP: KSerializer<Map<String, BossSpec>> = MapSerializer(String.serializer(), serializer())

    fun parse(text: String, warn: (String) -> Unit = {}): Map<String, BossSpec> = Yamls.decode(MAP, text, warn)
  }
}

/** `minions.yml`: what each boss type summons. */
@Serializable
data class Minions(val minions: List<Minion> = emptyList()) {
  @Serializable
  data class Minion(
    val summoner: String = "",
    val entity: String = "",
    /** 1.x's name for the minion, shown to everyone. Leave it out to use `minions.<summoner>` from the language files. */
    val name: String? = null,
  )

  companion object {
    fun parse(text: String, warn: (String) -> Unit = {}): Minions = Yamls.decode(serializer(), text, warn)
  }
}

/** `rewards.yml`: potion effects for players who dealt enough of a boss's health. */
@Serializable
data class Rewards(
  val enabled: Boolean = true,
  @SerialName("extinguish_player") val extinguishPlayer: Boolean = true,
  @SerialName("cleanse_negatives") val cleanseNegatives: Boolean = true,
  /** The share of a boss's health a player must deal to get rewards. */
  @SerialName("min_damage_share") val minDamageShare: Double = 0.27777777,
  @SerialName("base_duration_seconds") val baseDurationSeconds: Int = 8,
  @SerialName("duration_per_level") val durationPerLevel: Double = 0.6,
  @SerialName("max_duration_seconds") val maxDurationSeconds: Int = 60,
  @SerialName("amplifier_base") val amplifierBase: Int = 0,
  @SerialName("amplifier_per_level") val amplifierPerLevel: Double = 0.035,
  @SerialName("amplifier_max") val amplifierMax: Int = 3,
  @SerialName("cooldown_multiplier") val cooldownMultiplier: Double = 2.0,
  @SerialName("cooldown_min_seconds") val cooldownMinSeconds: Int = 8,
  @SerialName("cooldown_max_seconds") val cooldownMaxSeconds: Int = 180,
  val tiers: Map<String, Tier> = DEFAULT_TIERS,
) {
  @Serializable
  data class Tier(
    @SerialName("effects_min") val effectsMin: Int = 1,
    @SerialName("effects_max") val effectsMax: Int = 1,
    @SerialName("extra_absorption_hearts") val extraAbsorptionHearts: Int = 0,
    @SerialName("effect_pool") val effectPool: List<String> = emptyList(),
  )

  companion object {
    private val BASE = listOf("SPEED", "HASTE", "REGENERATION", "RESISTANCE", "FIRE_RESISTANCE")

    val DEFAULT_TIERS = mapOf(
      "common" to Tier(1, 1, 0, listOf("SPEED", "HASTE", "REGENERATION")),
      "uncommon" to Tier(1, 2, 0, listOf("REGENERATION", "HASTE", "WATER_BREATHING", "NIGHT_VISION", "JUMP_BOOST")),
      "rare" to Tier(2, 2, 2, BASE + "LUCK"),
      "epic" to Tier(2, 3, 4, BASE + listOf("DOLPHINS_GRACE", "CONDUIT_POWER")),
      "legendary" to Tier(3, 3, 6, BASE + listOf("DOLPHINS_GRACE", "CONDUIT_POWER")),
      "mythic" to Tier(3, 4, 8, BASE + listOf("STRENGTH", "DOLPHINS_GRACE", "CONDUIT_POWER", "SATURATION")),
    )

    fun parse(text: String, warn: (String) -> Unit = {}): Rewards = Yamls.decode(serializer(), text, warn)
  }
}

/** `loottable.yml`: what bosses drop, by class and level. */
@Serializable
data class LootTable(
  @SerialName("level_loot") val items: List<Item> = emptyList(),
  @SerialName("loot_settings") val settings: LootSettings = LootSettings(),
) {
  @Serializable
  data class Item(
    val material: String = "",
    /** The lowest boss class that drops it; higher classes drop it at lower odds. */
    val mobclass: String = "common",
    @SerialName("min_level") val minLevel: Int = 0,
    @SerialName("max_level") val maxLevel: Int = 100,
    val amount: IntRange = IntRange(1, 1),
    val weight: Double = 1.0,
    @SerialName("random_durability") val randomDurability: Boolean = false,
    val enchantments: Map<String, IntRange> = emptyMap(),
    /** rare, epic, legendary, or mythic: announced to everyone, and worn less by random durability. */
    @SerialName("itemClass") val itemClass: String? = null,
    /** Names `items.<id>.name` and `items.<id>.lore` in the language files, so each player reads them in their language. */
    val id: String? = null,
    /** 1.x's name and lore, shown to everyone; `&` and `§` color codes or MiniMessage. */
    val title: String? = null,
    val lore: List<String> = emptyList(),
  )

  @Serializable
  data class LootSettings(
    /** Each level above an item's `min_level` adds this share to its amount. */
    @SerialName("per_level_drop_factor") val perLevelDropFactor: Double = 0.08,
  )

  companion object {
    fun parse(text: String, warn: (String) -> Unit = {}): LootTable = Yamls.decode(serializer(), text, warn)
  }
}
