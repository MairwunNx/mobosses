package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Reel
import ru.mairwunnx.mobosses.combat.Fatigue
import ru.mairwunnx.mobosses.config.AbilitySettings
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

  private fun weight(item: LootTable.Item, bossClass: String, level: Int, theirs: Boolean = true, luck: Double = 0.0, others: List<LootTable.Item> = emptyList()) =
    LootRules.pool(others + item, { it }, { it !== item || theirs }, classes, bossClass, level, luck).firstOrNull { it.first === item }?.second

  @Test fun `a boss drops its class and below, at falling odds, never above`() {
    val rare = LootTable.Item(material = "diamond", mobclass = "rare", weight = 2.0)
    assertNull(weight(rare, "uncommon", 50))
    assertEquals(1.0, weight(rare, "rare", 50))
    assertEquals(0.6, weight(rare, "epic", 50)!!, 1e-12)
    assertEquals(0.15, weight(rare, "mythic", 50)!!, 1e-12)
  }

  @Test fun `a class drops as often however many entries it has`() {
    val epic = LootTable.Item(material = "diamond_sword", mobclass = "epic", weight = 1.0)
    val commons = List(40) { LootTable.Item(material = "iron_ingot", mobclass = "common", weight = 5.0) }
    val pool = LootRules.pool(commons + epic, { it }, { true }, classes, "epic", 30)
    assertEquals(1.0 / 1.15, pool.first { it.first === epic }.second / pool.sumOf { it.second }, 1e-12)
    val two = LootTable.Item(material = "diamond", mobclass = "epic", weight = 3.0)
    assertEquals(0.25, weight(epic, "epic", 30, others = listOf(two))!!, 1e-12)
  }

  @Test fun `levels and types keep an entry out`() {
    val item = LootTable.Item(material = "diamond", minLevel = 10, maxLevel = 20)
    assertNull(weight(item, "common", 9))
    assertNull(weight(item, "common", 15, theirs = false))
    assertEquals(1.0, weight(item, "common", 15))
  }

  @Test fun `luck tips rare items up by their tier`() {
    val mythic = LootTable.Item(material = "mace", mobclass = "common", itemClass = "mythic")
    assertEquals(1.0 + 0.5 * 2.5, weight(mythic, "common", 50, luck = 0.5)!!, 1e-12)
    assertTrue(LootRules.isRare(mythic))
  }

  @Test fun `higher levels give more items, up to the cap`() {
    val items = Settings.Items()
    assertEquals(1, LootRules.items(items, 0))
    assertEquals(1, LootRules.items(items, 19))
    assertEquals(2, LootRules.items(items, 20))
    assertEquals(3, LootRules.items(items, 40))
    assertEquals(3, LootRules.items(items, 100))
    assertEquals(1, LootRules.items(Settings.Items(everyLevels = 0), 100))
  }

  @Test fun `the game's drops grow with level and class, fatigue cutting the extra`() {
    val drops = Settings.VanillaDrops()
    assertEquals(1.3, LootRules.vanillaMultiplier(drops, "common", 10), 1e-12)
    assertEquals(3.25, LootRules.vanillaMultiplier(drops, "epic", 50), 1e-12)
    assertEquals(1.15, LootRules.vanillaMultiplier(drops, "common", 10, fatigue = 0.5), 1e-12)
    assertEquals(1.0, LootRules.vanillaMultiplier(drops.copy(enabled = false), "mythic", 100))
    val counts = List(10_000) { LootRules.vanillaCount(1, 1.3, Random(it)) }
    assertEquals(setOf(1, 2), counts.toSet())
    assertEquals(1.3, counts.average(), 0.02)
    assertEquals(4, LootRules.vanillaCount(2, 2.0))
  }

  @Test fun `heads, nether stars, and what doesn't stack drop as they are`() {
    val drops = Settings.VanillaDrops()
    assertTrue(LootRules.multiplies(drops, "ENDER_PEARL", 16))
    assertTrue(!LootRules.multiplies(drops, "minecraft:nether_star", 64))
    assertTrue(!LootRules.multiplies(drops, "WITHER_SKELETON_SKULL", 64))
    assertTrue(!LootRules.multiplies(drops, "minecraft:trident", 1))
  }

  @Test fun `the chain shortens and heaves`() {
    val chain = AbilitySettings.ChainSettings()
    assertEquals(8.0, Reel.length(chain, 0.0))
    assertEquals(5.0, Reel.length(chain, 5.0), 1e-12)
    assertEquals(3.0, Reel.length(chain, 60.0))
    assertEquals(0.2, Reel.pull(chain, 0, 0.0), 1e-12)
    assertEquals(0.3, Reel.pull(chain, 0, 0.4), 1e-12)
    assertEquals(0.1, Reel.pull(chain, 0, 1.2), 1e-12)
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

  @Test fun `damage nobody dealt by hand goes to the recent hitters by what they dealt`() {
    val a = java.util.UUID.randomUUID()
    val b = java.util.UUID.randomUUID()
    val gone = java.util.UUID.randomUUID()
    val credit = ru.mairwunnx.mobosses.combat.Shares.credit(30.0, mapOf(a to 30.0, b to 10.0, gone to 60.0), mapOf(a to 15.0), setOf(a, b))
    assertEquals(22.5, credit.getValue(a).own + credit.getValue(a).pets, 1e-9)
    assertEquals(11.25, credit.getValue(a).pets, 1e-9)
    assertEquals(7.5, credit.getValue(b).own, 1e-9)
    assertTrue(gone !in credit)
    assertTrue(ru.mairwunnx.mobosses.combat.Shares.credit(30.0, mapOf(a to 30.0), emptyMap(), emptySet()).isEmpty())
  }

  @Test fun `a solo fight keeps the whole share whatever the fall`() {
    val a = java.util.UUID.randomUUID()
    val credit = ru.mairwunnx.mobosses.combat.Shares.credit(40.0, mapOf(a to 60.0), emptyMap(), setOf(a)).getValue(a)
    val shares = ru.mairwunnx.mobosses.combat.Shares.of(mapOf(a to 60.0 + credit.own), 100.0)
    assertEquals(1.0, shares.getValue(a), 1e-9)
  }
}
