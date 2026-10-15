package ru.mairwunnx.mobosses.combat

import java.util.UUID

/**
 * Each player's share of a boss: the damage they dealt over its max health, or over all the damage
 * dealt when a boss that healed took more than that, so the shares never add up past the whole boss.
 */
object Shares {
  fun of(damage: Map<UUID, Double>, maxHealth: Double): Map<UUID, Double> {
    val whole = maxOf(maxHealth, damage.values.sum())
    return if (whole <= 0.0) emptyMap() else damage.mapValues { (_, dealt) -> (dealt / whole).coerceIn(0.0, 1.0) }
  }
}
