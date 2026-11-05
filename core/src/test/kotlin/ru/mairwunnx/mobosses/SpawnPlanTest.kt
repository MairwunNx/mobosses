package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.boss.SpawnPlan.Spot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpawnPlanTest {
  @Test fun `the level dial steps one digit and stays from 1 to the top`() {
    assertEquals(3, SpawnPlan.places(100))
    assertEquals(2, SpawnPlan.places(99))
    assertEquals(7, SpawnPlan.digit(47, 0))
    assertEquals(4, SpawnPlan.digit(47, 1))
    assertEquals(0, SpawnPlan.digit(47, 2))
    assertEquals(57, SpawnPlan.step(47, 1, up = true, max = 100))
    assertEquals(100, SpawnPlan.step(95, 1, up = true, max = 100))
    assertEquals(1, SpawnPlan.step(5, 1, up = false, max = 100))
    assertEquals(46, SpawnPlan.step(47, 0, up = false, max = 100))
  }

  @Test fun `the admin stands in the middle of the grid, north at the top`() {
    assertEquals(22, SpawnPlan.CENTER)
    assertEquals(0 to 0, SpawnPlan.offset(SpawnPlan.CENTER))
    assertEquals(-4 to -2, SpawnPlan.offset(0))
    assertEquals(4 to 2, SpawnPlan.offset(44))
    assertNull(SpawnPlan.offset(45))
  }

  @Test fun `the safest spot is at the admin's height, then near the chunk's middle`() {
    val spots = listOf(Spot(0, 70, 0), Spot(8, 64, 8), Spot(1, 64, 1), Spot(15, 65, 15))
    assertEquals(Spot(8, 64, 8), SpawnPlan.safest(spots, 64, 8.0, 8.0))
    assertEquals(Spot(0, 70, 0), SpawnPlan.safest(spots, 72, 8.0, 8.0))
    assertNull(SpawnPlan.safest(emptyList(), 64, 8.0, 8.0))
  }
}
