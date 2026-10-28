package ru.mairwunnx.mobosses.boss

/**
 * The Ender Dragon and the Wither, whose fights the game stages itself. They fly, so nothing traps them; they never
 * despawn, since each is a fight someone set up; and abilities that move them, pull a player to them, copy them, or
 * build around them stay off, since their fights hang on where they are. The dragon becomes a boss only once its first
 * fight is won, when the game brings it back.
 */
object WorldBosses {
  /** Their types' keys in `bosses.yml`. */
  val TYPES = setOf("ender_dragon", "wither")

  /** Abilities a world boss never casts. */
  val GROUNDED = setOf("teleport_strike", "grapple", "chain", "doubles", "obelisk", "fire_ring", "web_ring")

  fun isWorldBoss(typeKey: String) = typeKey.lowercase() in TYPES

  /** Whether a boss of [typeKey] may cast [ability]. */
  fun allows(typeKey: String, ability: String) = !isWorldBoss(typeKey) || ability !in GROUNDED
}
