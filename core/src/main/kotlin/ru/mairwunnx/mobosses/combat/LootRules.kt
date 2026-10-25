package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.ceil
import kotlin.random.Random

/**
 * How `loottable.yml` drops, the same on every platform: which entries a boss may drop and at what weight, how many
 * come, how worn, and when pity owes a rare one. A boss drops loot of its class and the classes below, at falling odds
 * the further down, never above; the survival bonus tips the odds toward rare and better items.
 */
object LootRules {
  /** How much the survival bonus tips each rare item class up: rarer ones more. */
  private val LUCK_TIER = mapOf("rare" to 1.0, "epic" to 1.5, "legendary" to 2.0, "mythic" to 2.5)

  /** How far toward `max_durability` each class pulls. */
  private val WEAR_BIAS = mapOf("common" to 0.0, "uncommon" to 0.3, "rare" to 0.6, "epic" to 1.0, "legendary" to 1.4, "mythic" to 2.0)

  /**
   * An entry's weight for a boss, or null when it can't drop it: its class above the boss's, its level range missing
   * the boss's level, or, with [theirs] false, an entry of other boss types. [luck] tips rare ones up.
   */
  fun weight(item: LootTable.Item, classes: Classes, bossClass: String, level: Int, theirs: Boolean, luck: Double = 0.0): Double? {
    val bossRank = classes.rank(bossClass)
    val itemRank = classes.rank(item.mobclass)
    if (!theirs || itemRank < 0 || itemRank > bossRank || level !in item.minLevel..item.maxLevel) return null
    return item.weight * crossClass(bossRank - itemRank) * (1.0 + luck.coerceAtLeast(0.0) * (LUCK_TIER[item.itemClass?.lowercase()] ?: 0.0))
  }

  /** Whether an entry is rare or better: announced, and what pity owes. */
  fun isRare(item: LootTable.Item) = item.itemClass?.lowercase() in LUCK_TIER

  /** How many of an entry drop at [level], up to [maxStack]. */
  fun amount(item: LootTable.Item, settings: LootTable.LootSettings, level: Int, maxStack: Int, rng: Random = Random): Int {
    val factor = 1.0 + (item.perLevel ?: settings.perLevelDropFactor) * (level - item.minLevel).coerceAtLeast(0)
    return ceil(item.amount.random(rng) * factor).toInt().coerceIn(1, maxStack.coerceAtLeast(1))
  }

  /**
   * The damage a worn item comes with, out of [maxDurability]: the share left lies between `min_durability` and
   * `max_durability`, pulled toward the top for better classes; null when the item comes new.
   */
  fun damage(settings: Settings.RandomDurability, item: LootTable.Item, maxDurability: Int, rng: Random = Random): Int? {
    if (!item.randomDurability || !settings.enabled || maxDurability <= 0) return null
    val min = settings.minDurability.coerceIn(0.1, 1.0)
    val max = settings.maxDurability.coerceIn(min, 1.0)
    val left = Rolls.biasedInRange(min, max, WEAR_BIAS[item.itemClass?.lowercase()] ?: 0.0, rng)
    return (maxDurability * (1.0 - left)).toInt().coerceIn(0, maxDurability - 1)
  }

  /** The drops without a rare item after which pity gives one: a new count each time, so it shows no pattern. */
  fun pityAt(pity: Settings.Pity, rng: Random = Random): Int {
    val low = pity.minDrops.coerceAtLeast(1)
    return rng.nextInt(low, pity.maxDrops.coerceAtLeast(low) + 1)
  }

  /** The odds a boss drops loot of a lower class, by how many classes lower. */
  fun crossClass(steps: Int) = when (steps) {
    0 -> 1.0
    1 -> 0.6
    2 -> 0.3
    3 -> 0.15
    4 -> 0.08
    else -> 0.03
  }
}
