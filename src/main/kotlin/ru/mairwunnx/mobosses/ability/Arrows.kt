package ru.mairwunnx.mobosses.ability

import org.bukkit.Location
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.bukkit.util.Vector
import kotlin.random.Random

/** Arrows abilities shoot: the boss's, never picked up, gone after a while and never saved; [ArrowListener] lets them hit players only. */
object Arrows {
  class Tip(val effects: List<PotionEffectType>, val seconds: Int, val minAmplifier: Int, val maxAmplifier: Int)

  fun shoot(cast: Cast, at: Location, velocity: Vector, damage: Double, gravity: Boolean, lifetimeSeconds: Int, tip: Tip?) {
    val arrow = at.world.spawn(at, Arrow::class.java) { a ->
      a.shooter = cast.boss.entity
      a.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
      a.damage = damage
      a.isCritical = false
      a.isPersistent = false
      a.persistentDataContainer.set(cast.plugin.keys.arrow, PersistentDataType.BOOLEAN, true)
      a.setGravity(gravity)
      tip?.effects?.randomOrNull()?.let { type ->
        val low = tip.minAmplifier.coerceAtLeast(0)
        a.basePotionType = PotionType.WATER
        a.addCustomEffect(PotionEffect(type, tip.seconds.coerceAtLeast(1) * 20, Random.nextInt(low, tip.maxAmplifier.coerceAtLeast(low) + 1)), true)
      }
    }
    arrow.velocity = velocity
    if (lifetimeSeconds > 0) cast.plugin.server.scheduler.runTaskLater(cast.plugin, Runnable { if (arrow.isValid) arrow.remove() }, lifetimeSeconds * 20L)
  }

  /** Which of [count] arrows carry an effect: at most [max], at random; none without effects. */
  fun tipped(count: Int, max: Int, effects: List<PotionEffectType>): Set<Int> =
    if (effects.isEmpty()) emptySet() else (0 until count).shuffled().take(minOf(max, count)).toSet()
}
