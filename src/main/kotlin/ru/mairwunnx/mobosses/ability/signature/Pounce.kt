package ru.mairwunnx.mobosses.ability.signature

import io.papermc.paper.entity.TeleportFlag
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
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.lang.Message

/** Spiders leap at the nearest player and bite where they land: slowness, or poison from a cave spider. */
object Pounce : Signature("pounce", Message.POUNCE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.nearest(16.0)?.takeIf { it.location.distanceSquared(cast.at) > 9 } ?: return false
    val leap = target.location.toVector().subtract(cast.at.toVector())
    cast.boss.entity.velocity = leap.clone().setY(0).normalize().multiply((leap.length() / 8).coerceIn(0.6, 1.6)).setY(0.6)
    cast.sound(Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.6f)
    cast.later(14) {
      val poison = cast.boss.entity.type == EntityType.CAVE_SPIDER
      around(cast, 2.5).forEach {
        it.damage(scaled(cast, 3.0, 0.06, 9.0), cast.boss.entity)
        it.addPotionEffect(PotionEffect(if (poison) PotionEffectType.POISON else PotionEffectType.SLOWNESS, ticks(cast, 3.0, 0.04, 7.0), if (cast.level >= 50) 1 else 0))
      }
    }
    return true
  }
}
