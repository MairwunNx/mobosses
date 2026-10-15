package ru.mairwunnx.mobosses.protect

import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * What a protection plugin lets bosses do. One adapter per plugin, each loaded only when that
 * plugin runs; Lands, Towny, or Residence would join [Guards.load] as another adapter. With none,
 * everything is allowed.
 */
interface Guard {
  val name: String

  /** Whether a boss may spawn here naturally. `/mobosses spawn` asks only when `protection.admin_spawn_bypass` is off. */
  fun canSpawnBoss(location: Location): Boolean = true

  /** Whether an ability may break, place, or ignite the block here. */
  fun canChangeBlock(location: Location): Boolean = true

  /** Whether an ability may hurt this player here. */
  fun canHurt(player: Player): Boolean = true

  /** The protected areas here, for the debug log. */
  fun regionsAt(location: Location): List<String> = emptyList()
}
