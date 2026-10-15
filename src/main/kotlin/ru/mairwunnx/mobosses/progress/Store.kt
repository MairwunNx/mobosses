package ru.mairwunnx.mobosses.progress

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import ru.mairwunnx.mobosses.config.Settings
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** A player's Mo’Bosses record: bosses they helped kill and the experience those kills brought. */
data class Progress(val kills: Int = 0, val experience: Long = 0)

/** The table 1.x made; its name, columns, and index stay, so 1.x databases keep working. */
object PlayerProgressTable : Table("player_progress") {
  val uuid = varchar("uuid", 36).index()
  val bossKills = integer("boss_kills")
  val totalExperience = long("total_experience")

  override val primaryKey = PrimaryKey(uuid)
}

/**
 * Players' progress in SQLite, MySQL or MariaDB, or PostgreSQL. Every query runs on one thread of
 * its own, in the order it came, never on the server thread.
 */
class Store(private val settings: Settings.Database, folder: File) : AutoCloseable {
  private val type = Type.of(settings.type)
  // The pool comes first: it fails fast on a database it can't reach, before the thread starts.
  private val pool = HikariDataSource(hikari(folder))
  private val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "Mo’Bosses store").apply { isDaemon = true } }
  private val database: Database

  init {
    try {
      database = Database.connect(pool, databaseConfig = DatabaseConfig { explicitDialect = mysqlDialect() })
      worker.submit { transaction(database) { SchemaUtils.create(PlayerProgressTable) } }.get()
    } catch (error: Exception) {
      worker.shutdownNow()
      pool.close()
      throw error
    }
  }

  /** Where the data lives, for the log; never the password. */
  val description: String
    get() = when (type) {
      Type.SQLITE -> "SQLite (${settings.sqlite.file})"
      Type.POSTGRES -> "PostgreSQL at ${settings.postgres.host}:${settings.postgres.port}/${settings.postgres.database}"
      Type.MYSQL -> "MySQL at ${settings.mysql.host}:${settings.mysql.port}/${settings.mysql.database}"
    }

  fun load(player: UUID): CompletableFuture<Progress> = CompletableFuture.supplyAsync({
    transaction(database) {
      PlayerProgressTable.selectAll().where { PlayerProgressTable.uuid eq player.toString() }.singleOrNull()
        ?.let { Progress(it[PlayerProgressTable.bossKills], it[PlayerProgressTable.totalExperience]) }
        ?: Progress()
    }
  }, worker)

  /**
   * Adds to a player's record and returns the new totals. It reads and writes in one transaction, so
   * a record the server hasn't loaded, or one another server shares, never gets overwritten.
   */
  fun add(player: UUID, kills: Int, experience: Long): CompletableFuture<Progress> = CompletableFuture.supplyAsync({
    transaction(database) {
      val current = PlayerProgressTable.selectAll().where { PlayerProgressTable.uuid eq player.toString() }.forUpdate().singleOrNull()
        ?.let { Progress(it[PlayerProgressTable.bossKills], it[PlayerProgressTable.totalExperience]) }
        ?: Progress()
      val next = Progress(current.kills + kills, current.experience + experience)
      PlayerProgressTable.upsert {
        it[uuid] = player.toString()
        it[bossKills] = next.kills
        it[totalExperience] = next.experience
      }
      next
    }
  }, worker)

  /** Finishes the queued queries, waiting up to ten seconds, then closes the pool. */
  override fun close() {
    worker.shutdown()
    worker.awaitTermination(10, TimeUnit.SECONDS)
    TransactionManager.closeAndUnregister(database)
    pool.close()
  }

  /** The MariaDB driver serves MySQL too, and Exposed would take any server it reaches for MariaDB, so the server says which it is. */
  private fun mysqlDialect() = if (type != Type.MYSQL) {
    null
  } else {
    val product = pool.connection.use { it.metaData.databaseProductName + " " + it.metaData.databaseProductVersion }
    if ("mariadb" in product.lowercase()) MariaDBDialect() else MysqlDialect()
  }

  private fun hikari(folder: File) = HikariConfig().apply {
    poolName = "Mo’Bosses"
    maximumPoolSize = 2
    // The class names come from class references, so they follow the drivers when the jar relocates them.
    when (type) {
      Type.SQLITE -> {
        // Paper ships this driver; it isn't in our jar.
        driverClassName = org.sqlite.JDBC::class.java.name
        jdbcUrl = "jdbc:sqlite:${folder.resolve(settings.sqlite.file).apply { parentFile.mkdirs() }.absolutePath}"
        addDataSourceProperty("busy_timeout", "10000")
      }
      Type.MYSQL -> {
        val db = settings.mysql
        driverClassName = org.mariadb.jdbc.Driver::class.java.name
        jdbcUrl = "jdbc:mariadb://${db.host}:${db.port}/${db.database}" + if (db.ssl) "?sslMode=verify-full" else ""
        username = db.username
        password = db.password
      }
      Type.POSTGRES -> {
        val db = settings.postgres
        driverClassName = org.postgresql.Driver::class.java.name
        jdbcUrl = "jdbc:postgresql://${db.host}:${db.port}/${db.database}" + if (db.ssl) "?sslmode=require" else ""
        username = db.username
        password = db.password
      }
    }
  }

  enum class Type {
    SQLITE, MYSQL, POSTGRES;

    companion object {
      /** 1.x's names: sqlite or sql3, mysql or mariadb, postgres, postgresql, or pg. */
      fun of(name: String) = when (name.trim().lowercase()) {
        "sqlite", "sql3", "sqlite3" -> SQLITE
        "mysql", "mariadb" -> MYSQL
        "postgres", "postgresql", "pg" -> POSTGRES
        else -> throw IllegalArgumentException("database.type: unknown type \"$name\"; use sqlite, mysql, or postgres")
      }
    }
  }
}
