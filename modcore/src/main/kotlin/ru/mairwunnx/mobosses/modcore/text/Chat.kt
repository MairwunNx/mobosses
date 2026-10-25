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

  /** The game's text as Adventure's, for a placeholder another mod filled; its plain words when the JSON won't read. */
  fun adventure(text: Vanilla, registries: HolderLookup.Provider): Text =
    ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), text).result()
      .map { runCatching { gson.deserializeFromTree(it) }.getOrNull() }.orElse(null) ?: Text.text(text.string)

  fun vanilla(text: Text, registries: HolderLookup.Provider): Vanilla {
    val json = gson.serializeToTree(text)
    return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).result()
      .orElseGet { Vanilla.literal(PlainTextComponentSerializer.plainText().serialize(text)) }
  }
}
