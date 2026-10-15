package ru.mairwunnx.mobosses.protect

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Location
import ru.mairwunnx.mobosses.config.Settings

/**
 * GriefPrevention: a claim is a player's land. No boss spawns in one and no ability changes its
 * blocks, unless the config says otherwise; bosses may still walk in and fight, as vanilla mobs do.
 * This class touches GriefPrevention's classes, so it loads only when GriefPrevention runs.
 */
class GriefPreventionGuard(private val settings: Settings.GriefPrevention) : Guard {
  private val store = GriefPrevention.instance.dataStore

  override val name = "GriefPrevention"

  override fun canSpawnBoss(location: Location) = !settings.noBossesInClaims || !claimed(location)

  override fun canChangeBlock(location: Location) = !settings.noBlockAbilitiesInClaims || !claimed(location)

  override fun regionsAt(location: Location): List<String> =
    store.getClaimAt(location, false, null)?.let { listOf("claim ${it.id} of ${it.ownerName}") }.orEmpty()

  private fun claimed(location: Location) = store.getClaimAt(location, false, null) != null
}
