package ru.mairwunnx.mobosses.modcore.combat

import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.animal.golem.AbstractGolem
import net.minecraft.world.entity.projectile.Projectile
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.ability.kinds.Chain
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/**
 * Hits on bosses, as Paper's `DamageListener`: each player's share of the damage, their pets' included, kept on the
 * boss, the health bar, and the abilities a hit may set off. A player, their pet, or a golem sets abilities off; the
 * pet or golem becomes fair game for them. Only health the hit took counts, so overkill and absorbed hits give
 * nothing. Damage nobody dealt by hand goes to the players fighting the boss ([Boss.credit]). Bosses' own shots grow
 * with their level here too.
 */
class Damage(private val mod: MoBosses) {
  /** After a hit took [dealt] health from [boss]. */
  fun hurt(boss: Boss, source: DamageSource, dealt: Float) {
    mod.bars.update(boss)
    val amount = dealt.toDouble().coerceAtLeast(0.0)
    val by = source.entity
    if (by == null) {
      // Nobody dealt it by hand, fire, a fall after a knockback, lava, poison: it goes to the players fighting it, or a
      // solo player who set it alight or knocked it off a ledge would lose part of their share.
      if (amount <= 0.0) return
      boss.credit(amount)
      mod.groupHealth.update(boss)
      return
    }
    val pet = (by as? OwnableEntity)?.takeIf { it.owner is ServerPlayer }
    val player = by as? ServerPlayer ?: pet?.owner as? ServerPlayer
    if (player != null && amount > 0.0) {
      boss.addDamage(player.uuid, amount, byPet = pet != null)
      mod.groupHealth.update(boss)
    }
    if (by is ServerPlayer) aggro(boss, by, amount)
    if (by !is ServerPlayer) {
      // Only pets and golems: other mobs that hit the boss by chance don't wake its abilities.
      if (pet == null && by !is AbstractGolem) return
      Targets.markAggressor(by, boss.uuid, mod.now())
    }
    player?.takeIf { !it.hasDisconnected() && it.level() == boss.entity.level() }?.let {
      mod.bars.show(it, boss)
      mod.achievements.grant(it, Achievement.HUNT_BEGINS)
    }
    // A blow in melee loosens the chain on whoever strikes.
    if (source.directEntity is ServerPlayer) Chain.struck(mod, boss, source.directEntity as ServerPlayer)
    // The killing blow casts nothing: no minions or doubles for a boss already dead.
    if (boss.entity.isDeadOrDying) return
    mod.abilities.onHit(boss, player?.takeIf { !it.hasDisconnected() })
  }

  /** A vanilla creeper keeps its first target whoever hits it: a boss turns on whoever hurts it most lately ([Threat]). */
  private fun aggro(boss: Boss, player: ServerPlayer, dealt: Double) {
    val mob = boss.entity as? Mob ?: return
    if (mob is EnderDragon) return
    val settings = mod.catalog.settings.aggro
    val now = mod.now()
    boss.threat.add(player.uuid, dealt, now, settings.halfLifeSeconds)
    if (boss.threat.turns(settings, player.uuid, mob.target?.uuid, now) && Targets.canHit(player, mod.guards)) mob.setTarget(player)
  }

  /**
   * The damage a boss's own shot does to [target]: its arrows, fireballs, and tridents hit harder with its level.
   * Abilities' shots have their own. The dragon's blows and breath have no attribute to grow, so they grow as its shots.
   */
  fun shot(target: LivingEntity, source: DamageSource, damage: Float): Float {
    val shot = source.directEntity as? Projectile ?: return dragon(target, source, damage)
    val data = Data(shot)
    data.double(Keys.SHOT_DAMAGE)?.let { return it.toFloat() }
    val boss = (source.entity as? LivingEntity)?.let(mod.bosses::get) ?: return damage
    if (target == boss.entity || source.`is`(DamageTypeTags.IS_EXPLOSION)) return damage
    if (data.has(Keys.ARROW) || data.has(Keys.METEOR)) return damage
    val formula = mod.catalog.settings.formula
    val graded = boss.graded(mod.catalog.settings.difficulty.weights.damage)
    return damage * (Stats.ranged(formula.damage, boss.clazz.damage, boss.level, formula.rangedDamage) * graded).coerceAtLeast(1.0).toFloat()
  }

  private fun dragon(target: LivingEntity, source: DamageSource, damage: Float): Float {
    val boss = (source.entity as? EnderDragon)?.let(mod.bosses::get) ?: return damage
    if (target == boss.entity) return damage
    val formula = mod.catalog.settings.formula
    val graded = boss.graded(mod.catalog.settings.difficulty.weights.damage)
    return damage * (Stats.ranged(formula.damage, boss.clazz.damage, boss.level, formula.rangedDamage) * graded).coerceAtLeast(1.0).toFloat()
  }
}
