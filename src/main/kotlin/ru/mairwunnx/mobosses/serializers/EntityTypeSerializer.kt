package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.entity.EntityType

object EntityTypeSerializer : KSerializer<EntityType> {
  override val descriptor = PrimitiveSerialDescriptor("EntityType", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: EntityType) = encoder.encodeString(value.name)
  override fun deserialize(decoder: Decoder) = runCatching { EntityType.valueOf(decoder.decodeString().uppercase()) }.getOrElse { EntityType.ZOMBIE }
}