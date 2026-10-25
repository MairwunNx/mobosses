package ru.mairwunnx.mobosses.config.edit

/**
 * The comments a YAML file has for its keys, by path: the one on a key's own line, else the lines just above it. The
 * settings screen shows them beside each value, so the file's own words explain it.
 */
object Notes {
  fun of(text: String): Map<List<String>, String> {
    val notes = HashMap<List<String>, String>()
    val above = ArrayList<String>()
    Lines.walk(text) { line, path ->
      when {
        path == null && line.trimStart().startsWith("#") -> above += line.trimStart().removePrefix("#").trim()
        path == null -> above.clear()
        else -> {
          val own = Lines.comment(line)
          val note = own ?: above.filterNot { it.all { c -> c == '-' || c == '#' || c == ' ' } }.joinToString(" ").trim()
          if (note.isNotEmpty()) notes[path] = note
          above.clear()
        }
      }
    }
    return notes
  }
}
