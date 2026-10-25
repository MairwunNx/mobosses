package ru.mairwunnx.mobosses.modcore

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.projectile.Projectile
import ru.mairwunnx.mobosses.modcore.ability.AbilityEntities

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

  /** Whether the game drops its own experience for [entity]: not for a boss, whose went out by share, nor a copy, which drops 1. */
  fun dropsExperience(entity: LivingEntity): Boolean {
    val mod = mod ?: return true
    return !mod.deaths.paid(entity) && !mod.abilityEntities.copyDied(entity)
  }

  /** Whether a projectile may hit [entity]: an ability's hits only the boss's foes. */
  fun mayHit(projectile: Projectile, entity: Entity): Boolean = mod?.arrowHits?.mayHit(projectile, projectile.owner, entity) ?: true

  /** Whom an ability's splash potion reaches: the boss's foes alone. */
  fun splashed(potion: Projectile, entities: List<LivingEntity>): List<LivingEntity> {
    val hits = mod?.arrowHits ?: return entities
    return if (hits.fromAbility(potion)) entities.filter { hits.mayHit(potion, potion.owner, it) } else entities
  }

  fun mayBite(owner: Entity?, entity: Entity): Boolean = mod?.arrowHits?.mayBite(owner, entity) ?: true

  /** True when [crystal] is an obelisk, which only players' hits wear down and which never explodes. */
  fun obeliskHurt(crystal: EndCrystal, source: DamageSource): Boolean = mod?.abilityEntities?.obeliskHurt(crystal, source) ?: false

  /**
   * Whether the game must not save [entity]: what abilities leave in the world. Asked of the entity's own data, since
   * the game saves its worlds after Mo’Bosses has stopped.
   */
  fun fleeting(entity: Entity): Boolean = AbilityEntities.fleeting(entity)

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
