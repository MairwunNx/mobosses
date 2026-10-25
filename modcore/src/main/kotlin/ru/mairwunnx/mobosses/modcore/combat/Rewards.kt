package ru.mairwunnx.mobosses.modcore.combat

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.combat.RewardRules
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/**
 * Potion effects from `rewards.yml` for players who dealt enough of a boss's health, as on Paper: a few from the
 * class's pool, longer and stronger with the boss's level, plus absorption hearts. A player who just got rewards waits
 * a while before the next ones. The wait lives on the player, so leaving and coming back doesn't end it.
 */
class Rewards(private val mod: MoBosses) {
  /** Null when it gave them or they're off; otherwise the seconds until the player may get rewards again. */
  fun give(player: ServerPlayer, boss: Boss): Long? {
    val cfg = mod.catalog.rewards
    if (!cfg.enabled) return null
    val now = mod.now()
    val data = Data(player)
    val until = data.long(Keys.REWARDS_UNTIL) ?: 0L
    if (until > now) {
      mod.log.debug { "${player.plainTextName} gets no rewards yet: cooldown" }
      return ((until - now) / 1000).coerceAtLeast(1)
    }

    if (cfg.extinguishPlayer) player.clearFire()
    if (cfg.cleanseNegatives) NEGATIVE.forEach(player::removeEffect)

    val tier = mod.catalog.tiers[boss.clazz.key] ?: mod.catalog.tiers["common"] ?: return null
    val seconds = RewardRules.seconds(cfg, boss.level)
    val ticks = seconds * 20
    val amplifier = RewardRules.amplifier(cfg, boss.level)
    val count = RewardRules.count(tier.spec)
    tier.effects.distinct().shuffled().take(count).forEach { player.addEffect(MobEffectInstance(it, ticks, amplifier, true, true, true)) }
    RewardRules.absorption(tier.spec)?.let { player.addEffect(MobEffectInstance(MobEffects.ABSORPTION, ticks, it, true, true, true)) }
    (player.level() as? ServerLevel)?.let { World.particles(it, ParticleTypes.CRIT, player.position().add(0.0, 1.0, 0.0), 24, 0.5, 0.6, 0.5, 0.01) }

    val cooldown = RewardRules.cooldown(cfg, seconds)
    data[Keys.REWARDS_UNTIL] = now + cooldown * 1000L
    mod.log.debug { "${player.plainTextName} got $count effects for ${seconds}s, next rewards in ${cooldown}s" }
    return null
  }

  private companion object {
    val NEGATIVE = listOf(
      MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS,
      MobEffects.BLINDNESS, MobEffects.UNLUCK, MobEffects.DARKNESS, MobEffects.BAD_OMEN, MobEffects.LEVITATION, MobEffects.NAUSEA,
    )
  }
}
