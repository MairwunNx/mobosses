package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `classes.yml`: the six classes from Common to Mythic. Each multiplies a boss's health, damage,
 * experience, armor, and toughness; `chance` is its weight when a boss picks a class. The order
 * matters for loot: a boss drops loot of its own class and the ones below.
 */
@Serializable
data class Classes(
  val common: BossClass = BossClass(
    key = "common", health = 1.0, damage = 1.0, experience = 1.5, defense = 1.30, toughness = 1.05, color = "WHITE", chance = 63.0,
    abilityThresholds = AbilityThresholds(28, 34, 45, 35, 38, 70, 67, 30, 75),
  ),
  val uncommon: BossClass = BossClass(
    key = "uncommon", health = 1.3, damage = 1.1, experience = 2.0, defense = 1.42, toughness = 1.13, color = "YELLOW", chance = 28.0,
    abilityThresholds = AbilityThresholds(20, 30, 40, 25, 32, 60, 50, 28, 65),
  ),
  val rare: BossClass = BossClass(
    key = "rare", health = 1.6, damage = 1.3, experience = 3.0, defense = 1.56, toughness = 1.23, color = "AQUA", chance = 9.0,
    abilityThresholds = AbilityThresholds(18, 28, 35, 22, 28, 55, 45, 25, 60),
  ),
  val epic: BossClass = BossClass(
    key = "epic", health = 2.2, damage = 1.7, experience = 4.0, defense = 1.83, toughness = 1.42, color = "DARK_PURPLE", chance = 3.0,
    abilityThresholds = AbilityThresholds(12, 22, 30, 18, 26, 45, 40, 20, 50),
  ),
  val legendary: BossClass = BossClass(
    key = "legendary", health = 3.0, damage = 2.2, experience = 6.0, defense = 2.18, toughness = 1.67, color = "GOLD", chance = 2.0,
    abilityThresholds = AbilityThresholds(8, 18, 25, 14, 18, 35, 28, 14, 40),
  ),
  val mythic: BossClass = BossClass(
    key = "mythic", health = 4.0, damage = 2.8, experience = 8.0, defense = 2.62, toughness = 1.97, color = "RED", chance = 1.0,
    abilityThresholds = AbilityThresholds(5, 12, 18, 10, 14, 30, 25, 10, 30),
  ),
) {
  /** From Common to Mythic, each under the key it has in this file whatever its `key` field says. */
  val all: List<BossClass>
    get() = listOf(common, uncommon, rare, epic, legendary, mythic).zip(KEYS) { c, key -> c.copy(key = key) }

  fun byKey(key: String): BossClass? = all.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }

  /** 0 for Common up to 5 for Mythic; -1 for a key that isn't a class. */
  fun rank(key: String) = KEYS.indexOf(key.trim().lowercase())

  companion object {
    val KEYS = listOf("common", "uncommon", "rare", "epic", "legendary", "mythic")

    fun parse(text: String, warn: (String) -> Unit = {}): Classes = Yamls.decode(serializer(), text, warn)
  }
}

@Serializable
data class BossClass(
  val key: String = "common",
  val health: Double = 1.0,
  val damage: Double = 1.0,
  val experience: Double = 1.0,
  val defense: Double = 1.0,
  val toughness: Double = 1.0,
  /** A color name (WHITE, GOLD, DARK_PURPLE, …) or `#RRGGBB`. */
  val color: String = "WHITE",
  val chance: Double = 0.0,
  /** 1.x's name for the class, shown to everyone. Leave it out to use `classes.<key>` from the language files. */
  @SerialName("display_name") val displayName: String? = null,
  /** The boss level each ability starts at. */
  @SerialName("ability_thresholds") val abilityThresholds: AbilityThresholds = AbilityThresholds(),
)

@Serializable
data class AbilityThresholds(
  val lightning: Int = 100,
  val summoning: Int = 100,
  val regeneration: Int = 100,
  @SerialName("fire_ring") val fireRing: Int = 100,
  @SerialName("web_ring") val webRing: Int = 100,
  @SerialName("teleport_strike") val teleportStrike: Int = 100,
  @SerialName("meteor_shower") val meteorShower: Int = 100,
  @SerialName("arrow_arc") val arrowArc: Int = 100,
  @SerialName("arrow_rain") val arrowRain: Int = 100,
)
