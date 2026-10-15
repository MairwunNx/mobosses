@file:UseSerializers(RangeSerializer::class)

package ru.mairwunnx.mobosses.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

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
