package ru.mairwunnx.mobosses.spawn

import ru.mairwunnx.mobosses.config.Settings

/** The numbers of the survival bonus and the full moon, the same on every platform. */
object Hunt {
  /** The survival bonus after [minutes]: half of `max_bonus` at `half_at_minutes`, nearing all of it later. */
  fun bonus(cfg: Settings.SurvivalBonus, minutes: Int): Double {
    val m = minutes.coerceAtLeast(0).toDouble()
    return cfg.maxBonus.coerceAtLeast(0.0) * m / (cfg.halfAtMinutes.coerceAtLeast(1) + m)
  }

  /** What the bonus multiplies spawn chances by: 1 without it. */
  fun multiplier(cfg: Settings.SurvivalBonus, minutes: Int) = if (cfg.enabled) 1.0 + bonus(cfg, minutes) else 1.0

  /** A class's weight under a full moon: Common's stays, the rarest's grows by `rarer_classes`, the ones between by their share. */
  fun moonWeight(cfg: Settings.FullMoon, chance: Double, rank: Int, classes: Int): Double {
    val top = (classes - 1).coerceAtLeast(1)
    return chance * (1.0 + (cfg.rarerClasses.coerceAtLeast(1.0) - 1.0) * rank.coerceAtLeast(0) / top)
  }
}
