package ru.mairwunnx.mobosses.modcore.config

import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.config.edit.EditableFile
import ru.mairwunnx.mobosses.config.edit.Editor
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.modcore.Scheduler
import ru.mairwunnx.mobosses.modcore.net.Payloads
import java.util.UUID

/**
 * The server's side of a player's settings screen: it shows the four files and changes their values, for a player
 * with `mobosses.config` only, checked on every message, never trusting the game that sent it. A change names a file
 * and a key the model has, and a value of its kind ([Editor]); never a path on disk or a whole file. Each one goes into
 * the server's log with who made it, one batch a second at most per player, and Mo’Bosses reloads once they stop. In
 * singleplayer the world's owner is that player.
 */
class SettingsService(private val mod: MoBosses) {
  private val last = HashMap<UUID, Long>()
  private var reload: Scheduler.Task? = null

  fun wanted(player: ServerPlayer) = send(player, "", failed = false)

  fun changed(player: ServerPlayer, batch: Payloads.SettingsChanged) {
    if (!allowed(player)) return send(player, "", failed = true)
    if (!pace(player)) return send(player, "Too many changes at once; wait a second", failed = true)
    val file = EditableFile.named(batch.file) ?: return send(player, "No such file", failed = true)
    val disk = mod.folder.resolve(file.name)
    var text = disk.readText()
    val refused = ArrayList<String>()
    var done = 0
    batch.changes.forEach { change ->
      when (val result = Editor.set(file, text, change.path, change.value)) {
        is Editor.Result.Refused -> refused += result.reason
        is Editor.Result.Done -> {
          val before = file.fields(text).firstOrNull { it.path == change.path }?.value
          text = result.text
          done++
          mod.log.info("${player.plainTextName} set ${file.name} ${result.field.key}: $before → ${result.field.value}")
        }
      }
    }
    if (done > 0) {
      disk.writeText(text)
      reload?.cancel()
      reload = mod.scheduler.later(20) {
        reload = null
        mod.reload()
      }
    }
    send(player, refused.joinToString("; ").ifEmpty { "${file.name}: $done" }, refused.isNotEmpty())
  }

  private fun allowed(player: ServerPlayer) = Perms.check(player.createCommandSourceStack(), Permissions.CONFIG)

  /** At most one batch a second per player. */
  private fun pace(player: ServerPlayer): Boolean {
    val now = System.currentTimeMillis()
    if (now - (last[player.uuid] ?: 0L) < 1000) return false
    last[player.uuid] = now
    return true
  }

  private fun send(player: ServerPlayer, message: String, failed: Boolean) {
    if (!Mod.loader.canSend(player, Payloads.Settings.TYPE)) return
    val allowed = allowed(player)
    val files = if (!allowed) emptyList() else EditableFile.ALL.map { file ->
      Payloads.SettingsFile(file.name, file.fields(mod.folder.resolve(file.name).takeIf { it.isFile }?.readText().orEmpty()))
    }
    Mod.loader.send(player, Payloads.Settings(allowed, message, failed, files))
  }
}
