package ru.mairwunnx.mobosses.config.edit

/**
 * YAML as lines: which line holds which key, by indentation, and how to change one value on its line without touching
 * the rest, comments and layout included. Only what Mo’Bosses' files use: block maps, and one-line flow maps like
 * `potion_volley: { enabled: true, cooldown: 9000 }`. Lists stay as they are.
 */
object Lines {
  /** A line with a key: where it is, how deep, its path, and what follows the colon. */
  class Entry(val index: Int, val indent: Int, val key: String, val path: List<String>, val rest: String)

  private val KEY = Regex("""^(\s*)([A-Za-z0-9_.\-]+):(\s.*|)$""")

  fun entries(lines: List<String>): List<Entry> {
    val stack = ArrayList<Pair<Int, String>>()
    return lines.mapIndexedNotNull { index, line ->
      val match = KEY.matchEntire(line) ?: return@mapIndexedNotNull null
      val indent = match.groupValues[1].length
      while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeAt(stack.lastIndex)
      val key = match.groupValues[2]
      val entry = Entry(index, indent, key, stack.map { it.second } + key, match.groupValues[3])
      stack += indent to key
      entry
    }
  }

  /** Each line, with its key's path, or null for comments, blanks, and list items. */
  fun walk(text: String, each: (line: String, path: List<String>?) -> Unit) {
    val lines = text.lines()
    val byLine = entries(lines).associateBy { it.index }
    lines.forEachIndexed { i, line -> each(line, byLine[i]?.path) }
  }

  /** The comment on a line, outside any quotes; null without one. */
  fun comment(line: String): String? = commentStart(line)?.let { line.substring(it + 1).trim() }?.takeIf { it.isNotEmpty() }

  private fun commentStart(text: String): Int? {
    var quote: Char? = null
    text.forEachIndexed { i, c ->
      when {
        quote != null -> if (c == quote && text.getOrNull(i - 1) != '\\') quote = null
        c == '"' || c == '\'' -> quote = c
        c == '#' && (i == 0 || text[i - 1].isWhitespace()) -> return i
      }
    }
    return null
  }

  /** [text] with the value at [path] set to [value], already written as YAML; a key the file lacks is added where it belongs. */
  fun set(text: String, path: List<String>, value: String): String {
    val lines = text.lines().toMutableList()
    val entries = entries(lines)
    entries.firstOrNull { it.path == path }?.let { entry ->
      lines[entry.index] = " ".repeat(entry.indent) + entry.key + ":" + replaced(entry.rest, value)
      return lines.joinToString("\n")
    }
    entries.firstOrNull { it.path == path.dropLast(1) && it.rest.trimStart().startsWith("{") }?.let { entry ->
      lines[entry.index] = " ".repeat(entry.indent) + entry.key + ":" + flow(entry.rest, path.last(), value)
      return lines.joinToString("\n")
    }
    // The deepest parent the file has; the rest of the path comes in under it, two spaces deeper each.
    val parent = (path.size - 1 downTo 1).firstNotNullOfOrNull { size -> entries.firstOrNull { it.path == path.take(size) && blockStart(it) } }
    val depth = parent?.path?.size ?: 0
    val children = entries.filter { parent != null && it.path.size == depth + 1 && it.path.take(depth) == parent.path }
    var indent = children.firstOrNull()?.indent ?: parent?.let { it.indent + 2 } ?: 0
    val added = path.drop(depth).mapIndexed { i, key ->
      val line = " ".repeat(indent) + key + ":" + if (i == path.size - depth - 1) " $value" else ""
      indent += 2
      line
    }
    val at = if (parent == null) lines.size else end(lines, parent)
    if (parent == null && lines.lastOrNull()?.isBlank() == true) lines.add(lines.size - 1, added.joinToString("\n")) else lines.addAll(at, added)
    return lines.joinToString("\n")
  }

  private fun blockStart(entry: Entry) = entry.rest.substringBefore('#').isBlank()

  /** The line after the last one in [parent]'s block: deeper than it, or blank, up to the next line at its depth or above. */
  private fun end(lines: List<String>, parent: Entry): Int {
    var last = parent.index
    for (i in parent.index + 1 until lines.size) {
      val line = lines[i]
      if (line.isBlank() || line.trimStart().startsWith("#")) continue
      if (line.length - line.trimStart().length <= parent.indent) break
      last = i
    }
    return last + 1
  }

  /** What follows a key's colon, with its value swapped and its comment kept where it was. */
  private fun replaced(rest: String, value: String): String {
    val comment = commentStart(rest) ?: return " $value"
    val gap = rest.substring(0, comment).let { it.length - it.trimEnd().length }.coerceAtLeast(1)
    return " $value" + " ".repeat(gap) + rest.substring(comment)
  }

  /** A one-line flow map's text with [key] set to [value]: in place when it has the key, else at its end. */
  private fun flow(rest: String, key: String, value: String): String {
    val open = rest.indexOf('{')
    val close = matching(rest, open) ?: return rest
    val parts = split(rest.substring(open + 1, close))
    val index = parts.indexOfFirst { it.substringBefore(':').trim() == key }
    val next = if (index >= 0) parts.mapIndexed { i, part -> if (i == index) " $key: $value" else part } else parts + " $key: $value"
    return rest.substring(0, open + 1) + next.joinToString(",").trimEnd() + " " + rest.substring(close)
  }

  /** The `}` that closes the `{` at [open], past nested brackets and quotes. */
  private fun matching(text: String, open: Int): Int? {
    var depth = 0
    var quote: Char? = null
    for (i in open until text.length) {
      val c = text[i]
      when {
        quote != null -> if (c == quote) quote = null
        c == '"' || c == '\'' -> quote = c
        c == '{' || c == '[' -> depth++
        c == '}' || c == ']' -> if (--depth == 0) return i
      }
    }
    return null
  }

  /** A flow map's inside, split at its own commas, never a nested list's. */
  private fun split(inside: String): List<String> {
    val parts = ArrayList<String>()
    var depth = 0
    var quote: Char? = null
    var start = 0
    inside.forEachIndexed { i, c ->
      when {
        quote != null -> if (c == quote) quote = null
        c == '"' || c == '\'' -> quote = c
        c == '{' || c == '[' -> depth++
        c == '}' || c == ']' -> depth--
        c == ',' && depth == 0 -> {
          parts += inside.substring(start, i)
          start = i + 1
        }
      }
    }
    if (inside.substring(start).isNotBlank()) parts += inside.substring(start)
    return parts
  }
}
