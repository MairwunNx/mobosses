package ru.mairwunnx.mobosses.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.enchantments.Enchantment

object EnchantmentSerializer : KSerializer<Enchantment> {
  override val descriptor = PrimitiveSerialDescriptor("Enchantment", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Enchantment) = encoder.encodeString(value.key.key)
  override fun deserialize(decoder: Decoder) = Enchantment.getByName(decoder.decodeString().uppercase()) ?: Enchantment.UNBREAKING!!
}