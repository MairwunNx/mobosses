package ru.mairwunnx.mobosses.boss

/** Small maps as text, `key=value;key=value`, for the entity data where a map type would be overkill. */
object Codec {
  fun encode(values: Map<String, Number>): String = values.entries.joinToString(";") { (key, value) -> "$key=$value" }

  /** Skips any pair it can't read, so a damaged value costs only itself. */
  fun decode(text: String?): Map<String, Double> = text.orEmpty().split(';').mapNotNull { pair ->
    val key = pair.substringBefore('=', "").trim()
    val value = pair.substringAfter('=', "").trim().toDoubleOrNull()
    if (key.isEmpty() || value == null) null else key to value
  }.toMap()
}
