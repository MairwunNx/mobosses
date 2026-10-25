package ru.mairwunnx.mobosses.progress

import org.jetbrains.exposed.v1.core.Table

/** A player's Mo’Bosses record: bosses they helped kill and the experience those kills brought. */
data class Progress(val kills: Int = 0, val experience: Long = 0)

/** A row of the hunters' board. */
data class Hunter(val uuid: String, val name: String?, val progress: Progress)

/** How many bosses of a type and class spawned and died to players. */
data class BossCount(val type: String, val clazz: String, val spawned: Long, val killed: Long)

/** The table 1.x made; its name, columns, and index stay, so 1.x databases keep working. `name` came in 2.2. */
object PlayerProgressTable : Table("player_progress") {
  val uuid = varchar("uuid", 36).index()
  val bossKills = integer("boss_kills")
  val totalExperience = long("total_experience")
  /** The player's name as of their last kill, for boards. */
  val name = varchar("name", 16).nullable()

  override val primaryKey = PrimaryKey(uuid)
}

/** A player's kills of one boss type and class, for the bestiary. */
data class BestiaryEntry(val type: String, val clazz: String, val kills: Long, val bestLevel: Int)

/** Each player's kills by boss type and class, and the highest level among them: the bestiary. Came in 2.4. */
object PlayerBossKillsTable : Table("player_boss_kills") {
  val uuid = varchar("uuid", 36)
  val type = varchar("type", 48)
  val clazz = varchar("class", 16)
  val kills = long("kills").default(0)
  val bestLevel = integer("best_level").default(0)

  override val primaryKey = PrimaryKey(uuid, type, clazz)
}

/** Bosses by type and class: how many spawned and how many players killed. Came in 2.2. */
object BossStatsTable : Table("boss_stats") {
  val type = varchar("type", 48)
  val clazz = varchar("class", 16)
  val spawned = long("spawned").default(0)
  val killed = long("killed").default(0)

  override val primaryKey = PrimaryKey(type, clazz)
}

/** What Mo’Bosses notes about the database itself, like which formula its experience follows. Came in 2.11. */
object MetaTable : Table("mobosses_meta") {
  val key = varchar("key", 32)
  val value = varchar("value", 64)

  override val primaryKey = PrimaryKey(key)

  /** The key whose value says which formula levels follow: [LEVELS_2_11] since 2.11.0, none before. */
  const val LEVELS = "levels"
  const val LEVELS_2_11 = "2.11"
}

/** `player_progress` as it was before 2.11.0 carried levels over to its formula, kept to go back by hand. */
object PlayerProgressBefore211Table : Table("player_progress_before_2_11") {
  val uuid = varchar("uuid", 36)
  val bossKills = integer("boss_kills")
  val totalExperience = long("total_experience")
  val name = varchar("name", 16).nullable()

  override val primaryKey = PrimaryKey(uuid)
}
