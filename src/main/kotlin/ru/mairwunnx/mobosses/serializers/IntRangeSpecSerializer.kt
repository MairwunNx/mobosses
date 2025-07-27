package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.mairwunnx.mobosses.models.types.IntRangeSpec
import kotlin.math.max
import kotlin.math.min

object IntRangeSpecSerializer : KSerializer<IntRangeSpec> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntRangeSpec", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: IntRangeSpec) = encoder.encodeString(value.toString())

  override fun deserialize(decoder: Decoder): IntRangeSpec {
    val raw = runCatching { decoder.decodeString() }
      .getOrElse { runCatching { decoder.decodeInt().toString() }.getOrElse { "1" } }
      .trim()

    return if ('-' in raw) {
      val parts = raw.split('-', limit = 2).map { it.trim().toIntOrNull() ?: 1 }
      val a = parts.getOrElse(0) { 1 }; val b = parts.getOrElse(1) { a }
      IntRangeSpec(min(a, b), max(a, b))
    } else {
      val v = raw.toIntOrNull() ?: 1
      IntRangeSpec(v, v)
    }
  }
}