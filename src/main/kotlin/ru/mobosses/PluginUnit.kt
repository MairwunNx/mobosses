package ru.mobosses

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import ru.mairwunnx.mobosses.framework.BukkitAsyncDispatcher
import ru.mairwunnx.mobosses.framework.BukkitDispatcher
import ru.mairwunnx.mobosses.framework.BukkitLogger
import ru.mairwunnx.mobosses.managers.ConfigurationManager
import ru.mairwunnx.mobosses.managers.DurabilityManager
import ru.mairwunnx.mobosses.managers.RewardsManager
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.MessagesConfigurationModel
import ru.mobosses.events.PlayerEventHandler
import ru.mobosses.events.WorldEventHandler
import ru.mobosses.managers.BossBarManager
import ru.mobosses.managers.BossManager
import ru.mobosses.managers.DamageManager
import ru.mobosses.managers.DatabaseManager
import ru.mobosses.managers.EffectsManager
import ru.mobosses.managers.LootManager
import ru.mobosses.managers.MessageManager
import ru.mobosses.managers.ProgressionManager
import ru.mobosses.managers.ProximityManager

class PluginUnit : JavaPlugin(), CommandExecutor {
  lateinit var scope: CoroutineScope private set
  lateinit var logger: BukkitLogger private set

  lateinit var mainDispatcher: BukkitDispatcher private set
  lateinit var asyncDispatcher: BukkitAsyncDispatcher private set

  lateinit var configuration: ConfigurationManager private set
  val isConfigurationDefined get() = ::configuration.isInitialized

  lateinit var messages: MessageManager private set
  lateinit var rewards: RewardsManager

  lateinit var bossManager: BossManager private set
  lateinit var bossBarManager: BossBarManager private set
  lateinit var durabilityManager: DurabilityManager private set
  lateinit var lootManager: LootManager private set
  lateinit var progressionManager: ProgressionManager private set
  lateinit var effectsManager: EffectsManager private set
  lateinit var database: DatabaseManager private set
  lateinit var proximity: ProximityManager private set
  lateinit var damageManager: DamageManager private set

  override fun onEnable() {
    logger = BukkitLogger(this)

    mainDispatcher = BukkitDispatcher(this)
    asyncDispatcher = BukkitAsyncDispatcher(this)

    scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    logger.info { "🔄 Loading Mo'Bosses plugin" }

    configuration = ConfigurationManager(this)
    runBlocking { configuration.initialize() }

    if (!configuration[GeneralConfigurationModel::class.java].enabled) {
      logger.info { "⛔️ Mo'Bosses is disabled in the config. Plugin will not be enabled." }
      onDisable()
      return
    }

    messages = MessageManager(this)
    rewards = RewardsManager(this)
    database = DatabaseManager(this)
    bossBarManager = BossBarManager(this)
    durabilityManager = DurabilityManager(this)
    effectsManager = EffectsManager(this)
    lootManager = LootManager(this, durabilityManager)
    progressionManager = ProgressionManager(this, database)
    damageManager = DamageManager()
    bossManager = BossManager(this, bossBarManager, effectsManager, progressionManager, messages)
    proximity = ProximityManager(this)
    server.pluginManager.registerEvents(WorldEventHandler(this, bossManager, bossBarManager), this)
    server.pluginManager.registerEvents(PlayerEventHandler(this, bossManager, lootManager, progressionManager, effectsManager, bossBarManager, messages, rewards, damageManager), this)
    getCommand("mobosses")?.setExecutor(this)

    proximity.initialize()

    logger.info { "✅ Plugin Mo'Bosses loaded" }
  }

  override fun onDisable() {
    // Отменяем регистрацию всех слушателей событий
    org.bukkit.event.HandlerList.unregisterAll(this)

    // Очищаем состояние менеджеров перед закрытием
    if (::bossManager.isInitialized) bossManager.cleanup()
    if (::bossBarManager.isInitialized) bossBarManager.cleanup()
    if (::progressionManager.isInitialized) progressionManager.cleanup()
    if (::effectsManager.isInitialized) effectsManager.cleanup()
    if (::lootManager.isInitialized) lootManager.cleanup()
    if (::durabilityManager.isInitialized) durabilityManager.cleanup()
    if (::proximity.isInitialized) proximity.cleanup()

    // Закрываем ресурсы
    if (::scope.isInitialized) scope.cancel()
    if (::messages.isInitialized) messages.close()
    if (::database.isInitialized) database.close()

    logger.info { "✅ Plugin Mo'Bosses unloaded" }
  }

