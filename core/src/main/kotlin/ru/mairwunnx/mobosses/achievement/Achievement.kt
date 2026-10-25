package ru.mairwunnx.mobosses.achievement

/**
 * Mo’Bosses' advancements, in a tab of their own: the hunt's path by level and kills, the ladder of
 * classes, the bestiary, and feats from the fights. Their names live in lang/ under
 * `achievements.<id>.title` and `.description`. [Advancement] writes them as the game reads them.
 */
enum class Achievement(val id: String, val parent: Achievement?, icon: String, val frame: Frame) {
  HUNT_BEGINS("hunt_begins", null, "zombie_head", Frame.TASK),
  FIRST_BLOOD("first_blood", HUNT_BEGINS, "iron_sword", Frame.TASK),

  LEVEL_10("level_10", FIRST_BLOOD, "experience_bottle", Frame.TASK),
  LEVEL_25("level_25", LEVEL_10, "experience_bottle", Frame.TASK),
  LEVEL_50("level_50", LEVEL_25, "experience_bottle", Frame.GOAL),
  LEVEL_75("level_75", LEVEL_50, "experience_bottle", Frame.GOAL),
  LEVEL_100("level_100", LEVEL_75, "nether_star", Frame.CHALLENGE),

  KILLS_10("kills_10", FIRST_BLOOD, "bone", Frame.TASK),
  KILLS_100("kills_100", KILLS_10, "skeleton_skull", Frame.GOAL),
  KILLS_500("kills_500", KILLS_100, "wither_skeleton_skull", Frame.GOAL),
  KILLS_1000("kills_1000", KILLS_500, "dragon_head", Frame.CHALLENGE),

  CLASS_UNCOMMON("class_uncommon", FIRST_BLOOD, "yellow_dye", Frame.TASK),
  CLASS_RARE("class_rare", CLASS_UNCOMMON, "light_blue_dye", Frame.TASK),
  CLASS_EPIC("class_epic", CLASS_RARE, "purple_dye", Frame.GOAL),
  CLASS_LEGENDARY("class_legendary", CLASS_EPIC, "golden_apple", Frame.GOAL),
  CLASS_MYTHIC("class_mythic", CLASS_LEGENDARY, "enchanted_golden_apple", Frame.CHALLENGE),

  BESTIARY("bestiary", FIRST_BLOOD, "writable_book", Frame.GOAL),
  MYTHIC_COLLECTOR("mythic_collector", BESTIARY, "enchanted_book", Frame.CHALLENGE),

  OBELISK_BREAKER("obelisk_breaker", FIRST_BLOOD, "end_crystal", Frame.TASK),
  CHAIN_BREAKER("chain_breaker", FIRST_BLOOD, "iron_chain", Frame.TASK),
  FIND_THE_REAL("find_the_real", FIRST_BLOOD, "spyglass", Frame.GOAL),
  SECOND_LIFE("second_life", FIRST_BLOOD, "soul_lantern", Frame.GOAL),
  LONE_WOLF("lone_wolf", FIRST_BLOOD, "netherite_sword", Frame.GOAL),
  UNTOUCHED("untouched", FIRST_BLOOD, "shield", Frame.CHALLENGE),

  FULL_MOON_HUNTER("full_moon_hunter", FIRST_BLOOD, "clock", Frame.TASK),
  AVENGER("avenger", FIRST_BLOOD, "crossbow", Frame.GOAL),
  ;

  /** The item it shows, by its id in the game. */
  val icon = "minecraft:$icon"

  enum class Frame { TASK, GOAL, CHALLENGE }

  companion object {
    /** The class ladder by class key. */
    val CLASSES = mapOf("uncommon" to CLASS_UNCOMMON, "rare" to CLASS_RARE, "epic" to CLASS_EPIC, "legendary" to CLASS_LEGENDARY, "mythic" to CLASS_MYTHIC)

    val LEVELS = listOf(10 to LEVEL_10, 25 to LEVEL_25, 50 to LEVEL_50, 75 to LEVEL_75, 100 to LEVEL_100)

    val KILLS = listOf(10 to KILLS_10, 100 to KILLS_100, 500 to KILLS_500, 1000 to KILLS_1000)

    /** The language keys every language file sets for them. */
    val TEXT_KEYS: List<String> = entries.flatMap { listOf("achievements.${it.id}.title", "achievements.${it.id}.description") }
  }
}
