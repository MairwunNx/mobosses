package ru.mairwunnx.mobosses.metrics

import ru.mairwunnx.mobosses.config.Classes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * What bStats sends every 30 minutes: counts since the last send. The server thread adds, bStats' own
 * thread drains, so every count is atomic and a drain hands each over once.
 */
class Tallies {
  val spawned = Count()
  val killed = Count()
  val spawnsByGrade = Tally()
  val killsByGrade = Tally()
  val killsByGradeMob = Drilldown()
  val languages = Tally()
  val languageSources = Tally()

  /** A boss of class [clazz] spawned. */
  fun spawn(clazz: String) {
    spawned.add()
    spawnsByGrade.add(grade(clazz))
  }

  /** Players killed a boss of [mob], the game's key (`zombie`), and class [clazz]. */
  fun kill(mob: String, clazz: String) {
    killed.add()
    val grade = grade(clazz)
    killsByGrade.add(grade)
    killsByGradeMob.add(grade, mob)
  }

  /** The plugin picked [code] for a player, from [source]: `ip`, `client`, `default`, or `choice`. */
  fun language(code: String, source: String) {
    languages.add(code)
    languageSources.add(source)
  }

  class Count {
    private val value = AtomicInteger()
    fun add() { value.incrementAndGet() }
    fun drain(): Int = value.getAndSet(0)
  }

  /** Counts by key. Keys stay once seen, so a drain never races a new count; they come from short fixed sets. */
  class Tally {
    private val counts = ConcurrentHashMap<String, AtomicInteger>()
    fun add(key: String) { counts.computeIfAbsent(key) { AtomicInteger() }.incrementAndGet() }
    fun drain(): Map<String, Int> = buildMap { counts.forEach { (key, count) -> count.getAndSet(0).takeIf { it > 0 }?.let { put(key, it) } } }
  }

  /** Counts by key and subkey, for a pie that opens into another. */
  class Drilldown {
    private val tallies = ConcurrentHashMap<String, Tally>()
    fun add(key: String, sub: String) = tallies.computeIfAbsent(key) { Tally() }.add(sub)
    fun drain(): Map<String, Map<String, Int>> = buildMap { tallies.forEach { (key, tally) -> tally.drain().takeIf { it.isNotEmpty() }?.let { put(key, it) } } }
  }

  companion object {
    /** A class's grade: one of the six, or `custom` for any other. */
    fun grade(clazz: String): String = clazz.trim().lowercase().takeIf { it in Classes.KEYS } ?: "custom"
  }
}
