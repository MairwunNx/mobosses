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

/** The evoker sends a line of fangs along the ground toward the nearest player. */
object FangLine : Signature("fang_line", Message.FANG_LINE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.nearest(20.0) ?: return false
    val step = target.location.toVector().subtract(cast.at.toVector()).setY(0)
    if (step.lengthSquared() < 1e-4) return false
    step.normalize().multiply(1.25)
    val count = (8 + cast.level / 15).coerceAtMost(16)
    for (i in 1..count) {
      cast.later(i.toLong()) {
        val point = cast.at.clone().add(step.clone().multiply(i))
        val ground = Targets.ground(point.world, point.x, point.y, point.z, 4) ?: return@later
        point.world.spawn(Location(point.world, point.x, ground.y + 1.0, point.z), EvokerFangs::class.java) { it.owner = cast.boss.entity }
      }
    }
    cast.sound(Sound.ENTITY_EVOKER_PREPARE_ATTACK)
    return true
  }
}
