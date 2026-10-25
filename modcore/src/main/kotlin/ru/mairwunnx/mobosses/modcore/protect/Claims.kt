package ru.mairwunnx.mobosses.modcore.protect

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.modcore.Loader

/**
 * The mods' claims, as GriefPrevention's on Paper: a claim is a player's land. No boss spawns in one and no ability
 * changes its blocks, unless `protection.claims` says otherwise; bosses may still walk in and fight, as vanilla mobs
 * do. Flan and Open Parties and Claims on either loader, and any mod behind Patbox's Common Protection API, like GOML.
 * Each mod's classes are touched only when it's loaded.
 */
class Claims private constructor(private val settings: Settings.Claims, private val log: Log, private val flan: Boolean, private val parties: Boolean, private val common: Boolean) : Guard {
  override val name = listOfNotNull("Flan".takeIf { flan }, "Open Parties and Claims".takeIf { parties }, "Common Protection API".takeIf { common }).joinToString()

  private var warned = false

  override fun canSpawnBoss(level: ServerLevel, pos: BlockPos) = !settings.noBossesInClaims || !claimed(level, pos)

  override fun canChangeBlock(level: ServerLevel, pos: BlockPos) = !settings.noBlockAbilitiesInClaims || !claimed(level, pos)

  /** Whether any claims mod has [pos] in a claim. A mod whose API moved counts as a claim: closed is the safe side. */
  fun claimed(level: ServerLevel, pos: BlockPos): Boolean = try {
    (common && Common.claimed(level, pos)) || (flan && Flan.claimed(level, pos)) || (parties && Parties.claimed(level, pos))
  } catch (error: Throwable) {
    if (!warned) {
      warned = true
      log.warn("Couldn't ask the claims mod about a claim, so Mo’Bosses treats the land as claimed: $error")
    }
    true
  }

  private object Flan {
    /** Outside any claim Flan answers with its global one. */
    fun claimed(level: ServerLevel, pos: BlockPos) =
      io.github.flemmli97.flan.api.ClaimHandler.getPermissionStorage(level).getForPermissionCheck(pos) is io.github.flemmli97.flan.claim.Claim
  }

  private object Common {
    fun claimed(level: ServerLevel, pos: BlockPos) = eu.pb4.common.protection.api.CommonProtection.isProtected(level, pos)
  }

  private object Parties {
    fun claimed(level: ServerLevel, pos: BlockPos) =
      xaero.pac.common.server.api.OpenPACServerAPI.get(level.server).serverClaimsManager.get(level.dimension().identifier(), pos) != null
  }

  companion object {
    /** The claims guard when `protection.claims` is on and a claims mod runs; null otherwise. */
    fun load(settings: Settings.Claims, loader: Loader, log: Log): Claims? {
      if (!settings.enabled) return null
      val claims = Claims(settings, log, loader.loaded("flan"), loader.loaded("openpartiesandclaims"), loader.loaded("common-protection-api"))
      return claims.takeIf { it.name.isNotEmpty() }
    }
  }
}
