package ru.mairwunnx.mobosses.achievement

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import ru.mairwunnx.mobosses.lang.Languages

/**
 * An achievement as the game's advancement JSON, the same on every platform: its icon, frame, texts in one language,
 * its parent, and one criterion, [CRITERION]. Paper grants it by hand ([IMPOSSIBLE]); the mods may hand in a trigger
 * of their own that the game meets itself.
 */
object Advancement {
  const val CRITERION = "done"
  const val IMPOSSIBLE = "minecraft:impossible"
  private const val BACKGROUND = "minecraft:gui/advancements/backgrounds/nether"

  /** [key] names an achievement as the game knows it, like `mobosses:first_blood`. */
  fun json(achievement: Achievement, code: String, languages: Languages, key: (Achievement) -> String, criterion: JsonObject = trigger(IMPOSSIBLE)): JsonObject {
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
      add("criteria", JsonObject().apply { add(CRITERION, criterion) })
      add("display", display)
      add("requirements", JsonArray().apply { add(JsonArray().apply { add(CRITERION) }) })
    }
  }

  fun trigger(id: String, conditions: JsonObject? = null) = JsonObject().apply {
    addProperty("trigger", id)
    conditions?.let { add("conditions", it) }
  }
}
