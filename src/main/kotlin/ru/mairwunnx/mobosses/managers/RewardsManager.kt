package ru.mairwunnx.mobosses.managers

import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.models.RewardsConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.BossEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.random.Random

class RewardsManager(private val plugin: PluginUnit) {

  private val rewardsCfg get() = plugin.configuration[RewardsConfigurationModel::class.java]
  private val rewardCooldownUntil = ConcurrentHashMap<UUID, Long>()
  private var lastCleanupTime = System.currentTimeMillis()
  private val cleanupIntervalMs = 300_000L

  fun applyRewards(killer: Player, boss: BossEntity, damageFraction: Double) {
    val cfg = rewardsCfg
    if (!cfg.enabled) {
      plugin.logger.debug { "Rewards are disabled, skipping rewards for ${killer.name}" }
      return
    }

    cleanupExpiredRecords()

    if (damageFraction <= 0.27777777) {
        plugin.logger.debug { "Player ${killer.name} will not receive rewards due to zero damage contribution." }
        return
    }

    plugin.logger.debug { "Applying rewards to ${killer.name} for killing boss ${boss.bossType.name} lvl ${boss.level}" }

    // 1) Потушим и при желании почистим негатив (всегда делаем)
    if (cfg.extinguishPlayer) killer.fireTicks = 0
    if (cfg.cleanseNegatives) cleanseNegatives(killer)

    // 2) Определяем «класс» босса и настройки для него
    val key = boss.clazz.key.lowercase()
    val tier = cfg.tiers[key] ?: cfg.tiers["common"] ?: return

    // 3) Считаем общую длительность/амплифаер с прогрессией от уровня босса и капами
    val durSec = (cfg.baseDurationSeconds + cfg.durationPerLevel * boss.level).toInt().coerceIn(1, cfg.maxDurationSeconds)
    val durationTicks = durSec * 20

    // 4) Проверяем анти-абьюз: можно ли выдать награды
    if (!canReceiveRewards(killer)) {
      plugin.logger.debug { "Anti-abuse: ${killer.name} cannot receive rewards yet (cooldown active)" }
      return
    }

    val amp = (cfg.amplifierBase + cfg.amplifierPerLevel * boss.level).toInt().coerceIn(0, cfg.amplifierMax)

    // 5) Сколько эффектов выдаём
    val effMin = min(tier.effectsMin, tier.effectsMax).coerceAtLeast(0)
    val effMax = maxOf(tier.effectsMin, tier.effectsMax).coerceAtLeast(effMin)
    val effectsCount = if (effMax == effMin) effMin else Random.nextInt(effMin, effMax + 1)
    val pool = tier.effectPool.distinct().toMutableList()
    pool.shuffle()

    // 6) Выдаём уникальные эффекты из пула
    repeat(min(effectsCount, pool.size)) {
      val type = pool[it]
      killer.addPotionEffect(PotionEffect(type, durationTicks, amp, true, true, true))
    }

    // 7) Доп. сердца через Absorption (каждые 2 сердца = +1 к amplifier)
    if (tier.extraAbsorptionHearts > 0) {
      val absAmpl = (tier.extraAbsorptionHearts / 2).coerceAtLeast(1) - 1
      killer.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, durationTicks, absAmpl, true, true, true))
    }

    // 8) Визуалочка (скромно, чтобы не спамить)
    val loc = killer.location
    loc.world?.apply {
      spawnParticle(Particle.CRIT, loc.add(0.0, 1.0, 0.0), 24, 0.5, 0.6, 0.5, 0.01)
    }

    // фиксируем кулдаун, основанный на ФАКТИЧЕСКОЙ длительности
    recordRewardCooldown(killer, durSec, cfg)

    plugin.logger.debug { "Applied rewards to ${killer.name}: ${effectsCount} effects, ${tier.extraAbsorptionHearts} absorption hearts, duration ${durSec}s" }
  }

  private fun cleanseNegatives(p: Player) {
    val negatives = setOf(
      PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER,
      PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS,
      PotionEffectType.BLINDNESS, PotionEffectType.UNLUCK, PotionEffectType.DARKNESS,
      PotionEffectType.BAD_OMEN, PotionEffectType.LEVITATION, PotionEffectType.NAUSEA
    )
    val removedCount = negatives.count { effect ->
      val hadEffect = p.hasPotionEffect(effect)
      p.removePotionEffect(effect)
      hadEffect
    }
    if (removedCount > 0) {
      plugin.logger.debug { "Cleansed $removedCount negative effects from ${p.name}" }
    }
  }

  private fun canReceiveRewards(player: Player): Boolean {
    val now = System.currentTimeMillis()
    val until = rewardCooldownUntil[player.uniqueId] ?: return true
    return now >= until
  }

  private fun recordRewardCooldown(player: Player, appliedDurationSec: Int, cfg: RewardsConfigurationModel) {
    val mult = cfg.cooldownMultiplier.coerceAtLeast(0.0)
    val minSec = cfg.cooldownMinSeconds.coerceAtLeast(0)
    val maxSec = cfg.cooldownMaxSeconds.coerceAtLeast(minSec)

    val cooldownSec = (appliedDurationSec * mult).toInt().coerceIn(minSec, maxSec)
    val until = System.currentTimeMillis() + cooldownSec * 1000L
    rewardCooldownUntil[player.uniqueId] = until

    plugin.logger.debug { "Reward cooldown for ${player.name}: ${cooldownSec}s (until=$until)" }
  }

  private fun cleanupExpiredRecords() {
    val now = System.currentTimeMillis()
    if (now - lastCleanupTime < cleanupIntervalMs) return
    lastCleanupTime = now

    val expired = rewardCooldownUntil.filterValues { it < now - 3_600_000L }.keys
    expired.forEach { rewardCooldownUntil.remove(it) }
    if (expired.isNotEmpty()) plugin.logger.debug { "Cleaned up ${expired.size} expired anti-abuse records" }
  }
}