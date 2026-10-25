package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.config.RewardSettings
import kotlin.random.Random

/** The numbers of `rewards.yml`, the same on every platform: how long, how strong, how many, and the wait after. */
object RewardRules {
  /** How long the effects last, in seconds, for a boss of [level]. */
  fun seconds(cfg: RewardSettings, level: Int) = (cfg.baseDurationSeconds + cfg.durationPerLevel * level).toInt().coerceIn(1, cfg.maxDurationSeconds.coerceAtLeast(1))

  fun amplifier(cfg: RewardSettings, level: Int) = (cfg.amplifierBase + cfg.amplifierPerLevel * level).toInt().coerceIn(0, cfg.amplifierMax.coerceAtLeast(0))

  /** How many effects a tier gives, rolled between its bounds. */
  fun count(tier: RewardSettings.Tier, rng: Random = Random): Int {
    val low = minOf(tier.effectsMin, tier.effectsMax).coerceAtLeast(0)
    val high = maxOf(tier.effectsMin, tier.effectsMax).coerceAtLeast(low)
    return if (high == low) low else rng.nextInt(low, high + 1)
  }

  /** The absorption amplifier for a tier's extra hearts, each level two hearts; null for none. */
  fun absorption(tier: RewardSettings.Tier): Int? = if (tier.extraAbsorptionHearts > 0) (tier.extraAbsorptionHearts / 2).coerceAtLeast(1) - 1 else null

  /** The wait, in seconds, before the next rewards after effects of [seconds]. */
  fun cooldown(cfg: RewardSettings, seconds: Int): Int {
    val min = cfg.cooldownMinSeconds.coerceAtLeast(0)
    return (seconds * cfg.cooldownMultiplier.coerceAtLeast(0.0)).toInt().coerceIn(min, cfg.cooldownMaxSeconds.coerceAtLeast(min))
  }
}
