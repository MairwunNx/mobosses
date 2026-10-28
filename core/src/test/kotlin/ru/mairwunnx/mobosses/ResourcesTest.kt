package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.MinionSettings
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.config.Yamls
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.achievement.Achievement
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
    assertEquals(RewardSettings(), clean("rewards.yml", RewardSettings::parse))
  }

  @Test fun `the bundled boss, minion, and loot files read without warnings`() {
    val bosses = clean("bosses.yml", BossSpec::parse)
    assertEquals(27, bosses.size)
    assertTrue(bosses.values.all { it.name == null }, "bosses.yml names bosses; they belong in lang/")
    assertEquals(23, clean("minions.yml", MinionSettings::parse).minions.size)
    val loot = clean("loottable.yml", LootTable::parse)
    assertTrue(loot.items.size > 500)
    assertTrue(loot.items.none { it.title != null || it.lore.isNotEmpty() }, "loottable.yml has fixed names; they belong in lang/")
  }

  @Test fun `no loot table entry, shared or a type's own, drops junk blocks`() {
    val junk = LootTable.parse(resource("loottable.yml")).items
      .map { it.material.trim().uppercase().removePrefix("MINECRAFT:") }
      .filter { it in JUNK || JUNK_FAMILIES.any { family -> family.matches(it) } }
    assertEquals(emptyList(), junk.distinct(), "loottable.yml drops junk blocks")
  }

  private companion object {
    /** Blocks anyone digs or crafts by the stack: a boss never drops them. Ores and blocks of metals or gems stay. */
    val JUNK = setOf(
      "COBBLESTONE", "MOSSY_COBBLESTONE", "STONE", "SMOOTH_STONE", "ANDESITE", "DIORITE", "GRANITE", "TUFF", "CALCITE",
      "DRIPSTONE_BLOCK", "POINTED_DRIPSTONE", "DIRT", "COARSE_DIRT", "ROOTED_DIRT", "GRASS_BLOCK", "PODZOL", "MYCELIUM", "MUD",
      "PACKED_MUD", "CLAY", "GRAVEL", "SAND", "RED_SAND", "SANDSTONE", "RED_SANDSTONE", "NETHERRACK", "BASALT", "SMOOTH_BASALT",
      "BLACKSTONE", "SOUL_SAND", "SOUL_SOIL", "END_STONE", "ICE", "SNOW_BLOCK", "MOSS_BLOCK", "TERRACOTTA", "GLASS", "BRICKS",
      "CRAFTING_TABLE", "FURNACE", "CHEST", "LADDER", "BOOKSHELF",
    )
    val JUNK_FAMILIES = listOf(
      ".+_(LOG|WOOD|STEM|HYPHAE|PLANKS|SLAB|STAIRS|FENCE|WOOL|CARPET|LEAVES|SAPLING|TERRACOTTA|CONCRETE|CONCRETE_POWDER|SIGN|STAINED_GLASS)",
      "(COBBLED_|POLISHED_)?DEEPSLATE(_BRICKS|_TILES)?",
      "POLISHED_(ANDESITE|DIORITE|GRANITE|TUFF|BLACKSTONE|BASALT)",
    ).map(::Regex)
  }

  @Test fun `every bundled language sets every key the bundled files use`() {
    val bosses = BossSpec.parse(resource("bosses.yml")).keys.map { "bosses.${it.lowercase()}" }
    val minions = MinionSettings.parse(resource("minions.yml")).minions.map { "minions.${it.summoner.lowercase()}" }
    val classes = Classes.KEYS.map { "classes.$it" }
    val items = LootTable.parse(resource("loottable.yml")).items.mapNotNull { it.id }.distinct().map { "items.$it.name" }
    val keys = Message.entries.map { it.key } + bosses + minions + classes + items + Achievement.TEXT_KEYS
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
    assertTrue(BossSpec.parse(resource("bosses.yml")).keys.containsAll(configs.bosses.keys))
    // Every one of 1.x's loot entries reads; 2.2 ships more.
    assertEquals(577, configs.loot.items.size)
    assertTrue(configs.loot.items.first { it.material == "STONE_SWORD" && it.title != null }.lore.isNotEmpty())
    // Everything else equals what ships now, but for the defaults 2.1, 2.3, and 2.5 changed, which a 1.x file keeps.
    val v1 = Settings()
    assertEquals(
      v1.copy(
        language = "ru",
        spawn = v1.spawn.copy(
          worldMultipliers = mapOf("world" to 1.0, "world_nether" to 1.1, "world_the_end" to 1.3),
          allowedSpawnReasons = v1.spawn.allowedSpawnReasons - "BUILD_WITHER",
        ),
        bossBar = Settings.BossBar(style = "SEGMENTED_20"),
        progression = v1.progression.copy(levelCurve = Settings.LevelCurve(power = 1.3, offset = 20.0), variance = Settings.Variance(extraChance = 0.45)),
        formula = v1.formula.copy(
          health = Settings.Curve(maxScale = 20.0, power = 1.8, offset = 400.0, flatten = 0.35),
          damage = Settings.Curve(maxScale = 6.0, power = 1.2, offset = 600.0, flatten = 0.40),
          experience = Settings.Curve(maxScale = 270.0, power = 2.0, offset = 1000.0),
          armor = v1.formula.armor.copy(basePoints = 0.0),
          toughness = v1.formula.toughness.copy(basePoints = 0.0),
        ),
      ),
      configs.settings,
    )
    // Abilities 2.1 added stay off for a 1.x classes.yml.
    assertEquals(100, configs.classes.mythic.abilityThresholds.shockwave)
    assertEquals(AbilitySettings(), configs.abilities)
    assertEquals(RewardSettings(), configs.rewards)
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
