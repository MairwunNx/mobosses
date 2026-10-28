package ru.mairwunnx.mobosses.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
  /** The chance, 0 to 1, of one more item per kill, from this type's own loot, for one of the players who got loot. */
  @SerialName("bonus_loot_chance") val bonusLootChance: Double = 0.0,
  /** Only grown mobs of this type become bosses, never babies. */
  @SerialName("adults_only") val adultsOnly: Boolean = false,
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
