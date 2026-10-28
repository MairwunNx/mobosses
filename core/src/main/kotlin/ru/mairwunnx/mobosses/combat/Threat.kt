package ru.mairwunnx.mobosses.combat

import ru.mairwunnx.mobosses.config.Aggro
import java.util.UUID
import kotlin.math.pow

/**
 * Whom a boss turns on. Each player's damage to it counts as threat, fading by half every `half_life_seconds`; a hit
 * from a player whose threat beats that of the boss's target by `margin` draws the boss to them. A vanilla creeper
 * keeps its first target whoever hits it; a boss answers whoever hurts it most. It lives only in memory: a restart
 * starts it over.
 */
class Threat {
  private val values = HashMap<UUID, Pair<Double, Long>>()

  fun add(player: UUID, amount: Double, now: Long, halfLife: Double) {
    values[player] = (of(player, now, halfLife) + amount.coerceAtLeast(0.0)) to now
  }

  fun of(player: UUID?, now: Long, halfLife: Double): Double {
    val (value, at) = values[player ?: return 0.0] ?: return 0.0
    return value * 0.5.pow((now - at).coerceAtLeast(0) / 1000.0 / halfLife.coerceAtLeast(0.1))
  }

  /** Whether a hit from [attacker] turns the boss from [target], whom it chases now, to them. */
  fun turns(settings: Aggro, attacker: UUID, target: UUID?, now: Long): Boolean {
    if (!settings.enabled || attacker == target) return false
    return target == null || of(attacker, now, settings.halfLifeSeconds) > of(target, now, settings.halfLifeSeconds) * settings.margin.coerceAtLeast(1.0)
  }
}
