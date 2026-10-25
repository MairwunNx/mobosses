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

  /**
   * The share of a player's damage that counts for experience, when [pets] of their [dealt] came
   * from their pets, each point of it worth [weight].
   */
  fun petWorth(dealt: Double, pets: Double, weight: Double): Double =
    if (dealt <= 0.0) 1.0 else ((dealt - pets.coerceIn(0.0, dealt)) + pets.coerceIn(0.0, dealt) * weight.coerceIn(0.0, 1.0)) / dealt

  /** A player's part of damage nobody dealt by hand: [own] theirs, [pets] their pets'. */
  class Credit(val own: Double, val pets: Double)

  /**
   * Damage with no player behind it, a fall after a knockback, lava, a cactus, poison, goes to the players who hit the
   * boss lately ([recent]), split by what each has dealt so far, their pets' part in step: a player who fights alone
   * gets all of it, a group splits it as they fought. Nobody gets it when nobody hit the boss lately.
   */
  fun credit(amount: Double, damage: Map<UUID, Double>, petDamage: Map<UUID, Double>, recent: Set<UUID>): Map<UUID, Credit> {
    if (amount <= 0.0) return emptyMap()
    val dealt = recent.associateWith { (damage[it] ?: 0.0).coerceAtLeast(0.0) }
    val total = dealt.values.sum()
    // Players who just landed their first hit, with nothing dealt yet, share it evenly.
    val weights = if (total > 0.0) dealt.mapValues { it.value / total } else recent.associateWith { 1.0 / recent.size }
    return weights.filterValues { it > 0.0 }.mapValues { (id, weight) ->
      val part = amount * weight
      val petShare = dealt[id]?.takeIf { it > 0.0 }?.let { ((petDamage[id] ?: 0.0) / it).coerceIn(0.0, 1.0) } ?: 0.0
      Credit(part * (1.0 - petShare), part * petShare)
    }
  }
}
