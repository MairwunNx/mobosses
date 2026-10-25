package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.kinds.TeleportStrike
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.lang.Message

/** The enderman trades places with the player it fights, blinding them for a moment. */
object Swap : Signature("swap", Message.SWAP) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foes(24.0).firstOrNull { it.location.distanceSquared(cast.at) > 16 } ?: return false
    val bossAt = cast.at
    val playerAt = target.location
    if (!cast.boss.entity.hasLineOfSight(target) || !TeleportStrike.fits(cast, playerAt)) return false
    if (!cast.boss.entity.teleport(playerAt)) return false
    target.teleport(bossAt.clone().setDirection(playerAt.toVector().subtract(bossAt.toVector())))
    target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 30, 0))
    listOf(bossAt, playerAt).forEach { it.world.spawnParticle(Particle.PORTAL, it, 40, 0.5, 1.0, 0.5, 0.2) }
    cast.sound(Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f)
    return true
  }
}
