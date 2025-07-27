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
import org.bukkit.entity.EntityType

@Serializable class MinionsConfigurationModel(val minions: List<Minion>) {
  @Serializable class Minion(
    @SerialName("entity") val entity: @Contextual EntityType,
    @SerialName("name") val name: String,
    @SerialName("health") val health: Double,
    @SerialName("damage") val damage: Double
  )

  companion object {
    fun default() = MinionsConfigurationModel(
      minions = listOf(
        Minion(entity = EntityType.ZOMBIE, name = "Зомбик", health = 10.0, damage = 1.5),
        Minion(entity = EntityType.CAVE_SPIDER, name = "Пауканчик", health = 8.0, damage = 2.0),
        Minion(entity = EntityType.SKELETON, name = "Скелетончик", health = 12.0, damage = 3.0),
        Minion(entity = EntityType.ENDERMITE, name = "Эндермит", health = 6.0, damage = 1.0),
        Minion(entity = EntityType.PIGLIN, name = "Пиглинок", health = 14.0, damage = 2.5),
        Minion(entity = EntityType.WITHER_SKELETON, name = "Скелетик", health = 16.0, damage = 4.0),
        Minion(entity = EntityType.PILLAGER, name = "Разбойник", health = 18.0, damage = 3.5),
      )
    )
  }
}