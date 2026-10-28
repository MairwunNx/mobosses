package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.config.AbilitySettings
import kotlin.math.PI
import kotlin.math.sin

/**
 * The chain as a rope the boss hauls in: it shortens with time, and a player past its length is pulled back in surges,
 * hard on the heave and slack between, so a sprint between heaves gains ground and a still player is dragged in.
 */
object Reel {
  /** How long the chain is [seconds] after it caught the player. */
  fun length(cfg: AbilitySettings.ChainSettings, seconds: Double): Double {
    val min = cfg.minLength.coerceIn(0.0, cfg.length)
    return (cfg.length - cfg.reelPerSecond.coerceAtLeast(0.0) * seconds).coerceAtLeast(min)
  }

  /** The speed it pulls at, in blocks a tick, [seconds] in: its level's base, heaving by `surge` every `surge_seconds`. */
  fun pull(cfg: AbilitySettings.ChainSettings, level: Int, seconds: Double): Double {
    val base = Rolls.linear(cfg.pullBase, cfg.pullPerLevel, cfg.pullMax, level).coerceAtLeast(0.0)
    val heave = if (cfg.surgeSeconds > 0) sin(2 * PI * seconds / cfg.surgeSeconds) else 0.0
    return base * (1.0 + cfg.surge.coerceIn(0.0, 1.0) * heave)
  }
}
