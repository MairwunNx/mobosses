package ru.mobosses.managers

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.attribute.Attribute
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.BossEntity
import java.util.UUID

class BossBarManager(private val plugin: PluginUnit) {
  private val general = plugin.configuration[GeneralConfigurationModel::class.java]

  private val bossBars = mutableMapOf<UUID, BossBar>()
  private val playerBossBars = mutableMapOf<UUID, BossBar>()

  fun createBossBar(boss: BossEntity) {
    if (!general.bossBar.enabled) {
      plugin.logger.debug { "Boss bars are disabled, skipping creation for ${boss.bossType.name}" }
      return
    }
    
    plugin.logger.debug { "Creating boss bar for ${boss.bossType.name} lvl ${boss.level}" }
    
    val bossBarConfig = general.bossBar
    val bossBar = Bukkit.createBossBar(
      boss.displayName,
      BarColor.valueOf(
        when (boss.color) {
          ChatColor.BLACK -> "PURPLE"
          ChatColor.DARK_BLUE -> "BLUE"
          ChatColor.DARK_GREEN -> "GREEN"
          ChatColor.DARK_AQUA -> "BLUE"
          ChatColor.DARK_RED -> "RED"
          ChatColor.DARK_PURPLE -> "PURPLE"
          ChatColor.GOLD -> "YELLOW"
          ChatColor.GRAY -> "WHITE"
          ChatColor.DARK_GRAY -> "PURPLE"
          ChatColor.BLUE -> "BLUE"
          ChatColor.GREEN -> "GREEN"
          ChatColor.AQUA -> "BLUE"
          ChatColor.RED -> "RED"
          ChatColor.LIGHT_PURPLE -> "PINK"
          ChatColor.YELLOW -> "YELLOW"
          ChatColor.WHITE -> "WHITE"
          else -> "WHITE"
        }
      ),
      BarStyle.valueOf(bossBarConfig.style.uppercase())
    )

    val maxHealth = boss.entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
    bossBar.progress = (boss.entity.health / maxHealth).coerceIn(0.0, 1.0)
    bossBars[boss.entity.uniqueId] = bossBar
    
    plugin.logger.debug { "Boss bar created for ${boss.bossType.name} with ${bossBar.players.size} players" }
  }

  fun updateBossBarHealth(boss: BossEntity) {
    if (!general.bossBar.enabled) return
    val bossBar = bossBars[boss.entity.uniqueId]
    if (bossBar != null) {
      val maxHealth = boss.entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
      bossBar.progress = (boss.entity.health / maxHealth).coerceIn(0.0, 1.0)
    }
  }

  fun showBossBarToPlayer(player: Player, boss: BossEntity) {
    if (!general.bossBar.enabled) return
    
    plugin.logger.debug { "Showing boss bar to ${player.name} for ${boss.bossType.name}" }
    
    // Если у игрока уже есть босс-бар, скрываем его
    playerBossBars[player.uniqueId]?.removePlayer(player)

    // Получаем или создаем новый босс-бар для этого босса
    val bossBar = bossBars.getOrPut(boss.entity.uniqueId) {
      createBossBar(boss)
      bossBars[boss.entity.uniqueId]!!
    }

    // Показываем новый босс-бар игроку
    bossBar.addPlayer(player)
    playerBossBars[player.uniqueId] = bossBar
  }

  fun hideBossBarFromPlayer(player: Player) {
    if (!general.bossBar.enabled) return
    playerBossBars.remove(player.uniqueId)?.removePlayer(player)
  }

  fun removeBossBar(boss: BossEntity) {
    if (!general.bossBar.enabled) return
    
    val bossBar = bossBars.remove(boss.entity.uniqueId)
    if (bossBar != null) {
      val playersCount = bossBar.players.size
      plugin.logger.debug { "Removing boss bar for ${boss.bossType.name} from $playersCount players" }
      
      bossBar.removeAll() // Убирает босс-бар у всех игроков, которые их видят
      // Также удаляем его из playerBossBars
      val iterator = playerBossBars.entries.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (entry.value == bossBar) {
          iterator.remove()
        }
      }
      
      plugin.logger.debug { "Boss bar removed for ${boss.bossType.name}" }
    }
  }

  /**
   * Очищает все боссбары и состояние менеджера при перезагрузке плагина
   */
  fun cleanup() {
    if (bossBars.isEmpty()) {
      plugin.logger.debug { "BossBarManager: no boss bars to cleanup" }
      return
    }
    
    val barsCount = bossBars.size
    val playersCount = bossBars.values.sumOf { it.players.size }
    
    plugin.logger.info { "🔄 Cleaning up $barsCount boss bars from $playersCount players" }
    
    // Удаляем все боссбары у всех игроков
    bossBars.values.forEach { bossBar ->
      bossBar.removeAll()
    }
    
    // Очищаем все карты
    bossBars.clear()
    playerBossBars.clear()
    
    plugin.logger.info { "✅ BossBarManager: $barsCount boss bars cleaned up during plugin shutdown" }
  }
}