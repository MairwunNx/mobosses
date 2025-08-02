package ru.mobosses.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.PlayerProgress
import java.io.Closeable
import java.io.File
import java.util.UUID

object PlayerProgressTable : Table("player_progress") {
  val uuid = varchar("uuid", 36).index()
  val bossKills = integer("boss_kills")
  val totalExperience = long("total_experience")

  override val primaryKey = PrimaryKey(uuid)
}

private fun createDataSource(cfg: GeneralConfigurationModel.DatabaseConfig, dataFolder: File): HikariDataSource {
  val hikariCfg = HikariConfig().apply {
    poolName = "MoBosses-${cfg.type.lowercase().capitalize()}"
    when (cfg.type.lowercase()) {
      "sqlite", "sql3" -> {
        val dbFile = File(dataFolder, cfg.sqlite.file).apply { parentFile.mkdirs() }
        jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        driverClassName = "org.sqlite.JDBC"
        maximumPoolSize = 1                     // SQLite: 1 поток
        addDataSourceProperty("foreign_keys", "true")
      }

      "mysql" -> {
        val m = cfg.mysql
        jdbcUrl = "jdbc:mysql://${m.host}:${m.port}/${m.database}?rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username = m.username
        password = m.password
        maximumPoolSize = 10
        leakDetectionThreshold = 30_000
      }

      "postgres", "postgresql", "pg" -> {
        val p = cfg.postgres
        val ssl = if (p.ssl) "?sslmode=require" else ""
        jdbcUrl = "jdbc:postgresql://${p.host}:${p.port}/${p.database}$ssl"
        driverClassName = "org.postgresql.Driver"
        username = p.username
        password = p.password
        maximumPoolSize = 10
        leakDetectionThreshold = 30_000
      }

      else -> error("Unsupported database type: ${cfg.type}")
    }
    isAutoCommit = true
  }
  return HikariDataSource(hikariCfg)
}


class DatabaseManager(private val plugin: PluginUnit) : Closeable {
  private val config = plugin.configuration[GeneralConfigurationModel::class.java].database
  private val dataSource = createDataSource(config, plugin.dataFolder)

  init {
    plugin.logger.info { "🔄 Initializing database connection (${config.type.uppercase()})" }
    try {
      Database.connect(dataSource)
      transaction { SchemaUtils.create(PlayerProgressTable) }
      plugin.logger.info { "✅ Database connection established and tables created" }
    } catch (e: Exception) {
      plugin.logger.warn { "Failed to initialize database: ${e.message}" }
      throw e
    }
  }

  fun getPlayerProgress(uuid: UUID): PlayerProgress = try {
    transaction {
      val result = PlayerProgressTable
        .selectAll().where { PlayerProgressTable.uuid eq uuid.toString() }
        .singleOrNull()
        ?.let { row ->
          PlayerProgress(
            playerId = uuid,
            bossKills = row[PlayerProgressTable.bossKills],
            totalExperience = row[PlayerProgressTable.totalExperience]
          )
        }
        ?: PlayerProgress(uuid, 0, 0)

      plugin.logger.debug { "Loaded progress for player $uuid: ${result.bossKills} kills, ${result.totalExperience} exp" }
      result
    }
  } catch (e: Exception) {
    plugin.logger.warn { "Failed to load progress for player $uuid: ${e.message}" }
    PlayerProgress(uuid, 0, 0)
  }

  fun savePlayerProgress(progress: PlayerProgress) = try {
    transaction {
      PlayerProgressTable.insertIgnore {
        it[uuid] = progress.playerId.toString()
        it[bossKills] = progress.bossKills
        it[totalExperience] = progress.totalExperience
      }
      PlayerProgressTable.update({ PlayerProgressTable.uuid eq progress.playerId.toString() }) {
        it[bossKills] = progress.bossKills
        it[totalExperience] = progress.totalExperience
      }
    }
    plugin.logger.debug { "Saved progress for player ${progress.playerId}: ${progress.bossKills} kills, ${progress.totalExperience} exp" }
  } catch (e: Exception) {
    plugin.logger.warn { "Failed to save progress for player ${progress.playerId}: ${e.message}" }
  }

  override fun close() {
    plugin.logger.info { "🔄 Closing database connection" }
    try {
      dataSource.close()
      plugin.logger.info { "✅ Database connection closed successfully" }
    } catch (e: Exception) {
      plugin.logger.warn { "Error closing database connection: ${e.message}" }
    }
  }
}
