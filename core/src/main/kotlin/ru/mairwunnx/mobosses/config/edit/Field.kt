package ru.mairwunnx.mobosses.config.edit

/**
 * One value a settings screen may change: where it sits in its file ([path], by the file's own keys), what it takes,
 * what it holds now and by default, and the comment the file has for it.
 */
data class Field(val path: List<String>, val kind: Kind, val value: String, val default: String, val note: String) {
  enum class Kind { BOOLEAN, INT, LONG, DOUBLE, STRING }

  val key: String get() = path.joinToString(".")

  companion object {
    /** Whether [raw] is a value of [kind]; a number may come with a comma for its point. */
    fun accepts(kind: Kind, raw: String): Boolean = when (kind) {
      Kind.BOOLEAN -> raw == "true" || raw == "false"
      Kind.INT -> raw.toIntOrNull() != null
      Kind.LONG -> raw.toLongOrNull() != null
      Kind.DOUBLE -> raw.replace(',', '.').toDoubleOrNull()?.isFinite() == true
      Kind.STRING -> '\n' !in raw
    }

    /** [raw] as YAML writes a value of [kind]. */
    fun render(kind: Kind, raw: String): String = when (kind) {
      Kind.DOUBLE -> raw.replace(',', '.')
      Kind.STRING -> "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
      else -> raw
    }
  }
}
