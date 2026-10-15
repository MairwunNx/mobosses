package ru.mairwunnx.mobosses.combat

import java.util.UUID

/** Each player's share of a boss: the damage they dealt over its max health, 0 to 1. */
object Shares {
  fun of(damage: Map<UUID, Double>, maxHealth: Double): Map<UUID, Double> =
    if (maxHealth <= 0.0) emptyMap() else damage.mapValues { (_, dealt) -> (dealt / maxHealth).coerceIn(0.0, 1.0) }
}
