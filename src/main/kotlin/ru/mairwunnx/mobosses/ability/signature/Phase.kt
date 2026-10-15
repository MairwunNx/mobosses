package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The vex fades from sight for a few seconds and speeds up. */
object Phase : Signature("phase", Message.PHASE) {
  override fun cast(cast: Cast): Boolean {
    val duration = ticks(cast, 3.0, 0.03, 6.0)
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false))
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 1, false, false))
    cast.at.world.spawnParticle(Particle.WITCH, cast.at.clone().add(0.0, 0.5, 0.0), 30, 0.4, 0.4, 0.4, 0.0)
    cast.sound(Sound.ENTITY_VEX_CHARGE, 1f, 1.5f)
    return true
  }
}
