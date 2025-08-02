@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.ChatColor
import kotlinx.serialization.UseSerializers
import ru.mairwunnx.mobosses.serializers.SoundSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer

@Serializable class ClassesConfigurationModel(
  @SerialName("common") val common: BossClass,
  @SerialName("uncommon") val uncommon: BossClass,
  @SerialName("rare") val rare: BossClass,
  @SerialName("epic") val epic: BossClass,
  @SerialName("legendary") val legendary: BossClass,
  @SerialName("mythic") val mythic: BossClass
) {
  @Serializable class BossClass(
    @SerialName("key") val key: String,
    @SerialName("health") val health: Double,
    @SerialName("damage") val damage: Double,
    @SerialName("experience") val experience: Double,
    @SerialName("defense") val defense: Double,
    @SerialName("toughness") val toughness: Double,
    @SerialName("color") val color: @Contextual ChatColor,
    @SerialName("chance") val chance: Double,
    @SerialName("display_name") val displayName: String
  )

  companion object {
    fun default() = ClassesConfigurationModel(
      common = BossClass(
        key = "common",
        health = 1.0,
        damage = 1.0,
        experience = 1.5,
        defense = 1.30,
        toughness = 1.05,
        color = ChatColor.WHITE,
        chance = 55.0,
        displayName = "Common"
      ),
      uncommon = BossClass(
        key = "uncommon",
        health = 1.3,
        damage = 1.1,
        experience = 2.0,
        defense = 1.42,
        toughness = 1.13,
        color = ChatColor.YELLOW,
        chance = 25.0,
        displayName = "Uncommon"
      ),
      rare = BossClass(
        key = "rare",
        health = 1.6,
        damage = 1.3,
        experience = 3.0,
        defense = 1.56,
        toughness = 1.23,
        color = ChatColor.AQUA,
        chance = 12.0,
        displayName = "Rare"
      ),
      epic = BossClass(
        key = "epic",
        health = 2.2,
        damage = 1.7,
        experience = 4.0,
        defense = 1.83,
        toughness = 1.42,
        color = ChatColor.DARK_PURPLE,
        chance = 5.0,
        displayName = "Epic"
      ),
      legendary = BossClass(
        key = "legendary",
        health = 3.0,
        damage = 2.2,
        experience = 6.0,
        defense = 2.18,
        toughness = 1.67,
        color = ChatColor.GOLD,
        chance = 2.0,
        displayName = "Legendary"
      ),
      mythic = BossClass(
        key = "mythic",
        health = 4.0,
        damage = 2.8,
        experience = 8.0,
        defense = 2.62,
        toughness = 1.97,
        color = ChatColor.RED,
        chance = 1.0,
        displayName = "Mythic"
      )
    )
  }
}