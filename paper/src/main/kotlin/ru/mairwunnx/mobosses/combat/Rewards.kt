package ru.mairwunnx.mobosses.combat

import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.MoBossesPlugin
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.boss.Boss

/**
 * Potion effects from `rewards.yml` for players who dealt enough of a boss's health: a few from the
 * class's pool, longer and stronger with the boss's level, plus absorption hearts. A player who just
 * got rewards waits a while before the next ones, so farming weak bosses doesn't keep them buffed.
 * The wait lives on the player, so leaving and coming back doesn't end it.
 */
class Rewards(private val plugin: MoBossesPlugin) {
  /** Null when it gave them or they're off; otherwise the seconds until the player may get rewards again. */
  fun give(player: Player, boss: Boss): Long? {
    val cfg = plugin.catalog.rewards
    if (!cfg.enabled) return null
    val now = System.currentTimeMillis()
    val until = player.persistentDataContainer.get(plugin.keys.rewardsUntil, PersistentDataType.LONG) ?: 0L
    if (until > now) {
      plugin.log.debug { "${player.name} gets no rewards yet: cooldown" }
      return ((until - now) / 1000).coerceAtLeast(1)
    }

    if (cfg.extinguishPlayer) player.fireTicks = 0
    if (cfg.cleanseNegatives) NEGATIVE.forEach(player::removePotionEffect)

    val tier = plugin.catalog.tiers[boss.clazz.key] ?: plugin.catalog.tiers["common"] ?: return null
    val seconds = RewardRules.seconds(cfg, boss.level)
    val ticks = seconds * 20
    val amplifier = RewardRules.amplifier(cfg, boss.level)

    val count = RewardRules.count(tier.spec)
    tier.effects.distinct().shuffled().take(count).forEach { player.addPotionEffect(PotionEffect(it, ticks, amplifier, true, true, true)) }
    RewardRules.absorption(tier.spec)?.let { player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, ticks, it, true, true, true)) }
    player.world.spawnParticle(Particle.CRIT, player.location.add(0.0, 1.0, 0.0), 24, 0.5, 0.6, 0.5, 0.01)

    val cooldown = RewardRules.cooldown(cfg, seconds)
    player.persistentDataContainer.set(plugin.keys.rewardsUntil, PersistentDataType.LONG, now + cooldown * 1000L)
    plugin.log.debug { "${player.name} got $count effects for ${seconds}s, next rewards in ${cooldown}s" }
    return null
  }

  private companion object {
    val NEGATIVE = listOf(
      PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER, PotionEffectType.SLOWNESS,
      PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.UNLUCK,
      PotionEffectType.DARKNESS, PotionEffectType.BAD_OMEN, PotionEffectType.LEVITATION, PotionEffectType.NAUSEA,
    )
  }
}
