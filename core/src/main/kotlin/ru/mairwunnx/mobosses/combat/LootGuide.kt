package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.LootTable
import kotlin.math.ceil

/**
 * `loottable.yml` as a helper reads it out to players, the same on every platform: each entry's chance to be the item
 * a boss drops, how many come at a level, which entries name an item someone asks about, and the named drops gathered
 * by their `id`. It reads the rules [LootRules] drops by, so the numbers match what bosses really drop.
 */
object LootGuide {
  /** The chance, 0 to 100, of each entry of [pool] to be one item a boss drops, best first. */
  fun <T> chances(pool: List<Pair<T, Double>>): List<Pair<T, Double>> {
    val total = pool.sumOf { it.second }.takeIf { it > 0.0 } ?: return emptyList()
    return pool.map { (entry, weight) -> entry to weight * 100.0 / total }.sortedByDescending { it.second }
  }

  /** The fewest and the most of [item] a boss of [level] drops at once, up to [maxStack]: [LootRules.amount]'s bounds. */
  fun amounts(item: LootTable.Item, settings: LootTable.LootSettings, level: Int, maxStack: Int): IntRange {
    val factor = 1.0 + (item.perLevel ?: settings.perLevelDropFactor) * (level - item.minLevel).coerceAtLeast(0)
    val cap = maxStack.coerceAtLeast(1)
    val low = ceil(item.amount.first * factor).toInt().coerceIn(1, cap)
    return low..ceil(item.amount.last * factor).toInt().coerceIn(low, cap)
  }

  /** `3` or `2–6`. */
  fun text(amounts: IntRange) = if (amounts.first == amounts.last) "${amounts.first}" else "${amounts.first}–${amounts.last}"

  /**
   * Whether [query] names an item: each of its words is part of its [id], with or without `minecraft:`, or of one of its
   * [names], in any case, with `_` as a space.
   */
  fun matches(query: String, id: String, names: Collection<String>): Boolean {
    val words = fold(query).split(' ').filter { it.isNotEmpty() }.ifEmpty { return false }
    return (names + id).any { name -> fold(name).let { text -> words.all(text::contains) } }
  }

  private fun fold(text: String) = text.trim().lowercase().removePrefix("minecraft:").replace('_', ' ').replace('ё', 'е')

  /** The game's languages a Mo’Bosses language file stands for: `ru` for `ru_ru`, `pt` for `pt_br` and `pt_pt`. */
  fun locales(code: String): List<String> {
    val normal = code.trim().lowercase().replace('-', '_')
    if ('_' in normal) return listOf(normal)
    return GAME_LOCALES[normal] ?: listOf("${normal}_$normal")
  }

  private val GAME_LOCALES = mapOf(
    "en" to listOf("en_us", "en_gb"),
    "pt" to listOf("pt_br", "pt_pt"),
    "es" to listOf("es_es", "es_mx"),
    "uk" to listOf("uk_ua"),
    "zh" to listOf("zh_cn"),
    "ja" to listOf("ja_jp"),
    "ko" to listOf("ko_kr"),
    "cs" to listOf("cs_cz"),
    "sv" to listOf("sv_se"),
  )

  /** One named drop, gathered from every entry with its [id]. */
  class Named(
    val id: String,
    /** The item underneath, as the first entry gives it. */
    val material: String,
    /** The lowest boss class that drops it. */
    val fromClass: String,
    val minLevel: Int,
    val maxLevel: Int,
    /** The boss types it belongs to; empty when any boss drops it. */
    val types: List<String>,
    val itemClass: String?,
  )

  /** The entries with an `id`, one per id, in the table's order. */
  fun named(items: List<LootTable.Item>, classes: Classes): List<Named> = items.filter { !it.id.isNullOrBlank() }.groupBy { it.id!!.trim() }.map { (id, entries) ->
    val lowest = entries.minBy { classes.rank(it.mobclass).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }
    val types = if (entries.any { it.types.isEmpty() }) emptyList() else entries.flatMap { it.types }.map { it.trim().lowercase().removePrefix("minecraft:") }.distinct()
    Named(id, entries.first().material.trim().lowercase().removePrefix("minecraft:"), lowest.mobclass.trim().lowercase(), entries.minOf { it.minLevel }, entries.maxOf { it.maxLevel }, types, entries.firstNotNullOfOrNull { it.itemClass?.lowercase() })
  }

  /** What a named drop is, in English, for a helper: where it comes from, then its [lore], at most 300 characters. */
  fun describe(named: Named, lore: String?): String {
    val types = if (named.types.isEmpty()) "" else " of type ${named.types.joinToString(", ")}"
    val rarity = named.itemClass?.let { "A $it Mo’Bosses drop" } ?: "A Mo’Bosses drop"
    val where = "$rarity: bosses$types of class ${named.fromClass} and up, levels ${named.minLevel}–${named.maxLevel}, drop it."
    val text = lore?.trim()?.takeIf { it.isNotEmpty() }?.let { "$where $it" } ?: where
    return if (text.length <= 300) text else text.take(299).trimEnd() + "…"
  }
}
