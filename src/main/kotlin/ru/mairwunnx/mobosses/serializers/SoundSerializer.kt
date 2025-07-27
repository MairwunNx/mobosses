package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Sound

object SoundSerializer : KSerializer<Sound> {
  override val descriptor = PrimitiveSerialDescriptor("Sound", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Sound) = encoder.encodeString(value.name)
  override fun deserialize(decoder: Decoder) = runCatching { Sound.valueOf(decoder.decodeString().uppercase()) }.getOrElse { Sound.ENTITY_GENERIC_HURT }
}