  override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
    if (args.isEmpty()) {
      sender.sendMessage("§c📋 Доступные команды:")
      sender.sendMessage("§7• §e/mobosses reload §7- перезагрузка конфигурации")
      sender.sendMessage("§7• §e/mobosses spawn <entity> <level> <class> §7- создание босса")
      sender.sendMessage("§7• §e/mobosses killall §7- убийство всех активных боссов")
      return true
    }

    when (args[0].lowercase()) {
      "reload" -> {
        if (sender.hasPermission("mobosses.reload")) {
          logger.info { "🔄 Re-Loading Mo'Bosses plugin" }

          onDisable()
          onEnable()

          messages.send(sender, configuration[MessagesConfigurationModel::class.java].general.configReloaded)
          logger.info { "✅ Plugin Mo'Bosses reloaded" }
        } else {
          messages.send(sender, configuration[MessagesConfigurationModel::class.java].general.noPermission)
        }
        return true
      }

      "spawn" -> {
        if (!sender.hasPermission("mobosses.spawn")) {
          messages.send(sender, configuration[MessagesConfigurationModel::class.java].general.noPermission)
          return true
        }

        if (sender !is org.bukkit.entity.Player) {
          sender.sendMessage("§cЭту команду может использовать только игрок!")
          return true
        }

        if (args.size != 4) {
          sender.sendMessage("§c❌ Неверные аргументы!")
          sender.sendMessage("§7Использование: §e/mobosses spawn <entity> <level> <class>")
          sender.sendMessage("§7Доступные существа: §a${bossManager.getAvailableEntityTypes().joinToString(", ")}")
          sender.sendMessage("§7Доступные классы: §a${bossManager.getAvailableBossClasses().joinToString(", ")}")
          return true
        }

        val entityTypeStr = args[1].uppercase()
        val levelStr = args[2]
        val classStr = args[3].lowercase()

        // Проверяем тип существа
        if (!bossManager.getAvailableEntityTypes().contains(entityTypeStr)) {
          sender.sendMessage("§c❌ Неизвестный тип существа: §e$entityTypeStr")
          sender.sendMessage("§7Доступные типы: §a${bossManager.getAvailableEntityTypes().joinToString(", ")}")
          return true
        }

        // Проверяем уровень
        val level = levelStr.toIntOrNull()
        if (level == null || level < 1 || level > 100) {
          sender.sendMessage("§c❌ Неверный уровень: §e$levelStr")
          sender.sendMessage("§7Уровень должен быть числом от 1 до 100")
          return true
        }

        // Проверяем класс босса
        val bossClass = bossManager.getBossClassByName(classStr)
        if (bossClass == null) {
          sender.sendMessage("§c❌ Неизвестный класс босса: §e$classStr")
          sender.sendMessage("§7Доступные классы: §a${bossManager.getAvailableBossClasses().joinToString(", ")}")
          return true
        }

        // Пытаемся заспавнить босса
        try {
          val entityType = org.bukkit.entity.EntityType.valueOf(entityTypeStr)
          val success = bossManager.spawnBoss(sender, entityType, level, bossClass)

          if (success) {
            sender.sendMessage("§a✅ Босс ${bossClass.color}[Lv. $level] [${bossClass.displayName}] §aуспешно создан!")
          } else {
            sender.sendMessage("§c❌ Не удалось создать босса. Проверьте настройки конфигурации для типа §e$entityTypeStr")
          }
        } catch (e: IllegalArgumentException) {
          sender.sendMessage("§c❌ Неизвестный тип существа: §e$entityTypeStr")
        }

        return true
      }

      "killall" -> {
        if (sender.hasPermission("mobosses.killall")) {
          val killedCount = bossManager.killAllBosses()

          if (killedCount > 0) {
            val successMessage = configuration[MessagesConfigurationModel::class.java].general.killallSuccess
              .replace("{count}", killedCount.toString())
            messages.send(sender, successMessage)
          } else {
            messages.send(sender, configuration[MessagesConfigurationModel::class.java].general.killallNoBosses)
          }
        } else {
          messages.send(sender, configuration[MessagesConfigurationModel::class.java].general.noPermission)
        }
        return true
      }

      else -> {
        sender.sendMessage("§c❌ Неизвестная команда: §e${args[0]}")
        sender.sendMessage("§7Используйте §e/mobosses §7для списка команд")
        return true
      }
    }
  }
}
