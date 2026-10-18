package ru.mairwunnx.mobosses.lang

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The languages players chose with `/mobosses language`, one `uuid: code` line each in [file]. A
 * player missing from it gets the automatic choice. The file is small and written on each change.
 */
class LanguageChoices(private val file: File) {
  private val chosen = ConcurrentHashMap<UUID, String>()

  /** Reads the file again; a line that doesn't read is skipped. */
  fun load() {
    chosen.clear()
    if (!file.isFile) return
    file.readLines().forEach { line ->
      val text = line.substringBefore('#').trim()
      val id = runCatching { UUID.fromString(text.substringBefore(':').trim()) }.getOrNull() ?: return@forEach
      val code = LocaleResolver.normalize(text.substringAfter(':', "").trim('\'', '"', ' '))
      if (code.isNotEmpty()) chosen[id] = code
    }
  }

  fun of(player: UUID): String? = chosen[player]

  /** Sets a player's language, or with null hands the choice back to the plugin, and writes the file. */
  fun set(player: UUID, code: String?) {
    if (code == null) chosen.remove(player) else chosen[player] = LocaleResolver.normalize(code)
    save()
  }

  private fun save() {
    file.parentFile?.mkdirs()
    val temp = File(file.parentFile, "${file.name}.tmp")
    temp.writeText(buildString {
      appendLine("# The languages players chose with /mobosses language. Mo’Bosses writes this file.")
      chosen.entries.sortedBy { it.key }.forEach { (id, code) -> appendLine("$id: $code") }
    })
    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  }

  companion object {
    const val AUTO = "auto"

    /** What a player typed, as one of [codes]: `pt-BR` finds `pt_br` first, then `pt`; null when none fits. */
    fun match(typed: String, codes: Collection<String>): String? {
      val wanted = LocaleResolver.normalize(typed)
      return codes.firstOrNull { it == wanted } ?: codes.firstOrNull { it == wanted.substringBefore('_') }
    }

    /** Each bundled language by its own name; a language a server added shows its code. */
    fun nativeName(code: String): String = NAMES[code] ?: code

    private val NAMES = mapOf("en" to "English", "ru" to "Русский", "es" to "Español", "pt" to "Português", "de" to "Deutsch")
  }
}
