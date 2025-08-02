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
  @Serializable class AbilityThresholds(
    @SerialName("lightning") val lightning: Int,
    @SerialName("summoning") val summoning: Int,
    @SerialName("regeneration") val regeneration: Int,
    @SerialName("fire_ring") val fireRing: Int,
    @SerialName("web_ring") val webRing: Int,
    @SerialName("teleport_strike") val teleportStrike: Int,
    @SerialName("meteor_shower") val meteorShower: Int,
    @SerialName("arrow_arc") val arrowArc: Int,
    @SerialName("arrow_rain") val arrowRain: Int
  )

  @Serializable class BossClass(
    @SerialName("key") val key: String,
    @SerialName("health") val health: Double,
    @SerialName("damage") val damage: Double,
    @SerialName("experience") val experience: Double,
    @SerialName("defense") val defense: Double,
    @SerialName("toughness") val toughness: Double,
    @SerialName("color") val color: @Contextual ChatColor,
    @SerialName("chance") val chance: Double,
    @SerialName("display_name") val displayName: String,
    @SerialName("ability_thresholds") val abilityThresholds: AbilityThresholds
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
        displayName = "Common",
        abilityThresholds = AbilityThresholds(
          lightning = 20,
          summoning = 30,
          regeneration = 45,
          fireRing = 25,
          webRing = 35,
          teleportStrike = 60,
          meteorShower = 50,
          arrowArc = 28,
          arrowRain = 65
        )
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
        displayName = "Uncommon",
        abilityThresholds = AbilityThresholds(
          lightning = 18,
          summoning = 28,
          regeneration = 40,
          fireRing = 22,
          webRing = 32,
          teleportStrike = 55,
          meteorShower = 45,
          arrowArc = 25,
          arrowRain = 60
        )
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
        displayName = "Rare",
        abilityThresholds = AbilityThresholds(
          lightning = 15,
          summoning = 25,
          regeneration = 35,
          fireRing = 20,
          webRing = 28,
          teleportStrike = 50,
          meteorShower = 40,
          arrowArc = 22,
          arrowRain = 55
        )
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
        displayName = "Epic",
        abilityThresholds = AbilityThresholds(
          lightning = 12,
          summoning = 20,
          regeneration = 30,
          fireRing = 16,
          webRing = 24,
          teleportStrike = 42,
          meteorShower = 35,
          arrowArc = 18,
          arrowRain = 48
        )
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
        displayName = "Legendary",
        abilityThresholds = AbilityThresholds(
          lightning = 8,
          summoning = 15,
          regeneration = 25,
          fireRing = 12,
          webRing = 18,
          teleportStrike = 35,
          meteorShower = 28,
          arrowArc = 14,
          arrowRain = 40
        )
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
        displayName = "Mythic",
        abilityThresholds = AbilityThresholds(
          lightning = 5,
          summoning = 10,
          regeneration = 18,
          fireRing = 8,
          webRing = 12,
          teleportStrike = 25,
          meteorShower = 20,
          arrowArc = 10,
          arrowRain = 30
        )
      )
    )
  }
}