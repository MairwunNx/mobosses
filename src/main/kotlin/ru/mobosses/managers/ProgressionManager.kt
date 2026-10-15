package ru.mobosses.managers

import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.algebra.innerexperience
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.BossEntity
import ru.mobosses.entities.PlayerProgress
import java.util.UUID

class ProgressionManager(private val plugin: PluginUnit, private val database: DatabaseManager) {
  private val configuration = plugin.configuration

  private val playerProgress = mutableMapOf<UUID, PlayerProgress>()

  fun getProgress(player: Player): PlayerProgress {
    val progress = playerProgress.computeIfAbsent(player.uniqueId) {
      val loadedProgress = database.getPlayerProgress(player.uniqueId)
      plugin.logger.debug { "Loaded progress for ${player.name}: ${loadedProgress.bossKills} kills, ${loadedProgress.totalExperience} exp" }
      loadedProgress
    }
    return progress
  }

  fun saveProgress(player: Player) {
    val progress = playerProgress[player.uniqueId]
    if (progress != null) {
      plugin.logger.debug { "Saving progress for ${player.name}: ${progress.bossKills} kills, ${progress.totalExperience} exp" }
      database.savePlayerProgress(progress)
    } else {
      plugin.logger.debug { "No progress to save for ${player.name}" }
    }
  }

  fun addExperience(player: Player, boss: BossEntity, damageFactor: Double): Long {
    val cfg = configuration[GeneralConfigurationModel::class.java].progression
    val gained = with(cfg.expGain) { innerexperience(boss) } * damageFactor
    val progress = getProgress(player)

    val oldExp = progress.totalExperience
    val oldKills = progress.bossKills

    progress.totalExperience += gained.toLong()
    progress.bossKills++

    plugin.logger.debug { "Added experience to ${player.name}: +${gained.toLong()} exp (${oldExp} -> ${progress.totalExperience}), kills: ${oldKills} -> ${progress.bossKills}" }

    saveProgress(player)

    return gained.toLong()
  }

  /**
   * Сохраняет прогресс всех игроков и очищает кэш при перезагрузке плагина
   */
  fun cleanup() {
    if (playerProgress.isEmpty()) {
      plugin.logger.debug { "ProgressionManager: no player progress to cleanup" }
      return
    }

    val playersCount = playerProgress.size
    plugin.logger.info { "🔄 Saving progress for $playersCount players during plugin shutdown" }

    // Сохраняем прогресс всех игроков перед очисткой
    playerProgress.values.forEach { progress ->
      database.savePlayerProgress(progress)
    }

    // Очищаем кэш прогресса
    playerProgress.clear()

    plugin.logger.info { "✅ ProgressionManager: progress saved for $playersCount players and cache cleared during plugin shutdown" }
  }
}