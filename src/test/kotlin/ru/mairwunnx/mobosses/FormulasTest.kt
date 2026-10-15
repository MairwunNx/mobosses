package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.config.Settings
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The formulas 2.1 kept, against numbers Mo’Bosses 1.12.0 produced with its bundled config: the
 * release jar's own functions, called from a small program, with the same seeds for the random parts.
 * Player levels and natural boss levels changed in 2.1; ProgressionTest covers them.
 */
class FormulasTest {
  private val settings = Settings()
  private val formula = settings.formula

  private data class Row(val stat: String, val level: Int, val classMul: Double, val expected: Double)

  @Test fun `health, damage, armor, and toughness match 1x`() {
    // A zombie: 20 health, 3 damage, 2 armor; toughness from a base of 2.
    listOf(
      Row("H", 1, 1.0, 20.947630922693268),
      Row("D", 1, 1.0, 3.0249584026622296),
      Row("A", 1, 1.0, 2.0110497237569063),
      Row("T", 1, 1.0, 2.011940298507463),
      Row("H", 1, 1.6, 33.50523939527739),
      Row("D", 1, 1.6, 4.838725474181966),
      Row("A", 1, 1.6, 3.215346295900614),
      Row("T", 1, 1.6, 3.2173027400311875),
      Row("H", 1, 4.0, 83.7356732856139),
      Row("D", 1, 4.0, 12.093793760260906),
      Row("A", 1, 4.0, 8.032532584475444),
      Row("T", 1, 4.0, 8.038752506126087),
      Row("H", 5, 1.0, 36.46755758437069),
      Row("D", 5, 1.0, 3.1705057753847457),
      Row("A", 5, 1.0, 2.073822345637974),
      Row("T", 5, 1.0, 2.0684665829526634),
      Row("H", 5, 1.6, 58.016219406200435),
      Row("D", 5, 1.6, 5.064159807886092),
      Row("A", 5, 1.6, 3.3020408378716573),
      Row("T", 5, 1.6, 3.298924969807771),
      Row("H", 5, 4.0, 144.2108666935194),
      Row("D", 5, 4.0, 12.638775937891479),
      Row("A", 5, 4.0, 8.214914806806389),
      Row("T", 5, 4.0, 8.2207585172282),
      Row("H", 10, 1.0, 71.77413071797274),
      Row("D", 10, 1.0, 3.3860264531533995),
      Row("A", 10, 1.0, 2.1618485407999257),
      Row("T", 10, 1.0, 2.142124821917375),
      Row("H", 10, 1.6, 112.78500631335677),
      Row("D", 10, 1.6, 5.396728792533529),
      Row("A", 10, 1.6, 3.422219001945195),
      Row("T", 10, 1.6, 3.4045660319048157),
      Row("H", 10, 4.0, 276.82850869489295),
      Row("D", 10, 4.0, 13.439538150054046),
      Row("A", 10, 4.0, 8.463700846526272),
      Row("T", 10, 4.0, 8.454330871854577),
      Row("H", 25, 1.0, 191.29937403183231),
      Row("D", 25, 1.0, 4.102346763444823),
      Row("A", 25, 1.0, 2.418217553419962),
      Row("T", 25, 1.0, 2.3530326893228457),
      Row("H", 25, 1.6, 287.96955835853646),
      Row("D", 25, 1.6, 6.491399482676329),
      Row("A", 25, 1.6, 3.7629572774449467),
      Row("T", 25, 1.6, 3.7025499935383372),
      Row("H", 25, 4.0, 674.650295665353),
      Row("D", 25, 4.0, 16.047610359602352),
      Row("A", 25, 4.0, 9.141916173544884),
      Row("T", 25, 4.0, 9.100619210400303),
      Row("H", 50, 1.0, 301.507366884213),
      Row("D", 50, 1.0, 5.312081483813499),
      Row("A", 50, 1.0, 2.755772728058999),
      Row("T", 50, 1.0, 2.6477779385934035),
      Row("H", 50, 1.6, 435.50632796465896),
      Row("D", 50, 1.6, 8.302818930273858),
      Row("A", 50, 1.6, 4.190548888271846),
      Row("T", 50, 1.6, 4.107806791630812),
      Row("H", 50, 4.0, 971.5021722864428),
      Row("D", 50, 4.0, 20.265768716115296),
      Row("A", 50, 4.0, 9.929653529123234),
      Row("T", 50, 4.0, 9.947922203780442),
      Row("H", 75, 1.0, 345.16546864116725),
      Row("D", 75, 1.0, 6.429765495947489),
      Row("A", 75, 1.0, 2.9940145141569707),
      Row("T", 75, 1.0, 2.8785866716549116),
      Row("H", 75, 1.6, 490.23965196380135),
      Row("D", 75, 1.6, 9.934783387995434),
      Row("A", 75, 1.6, 4.477933364970944),
      Row("T", 75, 1.6, 4.416057083428898),
      Row("H", 75, 4.0, 1024.0),
      Row("D", 75, 4.0, 23.954854956187205),
      Row("A", 75, 4.0, 10.413608768226835),
      Row("T", 75, 4.0, 10.565938730524842),
      Row("H", 100, 1.0, 365.3052928104405),
      Row("D", 100, 1.0, 7.426550656639984),
      Row("A", 100, 1.0, 3.165098604246019),
      Row("T", 100, 1.0, 3.06104797484784),
      Row("H", 100, 1.6, 514.7784981524583),
      Row("D", 100, 1.6, 11.356497007652457),
      Row("A", 100, 1.6, 4.676954310352285),
      Row("T", 100, 1.6, 4.654082853159715),
      Row("H", 100, 4.0, 1024.0),
      Row("D", 100, 4.0, 27.076282411702348),
      Row("A", 100, 4.0, 10.724377134777344),
      Row("T", 100, 4.0, 11.026222366407216),
    ).forEach { (stat, level, classMul, expected) ->
      val actual = when (stat) {
        "H" -> Stats.health(formula.health, 20.0, classMul, level)
        "D" -> Stats.damage(formula.damage, 3.0, classMul, level)
        "A" -> Stats.armor(formula.armor, 2.0, classMul, level)
        else -> Stats.armor(formula.toughness, 2.0, classMul, level)
      }
      assertEquals(expected, actual, 1e-9, "$stat at level $level, class ×$classMul")
    }
  }

