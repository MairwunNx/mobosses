package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.combat.Cheese
import ru.mairwunnx.mobosses.config.AntiCheese
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheeseTest {
  private val settings = AntiCheese.StepOut()

  /** Samples once a second from [from] to [to] seconds; true when any of them says to step out. */
  private fun Cheese.run(from: Int, to: Int, pressed: Boolean = true, blocked: Boolean = false, x: (Int) -> Double = { 0.0 }) =
    (from..to).map { sample(settings, x(it), 64.0, 0.0, it * 1000L, pressed, blocked) }.any { it }

  @Test fun `a boss that stands still while hit from afar steps out after stuck_seconds`() {
    val cheese = Cheese()
    assertFalse(cheese.run(0, 5))
    assertTrue(cheese.run(6, 6))
  }

  @Test fun `a boss that keeps moving is never stuck`() {
    assertFalse(Cheese().run(0, 30) { it * 2.0 })
  }

  @Test fun `a boss that moves but can't reach steps out after unreachable_seconds`() {
    val cheese = Cheese()
    assertFalse(cheese.run(0, 7, blocked = true) { it * 2.0 })
    assertTrue(cheese.run(8, 8, blocked = true) { it * 2.0 })
  }

  @Test fun `a fight in reach resets the watch`() {
    val cheese = Cheese()
    assertFalse(cheese.run(0, 4))
    assertFalse(cheese.run(5, 5, pressed = false))
    assertFalse(cheese.run(6, 11))
    assertTrue(cheese.run(12, 12))
  }

  @Test fun `after stepping out the boss waits cooldown_seconds`() {
    val cheese = Cheese()
    assertTrue(cheese.run(0, 6))
    cheese.stepped(6000)
    assertFalse(cheese.run(7, 25))
    assertTrue(cheese.run(26, 26))
  }

  @Test fun `switched off, nothing counts`() {
    val cheese = Cheese()
    assertFalse((0..60).any { cheese.sample(settings.copy(enabled = false), 0.0, 64.0, 0.0, it * 1000L, pressed = true, blocked = true) })
  }
}
