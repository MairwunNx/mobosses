package ru.mairwunnx.mobosses.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** An amount or level range: `3` or `"2-6"`. */
object RangeSerializer : KSerializer<IntRange> {
  override val descriptor = PrimitiveSerialDescriptor("IntRange", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: IntRange) =
    encoder.encodeString(if (value.first == value.last) "${value.first}" else "${value.first}-${value.last}")

  override fun deserialize(decoder: Decoder): IntRange = parse(decoder.decodeString())
    ?: throw SerializationException("expected a number or a range like 2-6")

  fun parse(text: String): IntRange? {
    val parts = text.trim().split('-', limit = 2).map { it.trim().toIntOrNull() ?: return null }
    val (a, b) = if (parts.size == 1) parts[0] to parts[0] else parts[0] to parts[1]
    return minOf(a, b)..maxOf(a, b)
  }
}
