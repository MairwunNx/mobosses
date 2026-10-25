package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The zombie breathes rot: hunger, weakness, and a moment of nausea for players close by. */
object Infection : Signature("infection", Message.INFECTION) {
  override fun cast(cast: Cast): Boolean {
    val seconds = ticks(cast, 5.0, 0.06, 12.0)
    if (!afflict(cast, 5.0, PotionEffect(PotionEffectType.HUNGER, seconds, 1), PotionEffect(PotionEffectType.WEAKNESS, seconds, 0), PotionEffect(PotionEffectType.NAUSEA, 80, 0))) return false
    cast.at.world.spawnParticle(Particle.SNEEZE, cast.at.clone().add(0.0, 1.5, 0.0), 40, 1.5, 0.5, 1.5, 0.02)
    cast.sound(Sound.ENTITY_ZOMBIE_INFECT)
    return true
  }
}
