package ru.mairwunnx.mobosses.modcore.net

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
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
  class SettingsWanted : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<SettingsWanted>("settings_wanted")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingsWanted> = StreamCodec.unit(SettingsWanted())
    }
  }

  /** One file's values for the screen. */
  class SettingsFile(val name: String, val fields: List<Field>)

  /**
   * The server's files for the screen, or none when the player may not change them ([allowed] false); [message] says
   * what the last change did, empty after a plain look.
   */
  class Settings(val allowed: Boolean, val message: String, val files: List<SettingsFile>) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<Settings>("settings")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, Settings> = CustomPacketPayload.codec({ value, out ->
        out.writeBoolean(value.allowed)
        out.writeUtf(value.message)
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
        val files = List(input.readVarInt()) {
          val name = input.readUtf()
          SettingsFile(name, List(input.readVarInt()) {
            Field(input.readStrings(), input.readEnum(Field.Kind::class.java), input.readUtf(), input.readUtf(), input.readUtf())
          })
        }
        Settings(allowed, message, files)
      })
    }
  }

  /** The player changes one value of one file. */
  class SettingChanged(val file: String, val path: List<String>, val value: String) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
      val TYPE = type<SettingChanged>("setting_changed")
      val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingChanged> = CustomPacketPayload.codec({ value, out ->
        out.writeUtf(value.file)
        out.writeStrings(value.path)
        out.writeUtf(value.value)
      }, { input -> SettingChanged(input.readUtf(), input.readStrings(), input.readUtf()) })
    }
  }
}
