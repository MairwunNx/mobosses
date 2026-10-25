package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/**
 * What abilities throw hits players only, and the pets and golems that hit the boss lately, as Paper's
 * `ArrowListener`: arrows, tridents, fireballs, and wind charges fly through everyone else, potions splash only them,
 * and a boss's fangs bite only them. Other pets, villagers, horses, other mobs, and the boss's own minions stay out of
 * it, and no minion turns on its boss. The game has no events for these, so mixins ask here: `ProjectileHitMixin`,
 * `SplashMixin`, and `FangsMixin`.
 */
class ArrowHits(private val mod: MoBosses) {
  /** Whether [projectile] may hit [entity]: anything, unless an ability threw it. */
  fun mayHit(projectile: Entity, owner: Entity?, entity: Entity) = !fromAbility(projectile) || hittable(entity, owner)

  /** Whether fangs of [owner] bite [entity]: a boss's bite only its foes, anyone else's as ever. */
  fun mayBite(owner: Entity?, entity: Entity) = owner == null || mod.bosses.get(owner) == null || hittable(entity, owner)

  fun fromAbility(entity: Entity) = Data(entity).has(Keys.ARROW)

  /** Players, and the pets and golems that hit [boss] lately. */
  private fun hittable(entity: Entity, boss: Entity?) =
    entity is ServerPlayer && Targets.canHit(entity, mod.guards) || boss != null && Targets.isAggressor(entity, boss.uuid, mod.now())
}
