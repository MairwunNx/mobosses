package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.boss.SpawnOptions
import ru.mairwunnx.mobosses.boss.SpawnOptions.Parsed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnOptionsTest {
  private val known = setOf("shockwave", "whirlwind", "enrage")

  private fun ok(vararg args: String) = (SpawnOptions.parse(args.toList(), known) as Parsed.Ok).options

  @Test fun `the place and options come in any order`() {
    val options = ok("abilities=shockwave,Whirlwind", "0", "-60", "4", "count=3", "world")
    assertEquals(listOf("0", "-60", "4", "world"), options.place)
    assertEquals(setOf("shockwave", "whirlwind"), options.abilities)
    assertEquals(3, options.count)
  }

  @Test fun `no options leave everything to the boss's level`() {
    assertEquals(SpawnOptions(), ok())
    assertEquals(emptySet(), ok("abilities=none").abilities)
  }

  @Test fun `what it can't read it names`() {
    assertEquals(Parsed.Bad("abilities=meteor"), SpawnOptions.parse(listOf("abilities=meteor"), known))
    assertEquals(Parsed.Bad("count=0"), SpawnOptions.parse(listOf("count=0"), known))
    assertEquals(Parsed.Bad("count=21"), SpawnOptions.parse(listOf("count=21"), known))
    assertEquals(Parsed.Bad("speed=2"), SpawnOptions.parse(listOf("speed=2"), known))
    assertEquals(Parsed.Bad("1 2"), SpawnOptions.parse(listOf("1", "2"), known))
  }

  @Test fun `given abilities go off whatever the level, and nothing else does`() {
    assertTrue(SpawnOptions.allows(setOf("shockwave"), "shockwave", reached = false))
    assertTrue(!SpawnOptions.allows(setOf("shockwave"), "whirlwind", reached = true))
    assertTrue(SpawnOptions.allows(null, "whirlwind", reached = true))
    assertEquals(setOf("a", "b"), SpawnOptions.decode(SpawnOptions.encode(setOf("b", "a"))))
    assertEquals(emptySet(), SpawnOptions.decode(SpawnOptions.encode(emptySet())))
  }
}
