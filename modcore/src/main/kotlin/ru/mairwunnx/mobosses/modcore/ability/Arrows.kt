package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.random.Random

/**
 * Arrows abilities shoot, as on Paper: the boss's, never picked up, gone after a while and never saved
 * (`TransientMixin`); [ArrowHits] lets them hit players only.
 */
object Arrows {
  class Tip(val effects: List<Holder<MobEffect>>, val seconds: Int, val minAmplifier: Int, val maxAmplifier: Int)

  fun shoot(cast: Cast, at: Vec3, velocity: Vec3, damage: Double, gravity: Boolean, lifetimeSeconds: Int, tip: Tip?) {
    val arrow = Arrow(cast.world, at.x, at.y, at.z, ItemStack(Items.ARROW), null)
    arrow.setOwner(cast.entity)
    arrow.pickup = AbstractArrow.Pickup.DISALLOWED
    arrow.setBaseDamage(damage)
    arrow.setCritArrow(false)
    arrow.isNoGravity = !gravity
    Data(arrow)[Keys.ARROW] = true
    tip?.effects?.randomOrNull()?.let { type ->
      val low = tip.minAmplifier.coerceAtLeast(0)
      arrow.addEffect(MobEffectInstance(type, tip.seconds.coerceAtLeast(1) * 20, Random.nextInt(low, tip.maxAmplifier.coerceAtLeast(low) + 1)))
    }
    // Shot, not just moved: the arrow points where it flies from its first tick.
    arrow.shoot(velocity.x, velocity.y, velocity.z, velocity.length().toFloat(), 0f)
    cast.world.addFreshEntity(arrow)
    if (lifetimeSeconds > 0) cast.mod.scheduler.later(lifetimeSeconds * 20L) { if (arrow.isAlive) arrow.discard() }
  }

  /** Which of [count] arrows carry an effect: at most [max], at random; none without effects. */
  fun tipped(count: Int, max: Int, effects: List<Holder<MobEffect>>): Set<Int> =
    if (effects.isEmpty()) emptySet() else (0 until count).shuffled().take(minOf(max, count)).toSet()
}
