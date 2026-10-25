package ru.mairwunnx.mobosses.modcore.text

import com.mojang.serialization.JsonOps
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.core.HolderLookup
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.Component as Vanilla
import net.kyori.adventure.text.Component as Text

/**
 * Adventure's texts, which the language files render, as the game's own components: through the JSON both speak, with
 * the server's registries for what the JSON names (items in hover texts, fonts). A text the game can't read falls back
 * to its plain words, so a broken custom message still says something.
 */
object Chat {
  private val gson = GsonComponentSerializer.gson()

  fun vanilla(text: Text, registries: HolderLookup.Provider): Vanilla {
    val json = gson.serializeToTree(text)
    return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).result()
      .orElseGet { Vanilla.literal(PlainTextComponentSerializer.plainText().serialize(text)) }
  }
}
