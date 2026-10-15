package ru.mobosses.managers

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel.BossClass
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.BossEntity
import java.util.UUID

class EffectsManager(private val plugin: PluginUnit) {
  private val bossAuras = mutableMapOf<UUID, BukkitRunnable>()

  private val general = plugin.configuration[GeneralConfigurationModel::class.java]

  private fun getBossClassName(bossClass: BossClass): String? {
    val classes = plugin.configuration[ru.mairwunnx.mobosses.models.ClassesConfigurationModel::class.java]
    return when (bossClass) {
      classes.common -> "common"
      classes.uncommon -> "uncommon"
      classes.rare -> "rare"
      classes.epic -> "epic"
      classes.legendary -> "legendary"
      classes.mythic -> "mythic"
      else -> null
    }
  }

  fun playSpawnEffect(entity: LivingEntity, boss: BossEntity) {
//    plugin.logger.debug { "Playing spawn effects for boss ${boss.bossType.name} lvl ${boss.level}" }
//
//    val effectsConfig = general.effects
//    val location = entity.location

//    if (effectsConfig.particles.enabled) {
////      val particleType = boss.bossType.effects.particles
////      location.world?.spawnParticle(particleType, location, 30, 0.5, 0.5, 0.5, 0.1)
////      plugin.logger.debug { "Spawned spawn particles for ${boss.bossType.name}: ${particleType}" }
//    } else if (!effectsConfig.particles.enabled) {
////      plugin.logger.debug { "Particles disabled, skipping particle effects for ${boss.bossType.name}" }
//    } else {
////      plugin.logger.debug { "No players nearby, skipping spawn particles for ${boss.bossType.name}" }
//    }

    // Запускаем ауру независимо от видимости (она сама будет проверять)
//    if (effectsConfig.particles.enabled) {
//      startAuraEffect(boss)
//    }

    // Применяем glow независимо от видимости (он сам будет проверять)
//    applyGlowEffect(entity, boss)
//
//    if (hasNearbyPlayers) {
//      entity.world.playSound(location, boss.bossType.effects.sounds.spawn, 1f, 1f)
//    }

//    plugin.logger.debug { "Spawn effects completed for boss ${boss.bossType.name}" }
  }

  fun playDeathEffect(boss: BossEntity) {
    plugin.logger.debug { "Playing death effects for boss ${boss.bossType.name} lvl ${boss.level}" }

    stopAuraEffect(boss.uuid)
    removeGlowEffect(boss.entity, boss)
    boss.entity.world.playSound(boss.entity.location, boss.bossType.effects.sounds.death, 1f, 1f)

    plugin.logger.debug { "Death effects completed for boss ${boss.bossType.name}" }
  }

  fun playAbilityEffect(location: Location, abilityName: String) {
    plugin.logger.debug { "Playing ability effect: ${abilityName} at ${location.world?.name} (${location.blockX}, ${location.blockY}, ${location.blockZ})" }

    val (sound, volume, pitch) = when (abilityName) {
      "lightning"       -> Triple(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f)
      "summon_minions"  -> Triple(Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f)
      "regeneration"    -> Triple(Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.6f)
      "fire_ring"       -> Triple(Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f)
      "web_ring"        -> Triple(Sound.BLOCK_BAMBOO_PLACE, 0.8f, 1.2f)
      "teleport_strike" -> Triple(Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f)
      "meteor_shower"   -> Triple(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.3f)
      "arrow_arc"       -> Triple(Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f)
      else              -> {
        plugin.logger.debug { "Unknown ability name: ${abilityName}, skipping sound effect" }
        return
      }
    }
    location.world?.playSound(location, sound, volume, pitch)
    plugin.logger.debug { "Played sound ${sound} for ability ${abilityName}" }
  }

