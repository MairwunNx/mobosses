package ru.mairwunnx.mobosses.modcore.net

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import ru.mairwunnx.mobosses.config.edit.Field

/**
 * What a player's game with Mo’Bosses and a server with it say to each other, beyond what the game itself says. A
 * vanilla game never asks and never hears any of it: the server answers only a game that asked, on a channel the
 * game said it has, and both loaders keep the channel optional.
 */
object Payloads {
  private fun <T : CustomPacketPayload> type(path: String) = CustomPacketPayload.Type<T>(Identifier.fromNamespaceAndPath("mobosses", path))

  private fun FriendlyByteBuf.writeStrings(values: List<String>) {
    writeVarInt(values.size)
    values.forEach(::writeUtf)
  }

  private fun FriendlyByteBuf.readStrings(): List<String> = List(readVarInt()) { readUtf() }

  /** The settings screen opens: the game asks for the server's files. */
  class SettingsWanted private constructor() : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<SettingsWanted>("settings_wanted")
      /** The one there is: the game's codec for a message without content sends only this instance. */
      val INSTANCE = SettingsWanted()
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingsWanted> = StreamCodec.unit(INSTANCE)
    }
  }

  /** One file's values for the screen. */
  class SettingsFile(val name: String, val fields: List<Field>)

  /**
   * The server's files for the screen, or none when the player may not change them ([allowed] false); [message] says
   * what the last change did, empty after a plain look, and [failed] whether any of it was refused.
   */
  class Settings(val allowed: Boolean, val message: String, val failed: Boolean, val files: List<SettingsFile>) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<Settings>("settings")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, Settings> = CustomPacketPayload.codec({ value, out ->
        out.writeBoolean(value.allowed)
        out.writeUtf(value.message)
        out.writeBoolean(value.failed)
        out.writeVarInt(value.files.size)
        value.files.forEach { file ->
          out.writeUtf(file.name)
          out.writeVarInt(file.fields.size)
          file.fields.forEach { field ->
            out.writeStrings(field.path)
            out.writeEnum(field.kind)
            out.writeUtf(field.value)
            out.writeUtf(field.default)
            out.writeUtf(field.note)
          }
        }
      }, { input ->
        val allowed = input.readBoolean()
        val message = input.readUtf()
        val failed = input.readBoolean()
        val files = List(input.readVarInt()) {
          val name = input.readUtf()
          SettingsFile(name, List(input.readVarInt()) {
            Field(input.readStrings(), input.readEnum(Field.Kind::class.java), input.readUtf(), input.readUtf(), input.readUtf())
          })
        }
        Settings(allowed, message, failed, files)
      })
    }
  }

  /** One value of a file, by its path, as the player typed it. */
  class Change(val path: List<String>, val value: String)

  /** The player changes values of one file, as the screen's Done sends them together. */
  class SettingsChanged(val file: String, val changes: List<Change>) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      /** More at once than any screen sends. */
      const val MAX = 256

      val TYPE = type<SettingsChanged>("settings_changed")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingsChanged> = CustomPacketPayload.codec({ value, out ->
        out.writeUtf(value.file)
        out.writeVarInt(value.changes.size)
        value.changes.forEach { change ->
          out.writeStrings(change.path)
          out.writeUtf(change.value)
        }
      }, { input ->
        val file = input.readUtf()
        val count = input.readVarInt().coerceIn(0, MAX)
        SettingsChanged(file, List(count) { Change(input.readStrings(), input.readUtf()) })
      })
    }
  }

  /** One entry of the loot table, for the recipe viewers: the item as it drops, and lines that say from whom, in the player's language. */
  class LootPage(val stack: ItemStack, val lines: List<Component>)

  /** The loot table for a player's recipe viewer: sent once they join, and again after a reload. */
  class LootPages(val pages: List<LootPage>) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<LootPages>("loot_pages")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, LootPages> = CustomPacketPayload.codec({ value, out ->
        out.writeVarInt(value.pages.size)
        value.pages.forEach { page ->
          ItemStack.OPTIONAL_STREAM_CODEC.encode(out, page.stack)
          out.writeVarInt(page.lines.size)
          page.lines.forEach { ComponentSerialization.STREAM_CODEC.encode(out, it) }
        }
      }, { input ->
        LootPages(List(input.readVarInt()) {
          LootPage(ItemStack.OPTIONAL_STREAM_CODEC.decode(input), List(input.readVarInt()) { ComponentSerialization.STREAM_CODEC.decode(input) })
        })
      })
    }
  }
}
