package ru.mairwunnx.mobosses.config.edit

import kotlinx.serialization.KSerializer
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.config.Yamls

/** A file the settings screen edits: its name, its model, and how Mo’Bosses reads it, warnings and all. */
class EditableFile<T>(val name: String, private val serializer: KSerializer<T>, private val defaults: T) {
  /** Every value in [text] the screen may change, with the defaults beside them. */
  fun fields(text: String): List<Field> = Fields.of(serializer.descriptor, text, Yamls.yaml.encodeToString(serializer, defaults))

  /** The warnings Mo’Bosses would log reading [text]: one per value that doesn't fit and falls back to its default. */
  fun warnings(text: String): List<String> = mutableListOf<String>().also { Yamls.decode(serializer, text, it::add) }

  companion object {
    val ALL: List<EditableFile<*>> = listOf(
      EditableFile("config.yml", Settings.serializer(), Settings()),
      EditableFile("classes.yml", Classes.serializer(), Classes()),
      EditableFile("abilities.yml", AbilitySettings.serializer(), AbilitySettings()),
      EditableFile("rewards.yml", RewardSettings.serializer(), RewardSettings()),
    )

    fun named(name: String) = ALL.firstOrNull { it.name == name }
  }
}
