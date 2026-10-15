package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.boss.Codec
import ru.mairwunnx.mobosses.combat.Shares
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.spawn.SurvivalBonus
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

  @Test fun `bosses below a player count for less, by ratio`() {
    val r = progression.relevance
    assertEquals(1.0, Leveling.relevance(r, 40, 40))
    // A few levels mean little to a newcomer, and a boss cut by variance costs a veteran some, not all.
    assertTrue(Leveling.relevance(r, 5, 2) > 0.75)
    assertTrue(Leveling.relevance(r, 80, 60) in 0.6..0.8)
    // An alt that halves the group's level halves what a veteran gets, and more.
    assertTrue(Leveling.relevance(r, 80, 40) < 0.45)
    assertEquals(0.1, Leveling.relevance(r, 80, 1))
    assertEquals(1.5, Leveling.relevance(r, 10, 60))
  }

  @Test fun `a kill counts with enough damage on a boss near the player's level`() {
    assertTrue(Leveling.countsAsKill(progression, 0.15, 1, 40, 30))
    assertFalse(Leveling.countsAsKill(progression, 0.05, 1, 40, 40), "a last hit isn't a kill")
    assertFalse(Leveling.countsAsKill(progression, 1.0, 1, 60, 30), "a boss at half the player's level doesn't count")
    // The deepest variance cut always counts: 30% off at level 100.
    assertTrue(Leveling.countsAsKill(progression, 1.0, 1, 100, 70))
  }

  @Test fun `thresholds come down for groups`() {
    assertEquals(0.27777777, Leveling.threshold(0.27777777, progression.groupShare, 2))
    assertEquals(0.15, Leveling.threshold(0.27777777, progression.groupShare, 4), 1e-9)
    // Eight players who each dealt an eighth all count the kill.
    assertTrue(Leveling.countsAsKill(progression, 0.125, 8, 30, 30))
  }

  @Test fun `pets take little at first and more at the top`() {
    val pets = progression.petDamage
    assertTrue(Leveling.petWeight(pets, 5) > 0.95)
    assertTrue(Leveling.petWeight(pets, 100) in 0.55..0.7)
    assertEquals(1.0, Shares.petWorth(10.0, 0.0, 0.5))
    assertEquals(0.75, Shares.petWorth(10.0, 5.0, 0.5))
  }

  @Test fun `a kill that counts is never worth nothing`() {
    assertEquals(1, Leveling.gained(0.2, counts = true))
    assertEquals(0, Leveling.gained(0.2, counts = false))
  }

  @Test fun `vanilla experience starts near the mob's and tops out past the dragon's first kill`() {
    val curve = settings.formula.experience
    // A level 1 Common zombie: a little more than the zombie's 5, and even a Mythic one less than the Wither's 50.
    assertEquals(8, Stats.experience(curve, 5, 1.5, 1.0, 1))
    assertTrue(Stats.experience(curve, 5, 8.0, 1.0, 1) < 50)
    // A level 30 Common zombie outlasts the Ender Dragon, and gives more than its 500.
    assertTrue(Stats.experience(curve, 5, 1.5, 1.0, 30) > 500)
    // A level 100 Mythic: 20,000, past the dragon's first kill of 12,000 and under 39,627, level 111 from nothing.
    assertEquals(20_000.0, Stats.experience(curve, 5, 8.0, 1.0, 100).toDouble(), 100.0)
    // A mob that drops nothing, like a vex, starts from base_points.
    assertEquals(Stats.experience(curve, 5, 1.5, 1.0, 50), Stats.experience(curve, 0, 1.5, 1.0, 50))
  }

  @Test fun `top bosses outlast the Ender Dragon and the Warden`() {
    val health = settings.formula.health
    assertTrue(Stats.health(health, 20.0, 1.0, 30) >= 200, "a level 30 Common zombie")
    assertTrue(Stats.health(health, 20.0, 1.0, 100) >= 500, "a level 100 Common zombie")
    assertTrue(Stats.health(health, 20.0, 2.2, 40) >= 500, "a level 40 Epic zombie")
    assertTrue(Stats.health(health, 20.0, 4.0, 20) >= 500, "a level 20 Mythic zombie")
    assertEquals(Stats.health(health, 20.0, 1.0, 50), Stats.health(health, 8.0, 1.0, 50), "a silverfish starts from a zombie's health")
  }

  @Test fun `the survival bonus grows fast, then slower, toward its top`() {
    val cfg = settings.survivalBonus
    assertEquals(0.0, SurvivalBonus.bonus(cfg, 0))
    assertEquals(0.25, SurvivalBonus.bonus(cfg, 60))
    assertEquals(0.5, SurvivalBonus.bonus(cfg, 180))
    assertEquals(0.75, SurvivalBonus.bonus(cfg, 540))
    assertTrue(SurvivalBonus.bonus(cfg, 100_000) < 1.0)
  }

  @Test fun `a creeper's blast grows past a charged creeper's`() {
    val explosion = settings.formula.explosion
    assertEquals(3, Stats.explosionRadius(explosion, 3, 1))
    assertEquals(6, Stats.explosionRadius(explosion, 6, 1))
    assertTrue(Stats.explosionRadius(explosion, 3, 100) >= 8, "an uncharged level 100 boss outblasts the game's charged creeper")
    assertTrue(Stats.explosionRadius(explosion, 6, 100) in 10..12)
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
