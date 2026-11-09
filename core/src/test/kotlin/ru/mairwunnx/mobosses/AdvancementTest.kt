package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.achievement.Advancement
import ru.mairwunnx.mobosses.lang.Languages
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancementTest {
  private val languages = Languages(mapOf("en" to emptyMap()), "en")

  @Test fun `the bestiary and the Mythic collection count each boss type, the rest but kill tiers have one criterion`() {
    val types = setOf("zombie", "creeper", "skeleton")
    val each = listOf(listOf("creeper"), listOf("skeleton"), listOf("zombie"))
    assertEquals(each, Advancement.requirements(Achievement.BESTIARY, types))
    assertEquals(each, Advancement.requirements(Achievement.MYTHIC_COLLECTOR, types))
    assertEquals(listOf(listOf(Advancement.CRITERION)), Advancement.requirements(Achievement.LEVEL_10, types))
    assertEquals(listOf(listOf(Advancement.CRITERION)), Advancement.requirements(Achievement.BESTIARY, emptySet()))
  }

  @Test fun `a kill tier has a step per kill, each met by its kill or by the tier's one criterion`() {
    for ((goal, tier) in Achievement.KILLS) {
      val steps = Advancement.requirements(tier, emptySet())
      assertEquals(goal, steps.size)
      assertEquals(goal, Advancement.goal(tier))
      assertEquals(listOf("1", Advancement.CRITERION), steps.first())
      assertEquals(listOf(goal.toString(), Advancement.CRITERION), steps.last())
    }
    assertEquals(listOf(10, 100, 500, 1000), Achievement.KILLS.map { it.first })
    assertEquals(null, Advancement.goal(Achievement.BESTIARY))
  }

  @Test fun `a kill count awards its kills, and the goal the tier's one criterion`() {
    val all = Advancement.requirements(Achievement.KILLS_10, emptySet()).flatten().distinct()
    assertEquals(listOf("1", "2", "3"), Advancement.counted(10, 3, all))
    assertEquals(emptyList(), Advancement.counted(10, 0, all))
    assertEquals(listOf("3"), Advancement.counted(10, 3, all - setOf("1", "2")))
    assertEquals(listOf(Advancement.CRITERION), Advancement.counted(10, 10, all))
    assertEquals(listOf(Advancement.CRITERION), Advancement.counted(10, 347, all - setOf("1")))
    // A data pack's list without the tier's criterion gets every step at the goal.
    assertEquals(listOf("4", "5"), Advancement.counted(5, 5, listOf("4", "5")))
  }

  @Test fun `each list is a requirement, so the game shows N of M`() {
    val json = Advancement.json(Achievement.BESTIARY, "en", languages, { "mobosses:${it.id}" }, requirements = listOf(listOf("creeper"), listOf("zombie")))
    assertEquals(setOf("creeper", "zombie"), json.getAsJsonObject("criteria").keySet())
    assertEquals("[[\"creeper\"],[\"zombie\"]]", json.getAsJsonArray("requirements").toString())
    val tier = Advancement.json(Achievement.KILLS_10, "en", languages, { "mobosses:${it.id}" }, requirements = Advancement.requirements(Achievement.KILLS_10, emptySet()))
    assertEquals(11, tier.getAsJsonObject("criteria").size())
    assertEquals(10, tier.getAsJsonArray("requirements").size())
    assertEquals("[\"1\",\"done\"]", tier.getAsJsonArray("requirements")[0].toString())
    assertEquals("mobosses:first_blood", tier.get("parent").asString)
    assertEquals("task", tier.getAsJsonObject("display").get("frame").asString)
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
