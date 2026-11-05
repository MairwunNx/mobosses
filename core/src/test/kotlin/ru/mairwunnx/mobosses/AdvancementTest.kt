package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.achievement.Advancement
import ru.mairwunnx.mobosses.lang.Languages
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancementTest {
  private val languages = Languages(mapOf("en" to emptyMap()), "en")

  @Test fun `the bestiary counts each boss type, the rest have one criterion`() {
    val types = setOf("zombie", "creeper", "skeleton")
    assertEquals(listOf("creeper", "skeleton", "zombie"), Advancement.criteria(Achievement.BESTIARY, types))
    assertEquals(listOf("creeper", "skeleton", "zombie"), Advancement.criteria(Achievement.MYTHIC_COLLECTOR, types))
    assertEquals(listOf(Advancement.CRITERION), Advancement.criteria(Achievement.KILLS_10, types))
    assertEquals(listOf(Advancement.CRITERION), Advancement.criteria(Achievement.BESTIARY, emptySet()))
  }

  @Test fun `each criterion is a requirement of its own, so the game shows N of M`() {
    val json = Advancement.json(Achievement.BESTIARY, "en", languages, { "mobosses:${it.id}" }, names = listOf("creeper", "zombie"))
    assertEquals(setOf("creeper", "zombie"), json.getAsJsonObject("criteria").keySet())
    assertEquals("[[\"creeper\"],[\"zombie\"]]", json.getAsJsonArray("requirements").toString())
  }

  @Test fun `a kill ticks off its type, and every type of the server ticks off the rest`() {
    val types = setOf("zombie", "creeper")
    assertEquals(listOf("zombie"), Advancement.collected(listOf("zombie", "creeper"), setOf("zombie", "husk"), types))
    // A list from 1.x names or an older bosses.yml still completes once the server's own types are in.
    assertEquals(listOf("pig_zombie", "creeper"), Advancement.collected(listOf("pig_zombie", "creeper"), setOf("zombie", "creeper"), types))
    assertEquals(listOf(Advancement.CRITERION), Advancement.collected(listOf(Advancement.CRITERION), types, types))
    assertEquals(emptyList(), Advancement.collected(listOf(Advancement.CRITERION), setOf("zombie"), types))
  }
}
