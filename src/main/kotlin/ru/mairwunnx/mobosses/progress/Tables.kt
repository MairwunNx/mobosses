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
