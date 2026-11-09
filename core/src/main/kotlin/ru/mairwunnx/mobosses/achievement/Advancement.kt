package ru.mairwunnx.mobosses.achievement

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import ru.mairwunnx.mobosses.lang.Languages

/**
 * An achievement as the game's advancement JSON, the same on every platform: its icon, frame, texts in one language,
 * its parent, and its requirements ([requirements]): one, [CRITERION], or one per step, so the game shows how many are
 * in. The bestiary and the Mythic collection have a step per boss type, a kill tier a step per kill. Paper grants them
 * by hand ([IMPOSSIBLE]); the mods may hand in a trigger of their own that the game meets itself.
 */
object Advancement {
  const val CRITERION = "done"
  const val IMPOSSIBLE = "minecraft:impossible"
  private const val BACKGROUND = "minecraft:gui/advancements/backgrounds/nether"

  /** [key] names an achievement as the game knows it, like `mobosses:first_blood`. */
  fun json(
    achievement: Achievement, code: String, languages: Languages, key: (Achievement) -> String,
    criterion: JsonObject = trigger(IMPOSSIBLE), requirements: List<List<String>> = listOf(listOf(CRITERION)),
  ): JsonObject {
    fun text(part: String) = GsonComponentSerializer.gson().serializeToTree(languages.render(code, "achievements.${achievement.id}.$part") ?: Component.text(achievement.id))
    val display = JsonObject().apply {
      add("icon", JsonObject().apply { addProperty("id", achievement.icon) })
      add("title", text("title"))
      add("description", text("description"))
      addProperty("frame", achievement.frame.name.lowercase())
      addProperty("show_toast", true)
      addProperty("announce_to_chat", true)
      addProperty("hidden", false)
      if (achievement.parent == null) addProperty("background", BACKGROUND)
    }
    return JsonObject().apply {
      achievement.parent?.let { addProperty("parent", key(it)) }
      add("criteria", JsonObject().apply { requirements.flatten().distinct().forEach { add(it, criterion.deepCopy()) } })
      add("display", display)
      // The game counts the requirements met, N of M; a requirement is met by any one of its criteria.
      add("requirements", JsonArray().apply { requirements.forEach { names -> add(JsonArray().apply { names.forEach(::add) }) } })
    }
  }

  /**
   * What [achievement] asks, each list one step the game counts: for the bestiary and the Mythic collection one per
   * boss [types] a server has, for a kill tier one per kill, `1` to its goal, and for every other achievement one,
   * [CRITERION]. A kill tier's every step takes [CRITERION] too, so a player who got the tier before it counted kills
   * keeps it whole, and one who reaches the goal needs that one criterion alone ([counted]).
   */
  fun requirements(achievement: Achievement, types: Collection<String>): List<List<String>> {
    val goal = goal(achievement)
    return when {
      goal != null -> (1..goal).map { listOf(it.toString(), CRITERION) }
      achievement in COLLECTIONS && types.isNotEmpty() -> types.sorted().map(::listOf)
      else -> listOf(listOf(CRITERION))
    }
  }

  /** The kills a kill tier asks, or null for every other achievement. */
  fun goal(achievement: Achievement): Int? = Achievement.KILLS.firstOrNull { it.second == achievement }?.first

  /**
   * The criteria to award from [remaining] on a kill tier of [goal] for a player's [kills]: each kill's own up to the
   * count, and once the goal is reached [CRITERION] alone, which meets every step at once. A list without it, from a
   * data pack, gets all its steps then.
   */
  fun counted(goal: Int, kills: Int, remaining: Collection<String>): List<String> = when {
    kills >= goal -> if (CRITERION in remaining) listOf(CRITERION) else remaining.toList()
    else -> remaining.filter { it.toIntOrNull()?.let { step -> step <= kills } == true }
  }

  /**
   * The criteria to award from [remaining] for boss types [killed]: once the server's every type of [types] is in,
   * the rest too, so a list built from older files, or from 1.x names, still completes.
   */
  fun collected(remaining: Collection<String>, killed: Set<String>, types: Set<String>): List<String> {
    val all = types.isNotEmpty() && killed.containsAll(types)
    return remaining.filter { all || it in killed }
  }

  private val COLLECTIONS = setOf(Achievement.BESTIARY, Achievement.MYTHIC_COLLECTOR)

  fun trigger(id: String, conditions: JsonObject? = null) = JsonObject().apply {
    addProperty("trigger", id)
    conditions?.let { add("conditions", it) }
  }
}
