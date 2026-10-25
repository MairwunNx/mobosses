package ru.mairwunnx.mobosses.modcore

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

/**
 * Where the game's moments reach Mo’Bosses: the mixins call these, the same on both loaders, and each hands the
 * moment to the server that runs now. Between servers, as in a singleplayer game's main menu, nothing happens.
 * Paper's listeners have their twins here: a spawn, a chunk's entities, a hit, a death, a boat, a portal, a block.
 */
object Hooks {
  private val mod get() = Mod.current

  fun spawned(entity: Entity) {
    mod?.spawns?.spawned(entity)
  }

  fun loaded(entity: Entity) {
    mod?.spawns?.loaded(entity)
  }

  fun unloaded(entity: Entity) {
    mod?.spawns?.unloaded(entity)
  }

  /** After a hit took [dealt] health from [entity]. */
  fun hurt(entity: LivingEntity, source: DamageSource, dealt: Float) {
    val mod = mod ?: return
    if (entity is ServerPlayer) return mod.achievements.hurt(entity, source)
    val boss = mod.bosses.get(entity) ?: return
    mod.damage.hurt(boss, source, dealt)
  }

  /** The damage a hit does, before armor: a boss's own shots grow with its level. */
  fun damage(entity: LivingEntity, source: DamageSource, damage: Float): Float = mod?.damage?.shot(entity, source, damage) ?: damage

  fun died(entity: LivingEntity, source: DamageSource) {
    val mod = mod ?: return
    if (entity is ServerPlayer) {
      mod.deathPenalty.died(entity, source)
      mod.survival.died(entity)
      return
    }
    mod.bosses.get(entity)?.let { return mod.deaths.died(it, source) }
    // A boss's copy drops what its mob would and 1 experience.
    if (mod.decoys.isDecoy(entity)) mod.decoys.forget(entity)
  }

  /** Whether the game drops its own experience for [entity]: not for a boss, whose went out by share. */
  fun dropsExperience(entity: LivingEntity): Boolean = mod?.deaths?.paid(entity) != true

  fun mayRide(entity: Entity, vehicle: Entity): Boolean = mod?.antiCheese?.mayRide(entity, vehicle) ?: true

  fun mayTravel(entity: Entity): Boolean = mod?.antiCheese?.mayTravel(entity) ?: true

  /** A player breaks a block: an ability's temporary block goes without drops; true when it was one. */
  fun broke(level: ServerLevel, pos: BlockPos): Boolean {
    val temporary = mod?.blockChanges?.temporary ?: return false
    if (!temporary.contains(level, pos)) return false
    temporary.forget(level, pos)
    return true
  }
}
