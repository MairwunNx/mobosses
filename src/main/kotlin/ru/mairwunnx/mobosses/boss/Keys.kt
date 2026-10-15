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
  /** On name labels, so a stray one from a crash can be cleared. */
  val label = NamespacedKey(plugin, "label")
  /** On players: when they may get rewards again, in epoch milliseconds. */
  val rewardsUntil = NamespacedKey(plugin, "rewards_until")
  /** On players: when a boss may spawn for them again, in epoch milliseconds. */
  val nextBoss = NamespacedKey(plugin, "next_boss")
}
