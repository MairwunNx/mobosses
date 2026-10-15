package ru.mobosses.events

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.algebra.experience
import ru.mairwunnx.mobosses.managers.RewardsManager
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.MessagesConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.managers.BossBarManager
import ru.mobosses.managers.BossManager
import ru.mobosses.managers.DamageManager
import ru.mobosses.managers.EffectsManager
import ru.mobosses.managers.LootManager
import ru.mobosses.managers.MessageManager
import ru.mobosses.managers.ProgressionManager
import java.util.UUID
import org.bukkit.attribute.Attribute

class PlayerEventHandler(
  private val plugin: PluginUnit,
  private val bossManager: BossManager,
  private val lootManager: LootManager,
  private val progressionManager: ProgressionManager,
  private val effectsManager: EffectsManager,
  private val bossBarManager: BossBarManager,
  private val messageManager: MessageManager,
  private val rewards: RewardsManager,
  private val damageManager: DamageManager
) : Listener {

  private val general = plugin.configuration[GeneralConfigurationModel::class.java]
  private val messagesSettings = plugin.configuration[MessagesConfigurationModel::class.java]
  private val classes = plugin.configuration[ClassesConfigurationModel::class.java]

  private val lastDamager = mutableMapOf<UUID, UUID>()

  /**
   * Вычисляет громкость звука в зависимости от класса босса
   * От 0.2 (Common) до 1.0 (Mythic)
   */
  private fun calculateSoundVolume(bossClass: ClassesConfigurationModel.BossClass): Float {
    return when (bossClass) {
      classes.common -> 0.2f
      classes.uncommon -> 0.35f
      classes.rare -> 0.5f
      classes.epic -> 0.65f
      classes.legendary -> 0.8f
      classes.mythic -> 1.0f
      else -> 0.2f // fallback
    }
  }

  @EventHandler fun onPlayerJoin(event: PlayerJoinEvent) {
    plugin.logger.debug { "onPlayerJoin event has come $event" }
    progressionManager.getProgress(event.player)
  }

  @EventHandler fun onPlayerQuit(event: PlayerQuitEvent) {
    plugin.logger.debug { "onPlayerQuit event has come $event" }
    progressionManager.saveProgress(event.player)
  }

  @EventHandler fun onEntityDeath(event: EntityDeathEvent) {
    val entity = event.entity
    if (bossManager.isBoss(entity)) {
      val boss = bossManager.getBoss(entity) ?: return
      val damagers = damageManager.getDamageContributors(entity)
      val totalDamage = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 0.0

      if (damagers != null && totalDamage > 0) {
        // Получаем опыт за убийство босса
        val originalExp = event.droppedExp
        val preset = general.formula.experience
        val totalBossExp = with(preset) { experience(boss, originalExp) }
        event.droppedExp = 0

        for ((playerId, playerDamage) in damagers) {
          val player = plugin.server.getPlayer(playerId)
          if (player != null) {
            val damageFraction = playerDamage / totalDamage
            val experience = (totalBossExp * damageFraction).toInt()

            plugin.logger.info { "🔄 Processing boss kill: ${boss.bossType.name} lvl ${boss.level} damaged by ${player.name}" }

            progressionManager.addExperience(player, boss, damageFraction)
            player.giveExp(experience)

            // Выдача наград и лута
            lootManager.dropLoot(boss, player, damageFraction)
            rewards.applyRewards(player, boss, damageFraction)

            plugin.logger.info { "✅ Boss kill processed: ${player.name} received $experience exp for ${boss.bossType.name} lvl ${boss.level}" }

            val killedMessages = messagesSettings.boss.death.killed
            if (killedMessages.enabled) {
              val adaptiveVolume = calculateSoundVolume(boss.clazz)
              player.playSound(player.location, killedMessages.sound, adaptiveVolume, 1.0f)
            }
          }
        }

        // Общее сообщение о смерти босса
        val killedMessages = messagesSettings.boss.death.killed
        if (killedMessages.enabled && killedMessages.broadcast.enabled) {
          val killerNames = damagers.keys.mapNotNull { plugin.server.getPlayer(it)?.name }.joinToString(", ")
          val stringArgs = mapOf(
            "player" to killerNames,
            "boss_name" to boss.bossType.name,
            "level" to boss.level.toString(),
            "experience" to totalBossExp.toString()
          )

          val named = NamedTextColor.NAMES.value(boss.clazz.color.name.lowercase()) ?: NamedTextColor.WHITE
          val classComponent = Component.text(boss.clazz.displayName).color(named)

          messageManager.broadcast(
            raw = killedMessages.broadcast.message,
            stringArgs = stringArgs,
            componentArgs = mapOf("class" to classComponent)
          )
        }

      } else {
        plugin.logger.debug { "Boss ${boss.bossType.name} lvl ${boss.level} died without player damage" }
      }

      bossManager.removeBoss(entity)
      effectsManager.playDeathEffect(boss)
      damageManager.clearDamage(entity)
    }
  }

  @EventHandler fun onEntityDamage(event: EntityDamageEvent) {
    val bossEntity = event.entity
    if (!bossManager.isBoss(bossEntity as? org.bukkit.entity.LivingEntity ?: return)) return

    val boss = bossManager.getBoss(bossEntity) ?: return

    plugin.server.scheduler.runTaskLater(plugin, Runnable {
      bossBarManager.updateBossBarHealth(boss)
    }, 1L)

    if (event is EntityDamageByEntityEvent) {
      if (event.damager == bossEntity) return
      var damager = event.damager
      if (damager is org.bukkit.entity.Projectile && damager.shooter is Player) {
        damager = damager.shooter as Player
      }

      if (damager is Player) {
        damageManager.addDamage(bossEntity as org.bukkit.entity.LivingEntity, damager, event.finalDamage)
        bossBarManager.showBossBarToPlayer(damager, boss)
        bossManager.abilitiesManager.useRandomAbility(boss, damager)
        plugin.logger.debug { "Player ${damager.name} damaged for ${event.finalDamage} boss ${boss.bossType.name} lvl ${boss.level}" }
      }
    }
  }
}