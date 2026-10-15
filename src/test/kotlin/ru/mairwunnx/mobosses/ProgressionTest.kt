package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.boss.Codec
import ru.mairwunnx.mobosses.combat.Shares
import ru.mairwunnx.mobosses.config.Settings
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What 2.1 changed in the formulas, for fewer spikes. */
class ProgressionTest {
  private val settings = Settings()
  private val progression = settings.progression

  @Test fun `levels climb slower at first than in 1x`() {
    // Kills only, every one counted: 1.x gave level 29 after 100 kills.
    assertEquals(listOf(3, 8, 19, 40, 67, 86), listOf(20, 50, 100, 200, 400, 800).map { Leveling.playerLevel(progression.levelCurve, 0, it) })
  }

  @Test fun `levels never pass max_level`() {
    assertEquals(100, Leveling.playerLevel(progression.levelCurve, Long.MAX_VALUE / 2, Int.MAX_VALUE / 2))
  }

  @Test fun `a cut boss level stays within its share`() {
    repeat(2000) { seed ->
      val level = Leveling.bossLevel(progression.variance, 80, 100, Random(seed))
      assertTrue(level in 56..80, "seed $seed gave $level")
    }
    // At level 1 nothing falls below 1.
    assertTrue((0 until 200).all { Leveling.bossLevel(progression.variance, 1, 100, Random(it)) == 1 })
  }

  @Test fun `a group's level is its average`() {
    assertEquals(50, Leveling.groupLevel(listOf(10, 90)))
    assertEquals(1, Leveling.groupLevel(emptyList()))
  }

  @Test fun `bosses far below a player count for little`() {
    val r = progression.relevance
    assertEquals(1.0, Leveling.relevance(r, 40, 40))
    assertEquals(0.5, Leveling.relevance(r, 40, 30))
    assertEquals(0.1, Leveling.relevance(r, 80, 10))
    assertEquals(1.5, Leveling.relevance(r, 10, 60))
  }

  @Test fun `a kill counts with enough damage on a boss near the player's level`() {
    assertTrue(Leveling.countsAsKill(progression, 0.15, 40, 30))
    assertFalse(Leveling.countsAsKill(progression, 0.05, 40, 40), "a last hit isn't a kill")
    assertFalse(Leveling.countsAsKill(progression, 1.0, 60, 30), "a boss 30 levels below doesn't count")
  }

  @Test fun `vanilla experience no longer explodes`() {
    // A level 100 Mythic zombie: 9822 points in 1.x, about 1300 now.
    assertEquals(1286, Stats.experience(settings.formula.experience, 5, 8.0, 1.0, 100))
  }

  @Test fun `toughness grows from base_points on mobs that have none`() {
    assertTrue(Stats.armor(settings.formula.toughness, 0.0, 1.97, 50) > 2.0)
    assertEquals(0.0, Stats.armor(settings.formula.toughness.copy(basePoints = 0.0), 0.0, 1.97, 50))
  }

  @Test fun `shares never add up past the whole boss`() {
    val a = UUID(0, 1)
    val b = UUID(0, 2)
    assertEquals(mapOf(a to 0.25, b to 0.5), Shares.of(mapOf(a to 25.0, b to 50.0), 100.0))
    // A boss that healed took 140 damage on 100 health: the shares split the 140.
    assertEquals(mapOf(a to 0.5, b to 0.5), Shares.of(mapOf(a to 70.0, b to 70.0), 100.0))
    assertEquals(emptyMap(), Shares.of(emptyMap(), 0.0))
  }

  @Test fun `saved maps read back, and a damaged pair costs only itself`() {
    val text = Codec.encode(mapOf("a" to 1.5, "b" to 1_760_000_000_000L))
    assertEquals(mapOf("a" to 1.5, "b" to 1.76e12), Codec.decode(text))
    assertEquals(mapOf("ok" to 2.0), Codec.decode("ok=2;broken;=3;x=nan?"))
  }

  @Test fun `weighted picks follow the weights and skip empty ones`() {
    val picks = (0 until 10_000).map { Rolls.weighted(listOf("a" to 3.0, "b" to 1.0, "c" to 0.0), Random(it)) }
    assertEquals(0, picks.count { it == "c" })
    assertTrue(picks.count { it == "a" } in 7_000..8_000)
    assertEquals(null, Rolls.weighted(listOf("a" to 0.0), Random(1)))
  }
}
