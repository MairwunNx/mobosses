package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.config.DifficultySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DifficultyTest {
  private val settings = DifficultySettings()

  @Test fun `peaceful makes no bosses, switched off or not`() {
    assertNull(Difficulty.modifier(settings, Difficulty.PEACEFUL, hardcore = false))
    assertNull(Difficulty.modifier(settings.copy(enabled = false), Difficulty.PEACEFUL, hardcore = false))
  }

  @Test fun `each difficulty takes its modifier, hardcore its own`() {
    assertEquals(0.7333333337, Difficulty.modifier(settings, Difficulty.EASY, hardcore = false))
    assertEquals(0.8777777773, Difficulty.modifier(settings, Difficulty.NORMAL, hardcore = false))
    assertEquals(1.0, Difficulty.modifier(settings, Difficulty.HARD, hardcore = false))
    assertEquals(1.2333333339, Difficulty.modifier(settings, Difficulty.HARD, hardcore = true))
  }

  @Test fun `switched off, every difficulty plays as hard`() {
    val off = settings.copy(enabled = false)
    Difficulty.entries.drop(1).forEach { assertEquals(1.0, Difficulty.modifier(off, it, hardcore = true)) }
  }

  @Test fun `a part takes its weight of the modifier`() {
    assertEquals(0.8, Difficulty.part(0.6, 0.5), 1e-12)
    assertEquals(1.0, Difficulty.part(0.6, 0.0), 1e-12)
    assertEquals(1.2333333339, Difficulty.part(1.2333333339, 1.0), 1e-12)
    // Weights past 1 or below 0 count as their nearest end.
    assertEquals(0.6, Difficulty.part(0.6, 3.0), 1e-12)
  }
}
