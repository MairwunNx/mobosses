package ru.mairwunnx.mobosses.achievement

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import ru.mairwunnx.mobosses.lang.Languages

/**
 * An achievement as the game's advancement JSON, the same on every platform: its icon, frame, texts in one language,
 * its parent, and its criteria: one, [CRITERION], or for the bestiary one per boss type ([criteria]), so the game shows
 * how many are in. Paper grants them by hand ([IMPOSSIBLE]); the mods may hand in a trigger of their own that the game
 * meets itself.
 */
object Advancement {
  const val CRITERION = "done"
  const val IMPOSSIBLE = "minecraft:impossible"
  private const val BACKGROUND = "minecraft:gui/advancements/backgrounds/nether"

  /** [key] names an achievement as the game knows it, like `mobosses:first_blood`. */
  fun json(
    achievement: Achievement, code: String, languages: Languages, key: (Achievement) -> String,
    criterion: JsonObject = trigger(IMPOSSIBLE), names: List<String> = listOf(CRITERION),
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
      add("criteria", JsonObject().apply { names.forEach { add(it, criterion.deepCopy()) } })
      add("display", display)
      // Each criterion a requirement of its own: all of them, one by one, which the game counts as N of M.
      add("requirements", JsonArray().apply { names.forEach { name -> add(JsonArray().apply { add(name) }) } })
    }
  }

  /** The bestiary's criteria, one per boss [types] a server has, and every other achievement's one. */
  fun criteria(achievement: Achievement, types: Collection<String>): List<String> =
    if (achievement in COLLECTIONS && types.isNotEmpty()) types.sorted() else listOf(CRITERION)

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
