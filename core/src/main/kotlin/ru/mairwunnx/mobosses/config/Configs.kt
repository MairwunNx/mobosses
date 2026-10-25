package ru.mairwunnx.mobosses.config

import java.io.File

/**
 * Every config file, read together on start and on `/mobosses reload`. A file that's missing is
 * copied from the jar first. A bad value costs only itself (see [Yamls]); broken YAML costs the
 * file, which then falls back to the copy bundled in the jar, and [ok] turns false.
 */
class Configs(
  val settings: Settings,
  val defaultLanguage: String,
  val abilities: AbilitySettings,
  val classes: Classes,
  val bosses: Map<String, BossSpec>,
  val minions: MinionSettings,
  val rewards: RewardSettings,
  val loot: LootTable,
  val ok: Boolean,
) {
  companion object {
    val FILES = listOf("config.yml", "abilities.yml", "classes.yml", "bosses.yml", "minions.yml", "rewards.yml", "loottable.yml")

    /**
     * [read] gives a file's text from the data folder, [bundled] the copy in the jar; [warn] and
     * [error] take messages that already name the file.
     */
    fun load(read: (String) -> String, bundled: (String) -> String, warn: (String) -> Unit, error: (String) -> Unit): Configs {
      var ok = true

      fun <T> file(name: String, parse: (String, (String) -> Unit) -> T): T = try {
        parse(read(name)) { warn("$name: $it") }
      } catch (e: Exception) {
        ok = false
        error("$name can't be read, using the bundled one: ${e.message?.substringBefore('\n')}")
        parse(bundled(name)) {}
      }

      val configText = runCatching { read("config.yml") }.getOrDefault("")
      val settings = file("config.yml", Settings::parse)
      return Configs(
        settings = settings,
        defaultLanguage = settings.defaultLanguage(configText),
        abilities = file("abilities.yml", AbilitySettings::parse),
        classes = file("classes.yml", Classes::parse),
        bosses = file("bosses.yml", BossSpec::parse),
        minions = file("minions.yml", MinionSettings::parse),
        rewards = file("rewards.yml", RewardSettings::parse),
        loot = file("loottable.yml", LootTable::parse),
        ok = ok,
      )
    }

    fun fromFolder(folder: File, bundled: (String) -> String, warn: (String) -> Unit, error: (String) -> Unit) =
      load({ folder.resolve(it).readText() }, bundled, warn, error)
  }
}
