package ru.mairwunnx.mobosses.modcore.client

import net.minecraft.client.Minecraft
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.BundledFiles
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.edit.EditableFile
import ru.mairwunnx.mobosses.config.edit.Editor
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Where the settings screen's values come from and go to. In a world on a server with Mo’Bosses as a mod, the
 * server's own files over the channel ([Remote]), its world's owner's in singleplayer too; in the main menu, the
 * game's `config/mobosses/`, which every world opened afterwards reads ([Local]).
 */
sealed interface SettingsSource {
  /** What the screen shows: the files when the player may change them, and a line about where they are. */
  class State(val allowed: Boolean, val note: Message?, val message: String, val failed: Boolean, val files: List<Payloads.SettingsFile>)

  fun load(done: (State) -> Unit)

  fun save(file: String, changes: List<Payloads.Change>, done: (State) -> Unit)

  object Local : SettingsSource {
    private val folder get() = Mod.loader.config.resolve("mobosses").toFile()

    private val quiet = Log(object : Log.Sink {
      override fun info(message: String) {}
      override fun warn(message: String) {}
      override fun error(message: String, cause: Throwable?) {}
    }) { false }

    private fun state(message: String, failed: Boolean = false): State {
      val shipped = BundledFiles.parse(MoBosses.bundled(BundledFiles.LIST))
      (Configs.FILES + Languages.BUNDLED.map { "lang/$it.yml" }).forEach { BundledFiles.copy(folder, it, MoBosses::bundled, shipped, quiet) }
      return State(true, Message.SETTINGS_LOCAL, message, failed, EditableFile.ALL.map { Payloads.SettingsFile(it.name, it.fields(folder.resolve(it.name).readText())) })
    }

    override fun load(done: (State) -> Unit) = done(state(""))

    override fun save(file: String, changes: List<Payloads.Change>, done: (State) -> Unit) {
      val editable = EditableFile.named(file) ?: return done(state("No such file", failed = true))
      val disk = folder.resolve(file)
      var text = disk.readText()
      val refused = changes.mapNotNull { change ->
        when (val result = Editor.set(editable, text, change.path, change.value)) {
          is Editor.Result.Done -> null.also { text = result.text }
          is Editor.Result.Refused -> result.reason
        }
      }
      disk.writeText(text)
      done(state(refused.joinToString("; "), refused.isNotEmpty()))
    }
  }

  object Remote : SettingsSource {
    private var waiting: ((State) -> Unit)? = null

    /** The server answered; the screen that asked hears it. */
    fun received(payload: Payloads.Settings) {
      val callback = waiting ?: return
      waiting = null
      Minecraft.getInstance().execute { callback(State(payload.allowed, if (payload.allowed) null else Message.SETTINGS_DENIED, payload.message, payload.failed, payload.files)) }
    }

    override fun load(done: (State) -> Unit) {
      waiting = done
      ClientLoader.current.send(Payloads.SettingsWanted.INSTANCE)
    }

    override fun save(file: String, changes: List<Payloads.Change>, done: (State) -> Unit) {
      waiting = done
      ClientLoader.current.send(Payloads.SettingsChanged(file, changes.take(Payloads.SettingsChanged.MAX)))
    }
  }

  companion object {
    /** The server's files in a world that has Mo’Bosses as a mod, the game's own in the main menu; null on a server without it. */
    fun pick(): SettingsSource? {
      val game = Minecraft.getInstance()
      return when {
        game.connection == null -> Local
        ClientLoader.current.canSend(Payloads.SettingsWanted.TYPE) -> Remote
        else -> null
      }
    }
  }
}
