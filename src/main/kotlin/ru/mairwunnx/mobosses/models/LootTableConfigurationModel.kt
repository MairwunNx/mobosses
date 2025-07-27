@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import ru.mairwunnx.mobosses.serializers.SoundSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import ru.mairwunnx.mobosses.models.types.IntRangeSpec

@Serializable class LootTableConfigurationModel(
  @SerialName("level_loot") val levelLoot: List<LootItem>,
  @SerialName("loot_settings") val lootSettings: LootSettings
) {
  @Serializable class LootItem(
    @SerialName("material") val material: @Contextual Material,
    @SerialName("mobclass") val mobclass: String,
    @SerialName("min_level") val minLevel: Int,
    @SerialName("max_level") val maxLevel: Int,
    @SerialName("amount") val amount: IntRangeSpec,
    @SerialName("experience_multiplier") val experienceMultiplier: Double,
    @SerialName("weight") val weight: Double,
    @SerialName("random_durability") val randomDurability: Boolean? = null,
    @SerialName("enchantments") val enchantments: Map<@Contextual Enchantment, IntRangeSpec>? = null,
    @SerialName("itemClass") val itemClass: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("lore") val lore: List<String>? = null
  )

  @Serializable class LootSettings(
    @SerialName("max_experience_multiplier") val maxExperienceMultiplier: Double,
    @SerialName("per_level_drop_factor") val perLevelDropFactor: Double
  )

  companion object {
    fun default() = LootTableConfigurationModel(levelLoot = listOf(), lootSettings = LootSettings(maxExperienceMultiplier = 10.0, 0.05))
  }
}