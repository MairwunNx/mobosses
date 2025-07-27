package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Material

object MaterialSerializer : KSerializer<Material> {
  override val descriptor = PrimitiveSerialDescriptor("Material", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Material) = encoder.encodeString(value.name)
  override fun deserialize(decoder: Decoder) = runCatching { Material.valueOf(decoder.decodeString().uppercase()) }.getOrElse { Material.STONE }
}