  @Test fun `minion counts match 1x`() {
    val exact = formula.minions.copy(variance = 0.0)
    mapOf(
      1 to 2,
      5 to 3,
      10 to 4,
      25 to 6,
      50 to 8,
      75 to 9,
      100 to 10,
    ).forEach { (level, count) -> assertEquals(count, Rolls.minions(exact, level, Random(1)), "level $level") }
    mapOf(
      1 to 2,
      5 to 3,
      10 to 3,
      25 to 5,
      50 to 7,
      75 to 8,
      100 to 9,
    ).forEach { (level, count) -> assertEquals(count, Rolls.minions(formula.minions, level, Random(42)), "level $level, ±15%") }
  }

  @Test fun `mini boss levels match 1x`() {
    mapOf(
      1 to 1,
      5 to 3,
      10 to 7,
      25 to 18,
      50 to 40,
      75 to 63,
      100 to 86,
    ).forEach { (parent, child) -> assertEquals(child, Leveling.childLevel(parent, settings.progression.miniBossesLevelCurve, Random(7)), "parent $parent") }
  }

  @Test fun `linear ability scaling matches 1x`() {
    listOf(
      Triple(1, 6, 5.1),
      Triple(5, 6, 5.5),
      Triple(10, 7, 6.0),
      Triple(25, 8, 7.5),
      Triple(50, 11, 10.0),
      Triple(75, 13, 12.5),
      Triple(100, 16, 15.0),
    ).forEach { (level, strikes, damage) ->
      assertEquals(strikes, Rolls.linear(6, 0.1, 26, level))
      assertEquals(damage, Rolls.linear(5.0, 0.1, 20.0, level), 1e-9)
    }
  }

  @Test fun `a bias of 1 or less leaves a cooldown as it is`() {
    // 1.x threw here, since Random.nextDouble(1.0, 1.0) has an empty range.
    assertEquals(5000.0, Rolls.biased(5000.0, 1.0, Random(1)))
  }
}
