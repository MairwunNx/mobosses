package ru.mairwunnx.mobosses.progress

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata
import ru.mairwunnx.mobosses.config.Settings
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
      worker.submit { transaction(database) { createSchema() } }.get()
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

  fun load(player: UUID): CompletableFuture<Progress> = query {
    PlayerProgressTable.selectAll().where { PlayerProgressTable.uuid eq player.toString() }.singleOrNull()?.let(::progress) ?: Progress()
  }

  /**
   * Adds to a player's record and returns the new totals. It reads and writes in one transaction, so
   * a record the server hasn't loaded, or one another server shares, never gets overwritten.
   */
  fun add(player: UUID, name: String, kills: Int, experience: Long): CompletableFuture<Progress> = query {
    val current = PlayerProgressTable.selectAll().where { PlayerProgressTable.uuid eq player.toString() }.forUpdate().singleOrNull()?.let(::progress) ?: Progress()
    val next = Progress(current.kills + kills, current.experience + experience)
    PlayerProgressTable.upsert {
      it[uuid] = player.toString()
      it[bossKills] = next.kills
      it[totalExperience] = next.experience
      it[this.name] = name
    }
    next
  }

  /** Wipes a player's record: they start again at level 1 and leave the board. */
  fun reset(player: UUID): CompletableFuture<Unit> = query {
    PlayerProgressTable.deleteWhere { PlayerProgressTable.uuid eq player.toString() }
    Unit
  }

  /** Changes a player's record in one transaction and returns it before and after. */
  fun update(player: UUID, name: String, change: (Progress) -> Progress): CompletableFuture<Pair<Progress, Progress>> = query {
    val current = PlayerProgressTable.selectAll().where { PlayerProgressTable.uuid eq player.toString() }.forUpdate().singleOrNull()?.let(::progress) ?: Progress()
    val next = change(current)
    if (next != current) PlayerProgressTable.upsert {
      it[uuid] = player.toString()
      it[bossKills] = next.kills
      it[totalExperience] = next.experience
      it[this.name] = name
    }
    current to next
  }

  /**
   * The best hunters, by [score], what their level grows from, then by experience. The score takes a
   * square root that SQLite may lack, so the rows are sorted here; a row is a few bytes, even for
   * thousands of players.
   */
  fun top(limit: Int, score: (Progress) -> Double): CompletableFuture<List<Hunter>> = query {
    hunters().sortedWith(order(score)).take(limit)
  }

  /** A player's place on the board: 1 plus how many rank above them. */
  fun rank(player: UUID, score: (Progress) -> Double): CompletableFuture<Int> = query {
    val all = hunters()
    val own = all.firstOrNull { it.uuid == player.toString() } ?: Hunter(player.toString(), null, Progress())
    val order = order(score)
    all.count { it.uuid != own.uuid && order.compare(it, own) < 0 } + 1
  }

  private fun hunters() = PlayerProgressTable.selectAll().map { Hunter(it[PlayerProgressTable.uuid], it[PlayerProgressTable.name], progress(it)) }

  private fun order(score: (Progress) -> Double) =
    compareByDescending<Hunter> { score(it.progress) }.thenByDescending { it.progress.experience }

  /** Counts a boss that spawned, or one players killed. */
  fun count(type: String, clazz: String, killed: Boolean) {
    query {
      val key = (BossStatsTable.type eq type) and (BossStatsTable.clazz eq clazz)
      val row = BossStatsTable.selectAll().where { key }.forUpdate().singleOrNull()
      BossStatsTable.upsert {
        it[this.type] = type
        it[this.clazz] = clazz
        it[spawned] = (row?.get(spawned) ?: 0L) + if (killed) 0 else 1
        it[this.killed] = (row?.get(this.killed) ?: 0L) + if (killed) 1 else 0
      }
    }
  }

  fun counts(): CompletableFuture<List<BossCount>> = query {
    BossStatsTable.selectAll().map { BossCount(it[BossStatsTable.type], it[BossStatsTable.clazz], it[BossStatsTable.spawned], it[BossStatsTable.killed]) }
  }

  private fun progress(row: ResultRow) = Progress(row[PlayerProgressTable.bossKills], row[PlayerProgressTable.totalExperience])

  private fun <T> query(block: JdbcTransaction.() -> T): CompletableFuture<T> = CompletableFuture.supplyAsync({ transaction(database) { block() } }, worker)

  /** Creates the tables a new database lacks, and the columns an older one lacks, like 1.x's file without `name`. */
  private fun JdbcTransaction.createSchema() {
    val tables = arrayOf(PlayerProgressTable, BossStatsTable)
    SchemaUtils.create(*tables)
    currentDialectMetadata.resetCaches()
    val columns = currentDialectMetadata.tableColumns(*tables)
    tables.forEach { table ->
      val names = columns[table].orEmpty().map { it.name.lowercase() }
      table.columns.filter { it.name.lowercase() !in names }.forEach { column -> column.ddl.forEach(::exec) }
    }
  }

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
