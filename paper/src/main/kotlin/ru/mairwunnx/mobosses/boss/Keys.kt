package ru.mairwunnx.mobosses.boss

import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/** The keys under which Mo’Bosses keeps its data on entities and players, so it survives relogs, restarts, and chunk unloads. */
class Keys(plugin: Plugin) {
  /** The boss type, like `zombie`. */
  val type = NamespacedKey(plugin, "type")
  val clazz = NamespacedKey(plugin, "class")
  val level = NamespacedKey(plugin, "level")
  /** Who dealt how much: `uuid=damage;…`. */
  val damage = NamespacedKey(plugin, "damage")
  /** The part of [damage] each player's pets dealt: `uuid=damage;…`. */
  val petDamage = NamespacedKey(plugin, "pet_damage")
  /** When each player, or their pet, last hit the boss: `uuid=millis;…`. */
  val hitAt = NamespacedKey(plugin, "hit_at")
  /** When a player was last near the boss, in epoch milliseconds, for despawning. */
  val seen = NamespacedKey(plugin, "seen")
  /** On bosses from `/mobosses spawn`, which don't despawn. */
  val pinned = NamespacedKey(plugin, "pinned")
  /** On pets and golems that hit a boss: `boss-uuid=millis`, until when its abilities may hit them back. */
  val aggressor = NamespacedKey(plugin, "aggressor")
  /** When each ability may go off again, in epoch milliseconds: `ability=millis;…`, with `*` for the shared cooldown. */
  val cooldowns = NamespacedKey(plugin, "ready")
  /** On ability arrows: they hit players only. */
  val arrow = NamespacedKey(plugin, "arrow")
  val enraged = NamespacedKey(plugin, "enraged")
  /** On minions: the type of the boss that summoned them, for their name. */
  val minionOf = NamespacedKey(plugin, "minion_of")
  /** On minions, and on bosses that were minions: the UUID of the boss that summoned them. */
  val summoner = NamespacedKey(plugin, "summoner")
  /** On meteors: they land without leaving a block. */
  val meteor = NamespacedKey(plugin, "meteor")
  /** On bosses: the difficulty's modifier it was made with; bosses from before 2.11 have none and count as hard. */
  val difficulty = NamespacedKey(plugin, "difficulty")
  /** On bosses past their second phase. */
  val phaseTwo = NamespacedKey(plugin, "phase_two")
  /** On burning bosses: who set them alight, `uuid` or `uuid|pet`, so the fire's damage is theirs. */
  val igniter = NamespacedKey(plugin, "igniter")
  /** On obelisks: the boss they heal; and the hits they have left. */
  val obelisk = NamespacedKey(plugin, "obelisk")
  val obeliskHits = NamespacedKey(plugin, "obelisk_hits")
  /** On a boss's copies: the boss they copy. */
  val decoy = NamespacedKey(plugin, "decoy")
  /** On bosses: the max health they started with, the health groups added, and how many fighters it counts. */
  val baseHealth = NamespacedKey(plugin, "base_health")
  val addedHealth = NamespacedKey(plugin, "added_health")
  val fighters = NamespacedKey(plugin, "fighters")
  /** On mobs that haven't rolled yet: the reason they spawned, until a player sees them. */
  val pending = NamespacedKey(plugin, "pending")
  /** On bosses from spawners, for spawner fatigue. */
  val fromSpawner = NamespacedKey(plugin, "from_spawner")
  /** On players: loot drops since their last rare or better item, and the count that brings one. */
  val lootDry = NamespacedKey(plugin, "loot_dry")
  val lootPityAt = NamespacedKey(plugin, "loot_pity_at")
  /** On players: their recent kills of spawner bosses, `world|x|z|millis;…`. */
  val spawnerKills = NamespacedKey(plugin, "spawner_kills")
  /** On bosses: the players it hurt, `uuid;…`, for the “untouched” achievement. */
  val hurt = NamespacedKey(plugin, "hurt")
  /** On players: the boss type that last killed them and when, `type|millis`, for the “avenger” achievement. */
  val avenge = NamespacedKey(plugin, "avenge")
  /** On name labels, so a stray one from a crash can be cleared. */
  val label = NamespacedKey(plugin, "label")
  /** On players: when they may get rewards again, in epoch milliseconds. */
  val rewardsUntil = NamespacedKey(plugin, "rewards_until")
  /** On players: minutes played since their last death, for the survival bonus. */
  val survival = NamespacedKey(plugin, "survival_minutes")
  /** On players: when a boss may spawn for them again, in epoch milliseconds. */
  val nextBoss = NamespacedKey(plugin, "next_boss")
}
