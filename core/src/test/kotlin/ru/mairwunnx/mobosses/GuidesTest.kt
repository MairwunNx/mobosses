package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.ability.AbilityGuide
import ru.mairwunnx.mobosses.combat.LootGuide
import ru.mairwunnx.mobosses.combat.LootRules
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.LootTable
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What Allay's tools read out to players, the same on every platform: the abilities explained, and the loot table's odds and names. */
class GuidesTest {
  private fun resource(path: String) = javaClass.classLoader.getResource(path)!!.readText()

  private val classes = Classes.parse(resource("classes.yml"))
  private val table = LootTable.parse(resource("loottable.yml"))

  @Test fun `every ability is explained, and nothing else`() {
    val shared = AbilityThresholds.serializer().descriptor.let { d -> (0 until d.elementsCount).map(d::getElementName) } - "signature" + "phase_two"
    val signatures = AbilitySettings.SIGNATURE_TYPES.keys
    assertEquals((shared + signatures).toSet(), AbilityGuide.ALL.map { it.key }.toSet())
    assertEquals(AbilityGuide.ALL.size, AbilityGuide.ALL.map { it.key }.distinct().size)
    AbilityGuide.ALL.forEach { entry -> assertEquals(entry.key in signatures, entry.signature, "${entry.key} is a signature move or not") }
  }

  @Test fun `each explanation is one short line in the docs' style`() {
    AbilityGuide.ALL.forEach { entry ->
      listOf(entry.description, entry.tip).forEach { text ->
        assertTrue(text.length in 20..170, "${entry.key}: ${text.length} characters")
        assertTrue(';' !in text && '—' !in text && " - " !in text, "${entry.key}: no semicolons or loose dashes")
        assertTrue(text.endsWith('.'), "${entry.key} ends its sentence")
      }
    }
  }

  @Test fun `the numbers come from abilities yml`() {
    val settings = AbilitySettings(lightningDamage = 7.0, lightningCooldown = 12000, signature = mapOf("whirlwind" to AbilitySettings.Signature(cooldown = 4000, strength = 2.0)))
    val lightning = AbilityGuide.of("lightning")!!.numbers(settings)
    assertEquals(12.0, lightning.cooldownSeconds)
    assertEquals(7.0, lightning.damage)
    val whirlwind = AbilityGuide.of("Whirlwind ")!!.numbers(settings)
    assertEquals(4.0, whirlwind.cooldownSeconds)
    assertEquals(8.0, whirlwind.damage)
    assertEquals(28.0, whirlwind.damageMax)
    assertEquals(30.0, AbilityGuide.of("enrage")!!.numbers(AbilitySettings()).healthPercent!!, 1e-9)
    assertNull(AbilityGuide.of("nothing"))
  }

  @Test fun `chances add up to the whole and come best first`() {
    val pool = LootRules.pool(table.items, { it }, { it.types.isEmpty() }, classes, "epic", 30)
    val chances = LootGuide.chances(pool)
    assertEquals(100.0, chances.sumOf { it.second }, 1e-9)
    assertEquals(chances.map { it.second }.sortedDescending(), chances.map { it.second })
    assertTrue(LootGuide.chances(emptyList<Pair<String, Double>>()).isEmpty())
  }

  @Test fun `amounts bound what really drops`() {
    val item = LootTable.Item(material = "arrow", minLevel = 10, amount = 2..5, perLevel = 0.1)
    val settings = LootTable.LootSettings()
    val bounds = LootGuide.amounts(item, settings, 20, 64)
    assertEquals(4..10, bounds)
    val rng = Random(7)
    repeat(500) { assertTrue(LootRules.amount(item, settings, 20, 64, rng) in bounds) }
    assertEquals(1..1, LootGuide.amounts(LootTable.Item(material = "bow"), settings, 50, 1))
    assertEquals("4–10", LootGuide.text(bounds))
    assertEquals("1", LootGuide.text(1..1))
  }

  @Test fun `an item is found by its id or a name in any language`() {
    assertTrue(LootGuide.matches("diamond sword", "minecraft:diamond_sword", emptyList()))
    assertTrue(LootGuide.matches("MINECRAFT:DIAMOND", "minecraft:diamond_sword", emptyList()))
    assertTrue(LootGuide.matches("клинок", "minecraft:stone_sword", listOf("Клинок ученика")))
    assertTrue(!LootGuide.matches("emerald", "minecraft:diamond_sword", listOf("Apprentice’s Stone Blade")))
    assertTrue(!LootGuide.matches("  ", "minecraft:diamond_sword", emptyList()))
  }

  @Test fun `the named drops stay few, and each says where it comes from`() {
    val named = LootGuide.named(table.items, classes)
    assertEquals(table.items.mapNotNull { it.id }.distinct().size, named.size)
    assertTrue(named.size in 1..400, "${named.size} named drops")
    named.forEach { drop ->
      assertTrue(drop.id.matches(Regex("[a-z0-9_./-]+")), "${drop.id} is an item path Allay takes")
      assertTrue(drop.fromClass in Classes.KEYS, "${drop.id} drops from ${drop.fromClass}")
      val text = LootGuide.describe(drop, "Plain, but it holds an edge. ".repeat(20))
      assertTrue(text.length <= 300 && text.startsWith("A "), "${drop.id}: ${text.length} characters")
    }
  }

  @Test fun `languages map to the game's`() {
    assertEquals(listOf("ru_ru"), LootGuide.locales("ru"))
    assertEquals(listOf("pt_br", "pt_pt"), LootGuide.locales("pt"))
    assertEquals(listOf("pt_br"), LootGuide.locales("pt_BR"))
    assertTrue("en_us" in LootGuide.locales("en"))
  }
}
