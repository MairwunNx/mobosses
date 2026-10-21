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

/** The boss charges at the player it fights and tramples whoever stands in its way at the end. */
object Stampede : Signature("stampede", Message.STAMPEDE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foes(18.0).firstOrNull { it.location.distanceSquared(cast.at) > 16 } ?: return false
    val dash = target.location.toVector().subtract(cast.at.toVector()).setY(0).normalize()
    cast.boss.entity.velocity = dash.multiply(1.8).setY(0.25)
    cast.sound(Sound.ENTITY_RAVAGER_ROAR, 1f, 1f)
    cast.later(10) {
      around(cast, 2.8).forEach {
        it.damage(scaled(cast, 5.0, 0.1, 16.0), cast.boss.entity)
        knock(cast, it, 1.3, 0.6)
      }
    }
    return true
  }
}
