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
    @SerialName("health") val health: Double,
    @SerialName("damage") val damage: Double,
    @SerialName("experience") val experience: Double,
    @SerialName("color") val color: @Contextual ChatColor,
    @SerialName("chance") val chance: Double,
    @SerialName("display_name") val displayName: String
  )

  companion object {
    fun default() = ClassesConfigurationModel(
      common = BossClass(
        health = 1.0,
        damage = 1.0,
        experience = 1.5,
        color = ChatColor.WHITE,
        chance = 55.0,
        displayName = "Common"
      ),
      uncommon = BossClass(
        health = 1.3,
        damage = 1.1,
        experience = 2.0,
        color = ChatColor.YELLOW,
        chance = 25.0,
        displayName = "Uncommon"
      ),
      rare = BossClass(
        health = 1.6,
        damage = 1.3,
        experience = 3.0,
        color = ChatColor.AQUA,
        chance = 12.0,
        displayName = "Rare"
      ),
      epic = BossClass(
        health = 2.2,
        damage = 1.7,
        experience = 4.0,
        color = ChatColor.DARK_PURPLE,
        chance = 5.0,
        displayName = "Epic"
      ),
      legendary = BossClass(
        health = 3.0,
        damage = 2.2,
        experience = 6.0,
        color = ChatColor.GOLD,
        chance = 2.0,
        displayName = "Legendary"
      ),
      mythic = BossClass(
        health = 4.0,
        damage = 2.8,
        experience = 8.0,
        color = ChatColor.RED,
        chance = 1.0,
        displayName = "Mythic"
      )
    )
  }
}