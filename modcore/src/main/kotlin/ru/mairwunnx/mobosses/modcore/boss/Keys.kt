package ru.mairwunnx.mobosses.modcore.boss

/**
 * The names under which Mo’Bosses keeps its data on entities and players ([ru.mairwunnx.mobosses.modcore.data.Data]),
 * the same as Paper's keys without their `mobosses:` namespace, so a world moved between platforms reads alike.
 */
object Keys {
  /** The boss type, like `zombie`. */
  const val TYPE = "type"
  const val CLASS = "class"
  const val LEVEL = "level"
  /** Who dealt how much: `uuid=damage;…`. */
  const val DAMAGE = "damage"
  /** The part of [DAMAGE] each player's pets dealt. */
  const val PET_DAMAGE = "pet_damage"
  /** When each player, or their pet, last hit the boss: `uuid=millis;…`. */
  const val HIT_AT = "hit_at"
  /** When a player was last near the boss, for despawning. */
  const val SEEN = "seen"
  /** On bosses from `/mobosses spawn`, which don't despawn. */
  const val PINNED = "pinned"
  /** On pets and golems that hit a boss: `boss-uuid=millis`, until when its abilities may hit them back. */
  const val AGGRESSOR = "aggressor"
  /** When each ability may go off again: `ability=millis;…`, with `*` for the shared cooldown. */
  const val COOLDOWNS = "ready"
  /** On ability arrows: they hit players only. */
  const val ARROW = "arrow"
  const val ENRAGED = "enraged"
  /** On minions: the type of the boss that summoned them, for their name. */
  const val MINION_OF = "minion_of"
  /** On minions, and on bosses that were minions: the UUID of the boss that summoned them. */
  const val SUMMONER = "summoner"
  /** On meteors: they land without leaving a block. */
  const val METEOR = "meteor"
  /** On bosses: the difficulty's modifier it was made with. */
  const val DIFFICULTY = "difficulty"
  /** On bosses past their second phase. */
  const val PHASE_TWO = "phase_two"
  /** On burning bosses: who set them alight, `uuid` or `uuid|pet`. */
  const val IGNITER = "igniter"
  /** On obelisks: the boss they heal; and the hits they have left. */
  const val OBELISK = "obelisk"
  const val OBELISK_HITS = "obelisk_hits"
  /** On a boss's copies: the boss they copy. */
  const val DECOY = "decoy"
  /** On bosses: the max health they started with, the health groups added, and how many fighters it counts. */
  const val BASE_HEALTH = "base_health"
  const val ADDED_HEALTH = "added_health"
  const val FIGHTERS = "fighters"
  /** On mobs that haven't rolled yet: the reason they spawned, until a player sees them. */
  const val PENDING = "pending"
  /** On bosses from spawners, for spawner fatigue. */
  const val FROM_SPAWNER = "from_spawner"
  /** On players: loot drops since their last rare or better item, and the count that brings one. */
  const val LOOT_DRY = "loot_dry"
  const val LOOT_PITY_AT = "loot_pity_at"
  /** On players: their recent kills of spawner bosses. */
  const val SPAWNER_KILLS = "spawner_kills"
  /** On bosses: the players it hurt, `uuid;…`, for the “untouched” achievement. */
  const val HURT = "hurt"
  /** On players: the boss type that last killed them and when, `type|millis`. */
  const val AVENGE = "avenge"
  /** On players: when they may get rewards again. */
  const val REWARDS_UNTIL = "rewards_until"
  /** On players: minutes played since their last death, for the survival bonus. */
  const val SURVIVAL = "survival_minutes"
  /** On players: when a boss may spawn for them again. */
  const val NEXT_BOSS = "next_boss"
}
