package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.ChatColor

object ChatColorSerializer : KSerializer<ChatColor> {
  override val descriptor = PrimitiveSerialDescriptor("ChatColor", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: ChatColor) = encoder.encodeString(value.name)
  override fun deserialize(decoder: Decoder) = runCatching { ChatColor.valueOf(decoder.decodeString().uppercase()) }.getOrElse { ChatColor.WHITE }
}