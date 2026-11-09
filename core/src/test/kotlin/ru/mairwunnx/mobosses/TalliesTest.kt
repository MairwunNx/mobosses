package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.metrics.Tallies
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class TalliesTest {
  @Test fun `a drain hands each count over once`() {
    val tallies = Tallies()
    tallies.spawn("epic")
    tallies.spawn("Mythic")
    tallies.kill("zombie", "epic")
    tallies.kill("husk", "epic")
    tallies.kill("spider", "boss_of_bosses")
    assertEquals(2, tallies.spawned.drain())
    assertEquals(0, tallies.spawned.drain())
    assertEquals(3, tallies.killed.drain())
    assertEquals(mapOf("epic" to 1, "mythic" to 1), tallies.spawnsByGrade.drain())
    assertEquals(mapOf("epic" to 2, "custom" to 1), tallies.killsByGrade.drain())
    assertEquals(mapOf("epic" to mapOf("zombie" to 1, "husk" to 1), "custom" to mapOf("spider" to 1)), tallies.killsByGradeMob.drain())
    assertEquals(emptyMap(), tallies.killsByGradeMob.drain())
    assertEquals(emptyMap(), tallies.killsByGrade.drain())
  }

  @Test fun `languages count each pick and its source`() {
    val tallies = Tallies()
    tallies.language("ru", "ip")
    tallies.language("ru", "choice")
    tallies.language("en", "default")
    assertEquals(mapOf("ru" to 2, "en" to 1), tallies.languages.drain())
    assertEquals(mapOf("ip" to 1, "choice" to 1, "default" to 1), tallies.languageSources.drain())
  }

  @Test fun `counts from many threads all arrive`() {
    val tally = Tallies.Tally()
    var drained = 0
    val adders = List(4) { thread { repeat(10_000) { tally.add("x") } } }
    val drainer = thread { repeat(200) { drained += tally.drain()["x"] ?: 0 } }
    adders.forEach(Thread::join)
    drainer.join()
    drained += tally.drain()["x"] ?: 0
    assertEquals(40_000, drained)
  }
}
