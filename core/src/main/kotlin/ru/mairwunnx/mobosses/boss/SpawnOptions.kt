package ru.mairwunnx.mobosses.boss

/**
 * What `/mobosses spawn` takes past the class: the place, `x y z [world]`, and `key=value` options in any order.
 * `abilities=shockwave,whirlwind` leaves the boss only those, whatever its level, and `abilities=none` none at all;
 * `count=5` spawns that many at once.
 */
data class SpawnOptions(val place: List<String> = emptyList(), val abilities: Set<String>? = null, val count: Int = 1) {
  sealed interface Parsed {
    data class Ok(val options: SpawnOptions) : Parsed
    /** [arg] is an option it can't read: an unknown key, an unknown ability, a count out of range. */
    data class Bad(val arg: String) : Parsed
  }

  companion object {
    const val MAX_COUNT = 20
    const val NONE = "none"

    /** [args] past the class; [known] are the ability keys a boss may be given. */
    fun parse(args: List<String>, known: Set<String>): Parsed {
      var options = SpawnOptions()
      val place = ArrayList<String>()
      for (arg in args) {
        val key = arg.substringBefore('=', "").lowercase()
        val value = arg.substringAfter('=', "")
        options = when (key) {
          "" -> options.also { place += arg }
          "abilities" -> options.copy(abilities = abilities(value, known) ?: return Parsed.Bad(arg))
          "count" -> options.copy(count = value.toIntOrNull()?.takeIf { it in 1..MAX_COUNT } ?: return Parsed.Bad(arg))
          else -> return Parsed.Bad(arg)
        }
      }
      if (place.size !in setOf(0, 3, 4)) return Parsed.Bad(place.joinToString(" "))
      return Parsed.Ok(options.copy(place = place))
    }

    private fun abilities(value: String, known: Set<String>): Set<String>? {
      if (value.equals(NONE, ignoreCase = true)) return emptySet()
      val keys = value.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
      return keys.takeIf { it.isNotEmpty() && known.containsAll(it) }
    }

    /** Whether a boss may cast [key]: the ones it was given, if any, whatever its level; else those it has [reached]. */
    fun allows(given: Set<String>?, key: String, reached: Boolean) = given?.contains(key) ?: reached

    /** What may follow the option [typed] so far: each ability not listed yet, or a count. */
    fun suggest(typed: String, known: Set<String>): List<String> {
      val key = typed.substringBefore('=')
      if (!key.equals("abilities", ignoreCase = true)) return if (key.equals("count", ignoreCase = true)) listOf("1", "3", "5").map { "$key=$it" } else emptyList()
      val head = typed.substringAfter('=').substringBeforeLast(',', "")
      val prefix = "$key=" + if (head.isEmpty()) "" else "$head,"
      val taken = head.split(',').toSet()
      return (if (head.isEmpty()) listOf(NONE) else emptyList()).map { prefix + it } + known.filter { it !in taken }.map { prefix + it }
    }

    fun encode(abilities: Set<String>) = abilities.sorted().joinToString(",")

    fun decode(text: String): Set<String> = text.split(',').filter { it.isNotEmpty() }.toSet()
  }
}
