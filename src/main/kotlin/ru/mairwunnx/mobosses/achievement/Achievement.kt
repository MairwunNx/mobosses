package ru.mairwunnx.mobosses.achievement

import org.bukkit.Material

/**
 * Mo’Bosses' advancements, in a tab of their own: the hunt's path by level and kills, the ladder of
 * classes, the bestiary, and feats from the fights. Their names live in lang/ under
 * `achievements.<id>.title` and `.description`.
 */
enum class Achievement(val id: String, val parent: Achievement?, val icon: Material, val frame: Frame) {
  HUNT_BEGINS("hunt_begins", null, Material.ZOMBIE_HEAD, Frame.TASK),
  FIRST_BLOOD("first_blood", HUNT_BEGINS, Material.IRON_SWORD, Frame.TASK),

  LEVEL_10("level_10", FIRST_BLOOD, Material.EXPERIENCE_BOTTLE, Frame.TASK),
  LEVEL_25("level_25", LEVEL_10, Material.EXPERIENCE_BOTTLE, Frame.TASK),
  LEVEL_50("level_50", LEVEL_25, Material.EXPERIENCE_BOTTLE, Frame.GOAL),
  LEVEL_75("level_75", LEVEL_50, Material.EXPERIENCE_BOTTLE, Frame.GOAL),
  LEVEL_100("level_100", LEVEL_75, Material.NETHER_STAR, Frame.CHALLENGE),

  KILLS_10("kills_10", FIRST_BLOOD, Material.BONE, Frame.TASK),
  KILLS_100("kills_100", KILLS_10, Material.SKELETON_SKULL, Frame.GOAL),
  KILLS_500("kills_500", KILLS_100, Material.WITHER_SKELETON_SKULL, Frame.GOAL),
  KILLS_1000("kills_1000", KILLS_500, Material.DRAGON_HEAD, Frame.CHALLENGE),

  CLASS_UNCOMMON("class_uncommon", FIRST_BLOOD, Material.YELLOW_DYE, Frame.TASK),
  CLASS_RARE("class_rare", CLASS_UNCOMMON, Material.LIGHT_BLUE_DYE, Frame.TASK),
  CLASS_EPIC("class_epic", CLASS_RARE, Material.PURPLE_DYE, Frame.GOAL),
  CLASS_LEGENDARY("class_legendary", CLASS_EPIC, Material.GOLDEN_APPLE, Frame.GOAL),
  CLASS_MYTHIC("class_mythic", CLASS_LEGENDARY, Material.ENCHANTED_GOLDEN_APPLE, Frame.CHALLENGE),

  BESTIARY("bestiary", FIRST_BLOOD, Material.WRITABLE_BOOK, Frame.GOAL),
  MYTHIC_COLLECTOR("mythic_collector", BESTIARY, Material.ENCHANTED_BOOK, Frame.CHALLENGE),

  OBELISK_BREAKER("obelisk_breaker", FIRST_BLOOD, Material.END_CRYSTAL, Frame.TASK),
  CHAIN_BREAKER("chain_breaker", FIRST_BLOOD, Material.IRON_CHAIN, Frame.TASK),
  FIND_THE_REAL("find_the_real", FIRST_BLOOD, Material.SPYGLASS, Frame.GOAL),
  SECOND_LIFE("second_life", FIRST_BLOOD, Material.SOUL_LANTERN, Frame.GOAL),
  LONE_WOLF("lone_wolf", FIRST_BLOOD, Material.NETHERITE_SWORD, Frame.GOAL),
  UNTOUCHED("untouched", FIRST_BLOOD, Material.SHIELD, Frame.CHALLENGE),

  FULL_MOON_HUNTER("full_moon_hunter", FIRST_BLOOD, Material.CLOCK, Frame.TASK),
  AVENGER("avenger", FIRST_BLOOD, Material.CROSSBOW, Frame.GOAL),
  ;

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
