package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffectType

object PotionEffectTypeSerializer : KSerializer<PotionEffectType> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PotionEffectType", PrimitiveKind.STRING)

  private val default: PotionEffectType = PotionEffectType.SLOWNESS

  private val aliases: Map<String, PotionEffectType> = buildMap {
    // По key.key (minecraft:poison -> "poison")
    PotionEffectType.values().forEach { type ->
      put(type.key.key.uppercase(), type)
      // Полный ключ "minecraft:poison"
      put(type.key.toString().uppercase(), type)
      @Suppress("DEPRECATION")
      put(type.name.uppercase(), type)
    }
  }

  override fun serialize(encoder: Encoder, value: PotionEffectType) {
    // Сериализуем компактно по key.key (например, "poison")
    encoder.encodeString(value.key.key.uppercase())
  }

  override fun deserialize(decoder: Decoder): PotionEffectType {
    val raw = decoder.decodeString().trim()
    val upper = raw.uppercase()

    // Пробуем по предкэшированным алиасам
    aliases[upper]?.let { return it }

    // Попробуем распарсить как NamespacedKey
    runCatching {
      val key = if (raw.contains(':')) NamespacedKey.fromString(raw) else NamespacedKey.minecraft(raw.lowercase())
      if (key != null) {
        PotionEffectType.values().firstOrNull { it.key == key }?.let { return it }
      }
    }

    // Последняя попытка через устаревший getByName
    @Suppress("DEPRECATION")
    PotionEffectType.getByName(upper)?.let { return it }

    // Fallback
    return default
  }
}