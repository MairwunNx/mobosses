@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bukkit.entity.EntityType
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer
import ru.mairwunnx.mobosses.serializers.SoundSerializer

@Serializable class MinionsConfigurationModel(val minions: List<Minion>) {
  @Serializable class Minion(
    @SerialName("summoner") val summoner: @Contextual EntityType,
    @SerialName("entity") val entity: @Contextual EntityType,
    @SerialName("name") val name: String,
  )

  companion object {
    fun default() = MinionsConfigurationModel(
      minions = listOf(
        Minion(summoner = EntityType.ZOMBIE, entity = EntityType.ZOMBIE, name = "Зомбик"),
        Minion(summoner = EntityType.SPIDER, entity = EntityType.CAVE_SPIDER, name = "Пауканчик"),
        Minion(summoner = EntityType.SKELETON, entity = EntityType.SKELETON, name = "Скелетончик"),
        Minion(summoner = EntityType.ENDERMAN, entity = EntityType.ENDERMITE, name = "Эндерчервь"),
        Minion(summoner = EntityType.PIGLIN, entity = EntityType.PIGLIN, name = "Свиняшка"),
        Minion(summoner = EntityType.WITHER_SKELETON, entity = EntityType.WITHER_SKELETON, name = "Скелетик"),
        Minion(summoner = EntityType.PILLAGER, entity = EntityType.PILLAGER, name = "Разбойник"),
        Minion(summoner = EntityType.EVOKER, entity = EntityType.VINDICATOR, name = "Патологоанатом"),
        Minion(summoner = EntityType.WITCH, entity = EntityType.WITCH, name = "Ученик хогвартса"),
      )
    )
  }
}