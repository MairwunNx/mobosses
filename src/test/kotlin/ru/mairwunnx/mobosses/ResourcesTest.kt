package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.Minions
import ru.mairwunnx.mobosses.config.Rewards
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.config.Yamls
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourcesTest {
  private fun resource(path: String) = javaClass.classLoader.getResource(path)!!.readText()

  private fun <T> clean(name: String, parse: (String, (String) -> Unit) -> T): T {
    val warnings = mutableListOf<String>()
    return parse(resource(name)) { warnings += it }.also { assertEquals(emptyList(), warnings, "$name warns") }
  }

  @Test fun `the bundled config files match the defaults`() {
    assertEquals(Settings(), clean("config.yml", Settings::parse))
    assertEquals(AbilitySettings(), clean("abilities.yml", AbilitySettings::parse))
    assertEquals(Classes(), clean("classes.yml", Classes::parse))
    assertEquals(Rewards(), clean("rewards.yml", Rewards::parse))
  }

  @Test fun `the bundled boss, minion, and loot files read without warnings`() {
    val bosses = clean("bosses.yml", BossSpec::parse)
    assertEquals(17, bosses.size)
    assertTrue(bosses.values.all { it.name == null }, "bosses.yml names bosses; they belong in lang/")
    assertEquals(15, clean("minions.yml", Minions::parse).minions.size)
    val loot = clean("loottable.yml", LootTable::parse)
    assertTrue(loot.items.size > 500)
    assertTrue(loot.items.none { it.title != null || it.lore.isNotEmpty() }, "loottable.yml has fixed names; they belong in lang/")
  }

  @Test fun `every bundled language sets every key the bundled files use`() {
    val bosses = BossSpec.parse(resource("bosses.yml")).keys.map { "bosses.${it.lowercase()}" }
    val minions = Minions.parse(resource("minions.yml")).minions.map { "minions.${it.summoner.lowercase()}" }
    val classes = Classes.KEYS.map { "classes.$it" }
    val items = LootTable.parse(resource("loottable.yml")).items.mapNotNull { it.id }.distinct().map { "items.$it.name" }
    val keys = Message.entries.map { it.key } + bosses + minions + classes + items
    Languages.BUNDLED.forEach { code ->
      val texts = Yamls.flatten(Yamls.parse(resource("lang/$code.yml")))
      assertEquals(emptyList(), keys.filter { it !in texts }, "lang/$code.yml misses keys")
      assertEquals(emptyList(), texts.keys.filter { it !in keys && !it.endsWith(".lore") }, "lang/$code.yml has keys nothing uses")
    }
  }

  @Test fun `1x config files still load, keeping their values`() {
    val warnings = mutableListOf<String>()
    val configs = Configs.load({ resource("v1/$it") }, ::resource, { warnings += it }, { warnings += it })
    assertTrue(configs.ok)
    assertEquals(emptyList(), warnings)
    // 1.x's language: ru becomes the default language.
    assertEquals("ru", configs.defaultLanguage)
    assertEquals(0.09, configs.bosses.getValue("ZOMBIE").spawnChance)
    assertEquals("Гнилой", configs.bosses.getValue("ZOMBIE").name)
    assertEquals("Common", configs.classes.common.displayName)
    assertEquals(BossSpec.parse(resource("bosses.yml")).keys, configs.bosses.keys)
    assertEquals(LootTable.parse(resource("loottable.yml")).items.size, configs.loot.items.size)
    assertTrue(configs.loot.items.first { it.material == "STONE_SWORD" && it.title != null }.lore.isNotEmpty())
    // Everything else equals what 2.0 ships.
    assertEquals(Settings().copy(language = "ru", bossBar = Settings.BossBar(style = "SEGMENTED_20")), configs.settings)
    assertEquals(AbilitySettings(), configs.abilities)
    assertEquals(Rewards(), configs.rewards)
  }

  @Test fun `a bad value costs only itself`() {
    val warnings = mutableListOf<String>()
    val settings = Settings.parse("debug: yes please\nspawn: { spawn_radius: far, enabled: false }\nlocales: { default: ru }", warnings::add)
    assertEquals(false, settings.debug)
    assertEquals(100, settings.spawn.spawnRadius)
    assertEquals(false, settings.spawn.enabled)
    assertEquals("ru", settings.locales.default)
    assertEquals(2, warnings.size, warnings.toString())
    assertTrue(warnings.any { it.startsWith("spawn.spawn_radius") })
  }

  @Test fun `locales default wins over 1x language`() {
    val text = "language: ru\nlocales: { default: de }"
    assertEquals("de", Settings.parse(text).defaultLanguage(text))
    assertEquals("en", Settings().defaultLanguage(""))
  }
}
