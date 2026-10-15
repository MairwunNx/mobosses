package ru.mairwunnx.mobosses.boss

import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID
import kotlin.random.Random

/**
 * Potion effects from `rewards.yml` for players who dealt enough of a boss's health: a few from the
 * class's pool, longer and stronger with the boss's level, plus absorption hearts. A player who just
 * got rewards waits a while before the next ones, so farming weak bosses doesn't keep them buffed.
 */
class Rewards(private val plugin: MoBossesPlugin) {
  private val cooldownUntil = HashMap<UUID, Long>()

  fun give(player: Player, boss: Boss) {
    val cfg = plugin.catalog.rewards
    if (!cfg.enabled) return
    val now = System.currentTimeMillis()
    cooldownUntil.entries.removeIf { it.value < now }
    if (cooldownUntil.containsKey(player.uniqueId)) return plugin.log.debug { "${player.name} gets no rewards yet: cooldown" }

    if (cfg.extinguishPlayer) player.fireTicks = 0
    if (cfg.cleanseNegatives) NEGATIVE.forEach(player::removePotionEffect)

    val tier = plugin.catalog.tiers[boss.clazz.key] ?: plugin.catalog.tiers["common"] ?: return
    val seconds = (cfg.baseDurationSeconds + cfg.durationPerLevel * boss.level).toInt().coerceIn(1, cfg.maxDurationSeconds.coerceAtLeast(1))
    val ticks = seconds * 20
    val amplifier = (cfg.amplifierBase + cfg.amplifierPerLevel * boss.level).toInt().coerceIn(0, cfg.amplifierMax.coerceAtLeast(0))

    val low = minOf(tier.spec.effectsMin, tier.spec.effectsMax).coerceAtLeast(0)
    val high = maxOf(tier.spec.effectsMin, tier.spec.effectsMax).coerceAtLeast(low)
    val count = if (high == low) low else Random.nextInt(low, high + 1)
    tier.effects.distinct().shuffled().take(count).forEach { player.addPotionEffect(PotionEffect(it, ticks, amplifier, true, true, true)) }
    // Each absorption level is two more hearts.
    if (tier.spec.extraAbsorptionHearts > 0) {
      player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, ticks, (tier.spec.extraAbsorptionHearts / 2).coerceAtLeast(1) - 1, true, true, true))
    }
    player.world.spawnParticle(Particle.CRIT, player.location.add(0.0, 1.0, 0.0), 24, 0.5, 0.6, 0.5, 0.01)

    val cooldown = (seconds * cfg.cooldownMultiplier.coerceAtLeast(0.0)).toInt()
      .coerceIn(cfg.cooldownMinSeconds.coerceAtLeast(0), cfg.cooldownMaxSeconds.coerceAtLeast(cfg.cooldownMinSeconds.coerceAtLeast(0)))
    cooldownUntil[player.uniqueId] = now + cooldown * 1000L
    plugin.log.debug { "${player.name} got $count effects for ${seconds}s, next rewards in ${cooldown}s" }
  }

  private companion object {
    val NEGATIVE = listOf(
      PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER, PotionEffectType.SLOWNESS,
      PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.UNLUCK,
      PotionEffectType.DARKNESS, PotionEffectType.BAD_OMEN, PotionEffectType.LEVITATION, PotionEffectType.NAUSEA,
    )
  }
}