  fun startAuraEffect(boss: BossEntity) {
    val effectsConfig = general.effects
    if (!effectsConfig.particles.enabled) return

    // Проверяем, не запущена ли уже аура для этого босса
    if (bossAuras.containsKey(boss.uuid)) {
      plugin.logger.debug { "Aura effect already active for boss ${boss.bossType.name}, skipping" }
      return
    }

    val particleType = boss.bossType.effects.particles

    val task = object : BukkitRunnable() {
      override fun run() {
        if (!boss.entity.isValid) {
          cancel()
          bossAuras.remove(boss.uuid) // Очищаем запись при завершении
          return
        }

        val location = boss.entity.location.add(0.0, 1.0, 0.0)
        location.world?.spawnParticle(particleType, location, 5, 0.5, 0.5, 0.5, 0.0)
      }
    }
    task.runTaskTimer(plugin, 0L, effectsConfig.particles.updateInterval.toLong())
    bossAuras[boss.uuid] = task
    plugin.logger.debug { "Started aura effect for boss ${boss.bossType.name}" }
  }

  fun applyGlowEffect(entity: LivingEntity, boss: BossEntity) {
    val glowCfg = general.effects.glow
    if (!glowCfg.enabled) {
      plugin.logger.debug { "Glow effects disabled, skipping for ${boss.bossType.name}" }
      return
    }

    // Если эффект уже применен, не применяем повторно
    if (entity.isGlowing) {
      plugin.logger.debug { "Glow effect already active for ${boss.bossType.name}, skipping" }
      return
    }

    // Проверяем, разрешен ли glow для класса данного босса
    if (glowCfg.classes.isNotEmpty()) {
      val bossClassName = getBossClassName(boss.clazz)
      if (bossClassName == null || !glowCfg.classes.contains(bossClassName)) {
        plugin.logger.debug { "Glow effect not allowed for boss class ${boss.clazz.displayName}" }
        return // Glow не разрешен для этого класса
      }
    }

    plugin.logger.debug { "Applying glow effect to ${boss.bossType.name} with color ${boss.clazz.color}" }

    // 1️⃣  Цвет берём напрямую из clazz.color
    val color: ChatColor = boss.clazz.color

    // 2️⃣  Команда на главном табло
    val scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
    val teamName = "mobosses_${boss.clazz.displayName.lowercase()}"
    val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)

    // 3️⃣  Обновляем цвет, если команда уже существовала
    if (team.color != color) team.color = color

    // 4️⃣  Добавляем сущность по UUID‑строке (до 40 симв.), только если еще не добавлена
    val entityEntry = entity.uniqueId.toString()
    if (!team.hasEntry(entityEntry)) {
      team.addEntry(entityEntry)
    }

    entity.isGlowing = true
    plugin.logger.debug { "Glow effect applied to ${boss.bossType.name} with team ${teamName}" }
  }

  fun removeGlowEffect(entity: LivingEntity, boss: BossEntity) {
    plugin.logger.debug { "Removing glow effect from ${boss.bossType.name}" }

    val teamName = "mobosses_${boss.clazz.displayName.lowercase()}"
    Bukkit.getScoreboardManager()!!.mainScoreboard
      .getTeam(teamName)
      ?.removeEntry(entity.uniqueId.toString())

    entity.isGlowing = false
    plugin.logger.debug { "Glow effect removed from ${boss.bossType.name}" }
  }

  fun stopAuraEffect(uuid: UUID) {
    val task = bossAuras.remove(uuid)
    if (task != null) {
      task.cancel()
      plugin.logger.debug { "Stopped aura effect for boss ${uuid}" }
    }
  }

  /**
   * Останавливает все эффекты и очищает состояние менеджера при перезагрузке плагина
   */
  fun cleanup() {
    if (bossAuras.isEmpty()) {
      plugin.logger.debug { "EffectsManager: no active auras to cleanup" }
      return
    }

    val aurasCount = bossAuras.size
    plugin.logger.info { "🔄 Stopping ${aurasCount} active boss auras during plugin shutdown" }

    // Останавливаем все активные ауры
    bossAuras.values.forEach { task ->
      task.cancel()
    }

    // Очищаем карту аур
    bossAuras.clear()

    plugin.logger.info { "✅ EffectsManager: ${aurasCount} auras stopped and state cleaned up during plugin shutdown" }
  }
}