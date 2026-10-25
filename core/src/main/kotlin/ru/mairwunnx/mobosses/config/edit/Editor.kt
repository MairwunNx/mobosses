package ru.mairwunnx.mobosses.config.edit

/**
 * Changes one value of a config file the way a settings screen asks, the same on every platform and for the server's
 * files as for a singleplayer game's: only a value the model has, only in its kind, written on its own line so every
 * comment stays, and only when Mo’Bosses reads the result without falling back for that value.
 */
object Editor {
  sealed interface Result {
    class Done(val text: String, val field: Field) : Result
    class Refused(val reason: String) : Result
  }

  fun set(file: EditableFile<*>, text: String, path: List<String>, raw: String): Result {
    val field = file.fields(text).firstOrNull { it.path == path } ?: return Result.Refused("${file.name} has no ${path.joinToString(".")}")
    val value = raw.trim()
    if (!Field.accepts(field.kind, value)) return Result.Refused("${field.key} takes ${field.kind.name.lowercase()}, not “$value”")
    val edited = Lines.set(text, path, Field.render(field.kind, value))
    val warnings = runCatching { file.warnings(edited) }.getOrElse { return Result.Refused("${file.name} wouldn't read: ${it.message}") }
    warnings.firstOrNull { it.startsWith(field.key) }?.let { return Result.Refused(it) }
    val now = file.fields(edited).first { it.path == path }
    return Result.Done(edited, now)
  }
}
