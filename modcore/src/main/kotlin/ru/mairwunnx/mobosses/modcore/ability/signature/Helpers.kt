package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.random.Random

/** Where the boss aims at [target]: its chest, with a little spread. */
internal fun aim(cast: Cast, target: ServerPlayer, spread: Double): Vec3 {
  val from = cast.entity.eyePosition
  val to = target.position().add(0.0, 1.1, 0.0)
  return to.subtract(from).normalize()
    .add(Random.nextDouble(-spread, spread), Random.nextDouble(-spread / 2, spread / 2), Random.nextDouble(-spread, spread)).normalize()
}

/** Players within [radius] of the boss, measured as a sphere. */
internal fun around(cast: Cast, radius: Double): List<ServerPlayer> = cast.players(cast.at, radius).filter { it.position().distanceToSqr(cast.at) <= radius * radius }

/** Pushes [target] away from the boss, and up a little. */
internal fun knock(cast: Cast, target: Entity, strength: Double, lift: Double) {
  val away = target.position().subtract(cast.at).multiply(1.0, 0.0, 1.0)
  cast.push(target, (if (away.lengthSqr() < 1e-4) Vec3(1.0, 0.0, 0.0) else away.normalize()).scale(strength).add(0.0, lift, 0.0))
}

/** Effects for every player around the boss; false when nobody is there. Each player gets their own copy of each. */
internal fun afflict(cast: Cast, radius: Double, vararg effects: () -> MobEffectInstance): Boolean {
  val players = around(cast, radius).ifEmpty { return false }
  players.forEach { player -> effects.forEach { player.addEffect(it(), cast.entity) } }
  return true
}

/** Hurts [target] as the boss's blow would, as Bukkit's `damage(amount, boss)`. */
internal fun strike(cast: Cast, target: LivingEntity, amount: Double) {
  target.hurtServer(cast.world, cast.world.damageSources().mobAttack(cast.entity), amount.toFloat())
}

/**
 * Throws [projectile] from the boss's eyes along [velocity], marked as an ability's so it hits players only, as
 * Bukkit's `launchProjectile`; gone after [lifetimeTicks] when that's above zero.
 */
internal fun launch(cast: Cast, projectile: Projectile, velocity: Vec3, lifetimeTicks: Long = 0) {
  val eye = cast.entity.eyePosition
  projectile.setOwner(cast.entity)
  projectile.setPos(eye.x, eye.y - 0.1, eye.z)
  projectile.deltaMovement = velocity
  Data(projectile)[Keys.ARROW] = true
  cast.world.addFreshEntity(projectile)
  if (lifetimeTicks > 0) cast.mod.scheduler.later(lifetimeTicks) { if (projectile.isAlive) projectile.discard() }
}

