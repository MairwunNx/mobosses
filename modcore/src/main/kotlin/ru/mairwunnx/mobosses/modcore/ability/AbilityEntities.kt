package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.ability.kinds.Obelisk
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/**
 * What abilities leave in the world, as on Paper: obelisks, which only players' hits break and which never explode,
 * and a boss's copies, which drop only what their mob would and 1 experience. None of it is ever saved ([fleeting]).
 */
class AbilityEntities(private val mod: MoBosses) {
  /** A hit on [crystal]: true when it's an obelisk, which the game must not hurt or blow up; a player's hit wears it down. */
  fun obeliskHurt(crystal: EndCrystal, source: DamageSource): Boolean {
    if (!Data(crystal).has(Keys.OBELISK)) return false
    val player = source.entity as? ServerPlayer ?: return true
    if (Targets.canHit(player, mod.guards)) Obelisk.hit(mod, crystal, player)
    return true
  }

  /** A copy's experience, instead of the game's: 1. True when [entity] was a copy. */
  fun copyDied(entity: LivingEntity): Boolean {
    if (!mod.decoys.isDecoy(entity)) return false
    (entity.level() as? ServerLevel)?.let { ExperienceOrb.award(it, entity.position(), 1) }
    return true
  }

  companion object {
    /** Whether the game must not save [entity]: an ability's arrow, meteor, obelisk, or copy. */
    fun fleeting(entity: Entity): Boolean {
      val data = Data(entity)
      return data.has(Keys.ARROW) || data.has(Keys.METEOR) || data.has(Keys.OBELISK) || data.has(Keys.DECOY)
    }
  }
}
