package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.combat.Fatigue
import ru.mairwunnx.mobosses.combat.LootRules
import ru.mairwunnx.mobosses.combat.Payout
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.combat.RewardRules
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.spawn.Hunt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The rules every platform shares: loot, rewards, spawner fatigue, the survival bonus, and the full moon. */
class RulesTest {
  private val classes = Classes.parse(Settings::class.java.classLoader.getResource("classes.yml")!!.readText())

  @Test fun `a boss drops its class and below, at falling odds, never above`() {
    val rare = LootTable.Item(material = "diamond", mobclass = "rare", weight = 2.0)
    assertNull(LootRules.weight(rare, classes, "uncommon", 50, theirs = true))
    assertEquals(2.0, LootRules.weight(rare, classes, "rare", 50, theirs = true))
    assertEquals(2.0 * 0.6, LootRules.weight(rare, classes, "epic", 50, theirs = true)!!, 1e-12)
    assertEquals(2.0 * 0.15, LootRules.weight(rare, classes, "mythic", 50, theirs = true)!!, 1e-12)
  }

  @Test fun `levels and types keep an entry out`() {
    val item = LootTable.Item(material = "diamond", minLevel = 10, maxLevel = 20)
    assertNull(LootRules.weight(item, classes, "common", 9, theirs = true))
    assertNull(LootRules.weight(item, classes, "common", 15, theirs = false))
    assertEquals(1.0, LootRules.weight(item, classes, "common", 15, theirs = true))
  }

  @Test fun `luck tips rare items up by their tier`() {
    val mythic = LootTable.Item(material = "mace", mobclass = "common", itemClass = "mythic")
    assertEquals(1.0 + 0.5 * 2.5, LootRules.weight(mythic, classes, "common", 50, theirs = true, luck = 0.5)!!, 1e-12)
    assertTrue(LootRules.isRare(mythic))
  }

  @Test fun `amounts grow with levels above the entry's lowest, up to a stack`() {
    val item = LootTable.Item(material = "arrow", amount = 10..10, minLevel = 10)
    assertEquals(10, LootRules.amount(item, LootTable.LootSettings(), 10, 64))
    assertEquals(18, LootRules.amount(item, LootTable.LootSettings(), 20, 64))
    assertEquals(16, LootRules.amount(item, LootTable.LootSettings(), 100, 16))
  }

  @Test fun `a worn item keeps between the bounds of durability`() {
    val item = LootTable.Item(material = "iron_sword", randomDurability = true)
    repeat(200) {
      val damage = LootRules.damage(Settings.RandomDurability(), item, 250, Random(it))!!
      assertTrue(damage in 25..225, "$damage")
    }
    assertNull(LootRules.damage(Settings.RandomDurability(), item.copy(randomDurability = false), 250))
  }

  @Test fun `rewards grow with the level within their bounds`() {
    val cfg = RewardSettings()
    assertEquals(8, RewardRules.seconds(cfg, 0))
    assertEquals(60, RewardRules.seconds(cfg, 100))
    assertEquals(3, RewardRules.amplifier(cfg, 100))
    assertEquals(120, RewardRules.cooldown(cfg, 60))
    assertEquals(8, RewardRules.cooldown(cfg, 1))
    assertEquals(3, RewardRules.absorption(RewardSettings.Tier(extraAbsorptionHearts = 8)))
    assertNull(RewardRules.absorption(RewardSettings.Tier()))
  }

  @Test fun `spawner kills in one spot wear the reward down, and fade`() {
    val cfg = Settings.SpawnerFatigue()
    var kills: String? = null
    val factors = (0 until 6).map { i -> Fatigue.take(cfg, kills, "world", 0, 0, 1000L * i).also { kills = it.second }.first }
    assertEquals(listOf(1.0, 1.0, 1.0, 1.0), factors.take(4))
    assertTrue(factors[5] < factors[4] && factors[4] < 1.0)
    // Elsewhere, or much later, the farm is forgotten.
    assertEquals(1.0, Fatigue.take(cfg, kills, "world", 500, 500, 6000L).first)
    assertEquals(1.0, Fatigue.take(cfg, kills, "world", 0, 0, 6000L + 20 * 60_000L * 6).first)
  }

  @Test fun `the survival bonus is half at half_at_minutes`() {
    val cfg = Settings.SurvivalBonus()
    assertEquals(0.5, Hunt.bonus(cfg, cfg.halfAtMinutes), 1e-12)
    assertEquals(1.0, Hunt.multiplier(cfg.copy(enabled = false), 600))
  }

  @Test fun `the full moon doubles the rarest class and leaves Common`() {
    val cfg = Settings.FullMoon()
    assertEquals(63.0, Hunt.moonWeight(cfg, 63.0, 0, 6))
    assertEquals(2.0, Hunt.moonWeight(cfg, 1.0, 5, 6))
  }

  @Test fun `a solo kill pays it all, and the note says no more`() {
    val payout = Payout.of(Settings(), RewardSettings(), 1.0, 1.0, 1.0, 100, 200.0, 1.0, 20, 20, 1)
    assertEquals(100, payout.vanilla)
    assertTrue(payout.counts && payout.took && payout.gained > 0)
    assertEquals(listOf(Message.NOTE_HEADER), payout.notes(Settings(), emptyArray(), 0.0, null).map { it.first })
  }

  @Test fun `a small share in a group says why it got no loot`() {
    val payout = Payout.of(Settings(), RewardSettings(), 0.1, 1.0, 1.0, 100, 200.0, 1.0, 20, 20, 2)
    assertEquals(10, payout.vanilla)
    assertTrue(Message.NOTE_SMALL_SHARE in payout.notes(Settings(), emptyArray(), 0.0, null).map { it.first })
  }
}
