package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Cast
import kotlin.random.Random

/** Where the boss aims at [target]: its chest, with a little spread. */
internal fun aim(cast: Cast, target: Player, spread: Double): Vector {
  val from = cast.boss.entity.eyeLocation
  val to = target.location.add(0.0, 1.1, 0.0)
  return to.toVector().subtract(from.toVector()).normalize()
    .add(Vector(Random.nextDouble(-spread, spread), Random.nextDouble(-spread / 2, spread / 2), Random.nextDouble(-spread, spread))).normalize()
}

/** Players within [radius] of the boss, measured as a sphere. */
internal fun around(cast: Cast, radius: Double): List<Player> = cast.players(cast.at, radius).filter { it.location.distanceSquared(cast.at) <= radius * radius }

/** Pushes [player] away from the boss, and up a little. */
internal fun knock(cast: Cast, player: Player, strength: Double, lift: Double) {
  val away = player.location.toVector().subtract(cast.at.toVector()).setY(0)
  player.velocity = (if (away.lengthSquared() < 1e-4) Vector(1, 0, 0) else away.normalize()).multiply(strength).setY(lift)
}

/** Effects for every player around the boss; false when nobody is there. */
internal fun afflict(cast: Cast, radius: Double, vararg effects: PotionEffect): Boolean {
  val players = around(cast, radius).ifEmpty { return false }
  players.forEach { player -> effects.forEach(player::addPotionEffect) }
  return true
}
