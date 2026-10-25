package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `difficulty` in `config.yml`: how the game's difficulty grades new bosses. Each difficulty has one modifier, and
 * each part of a boss moves by its own weight of it; hard is 1, the difficulty the balance was built for. Peaceful
 * makes no bosses whatever this says.
 */
@Serializable
data class DifficultySettings(
  /** False plays every difficulty but peaceful as hard. */
  val enabled: Boolean = true,
  val modifiers: Modifiers = Modifiers(),
  val weights: Weights = Weights(),
) {
  @Serializable
  data class Modifiers(
    val easy: Double = 0.7333333337,
    val normal: Double = 0.8777777773,
    val hard: Double = 1.0,
    /** A hardcore world's, which the game keeps on hard. */
    val hardcore: Double = 1.2333333339,
  )

  /** How much of the modifier each part takes: 0 leaves it, 1 takes all of it. */
  @Serializable
  data class Weights(
    val health: Double = 1.0,
    /** Melee, and ranged through `formula.ranged_damage`. */
    val damage: Double = 1.0,
    /** Armor and toughness. */
    val armor: Double = 0.5,
    /** An ability's cooldown divides by its part. */
    val cooldowns: Double = 0.5,
    /** How many minions a summon brings. */
    val minions: Double = 0.5,
    @SerialName("spawn_chance") val spawnChance: Double = 0.5,
    /** The vanilla experience a boss drops. */
    val experience: Double = 1.0,
  )
}
