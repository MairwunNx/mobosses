package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The husk raises a sandstorm: players around it can't see and get hungry. */
object Sandstorm : Signature("sandstorm", Message.SANDSTORM) {
  override fun cast(cast: Cast): Boolean {
    if (!afflict(cast, 8.0, PotionEffect(PotionEffectType.BLINDNESS, ticks(cast, 2.0, 0.02, 4.0), 0), PotionEffect(PotionEffectType.HUNGER, ticks(cast, 6.0, 0.08, 14.0), 1))) return false
    cast.at.world.spawnParticle(Particle.WHITE_ASH, cast.at.clone().add(0.0, 1.0, 0.0), 200, 4.0, 1.5, 4.0, 0.05)
    cast.sound(Sound.WEATHER_RAIN, 1f, 0.5f)
    return true
  }
}
