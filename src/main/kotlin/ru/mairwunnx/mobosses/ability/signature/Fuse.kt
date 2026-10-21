package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The creeper's fuse catches early: it gets fast and hard to stop for a few seconds. */
object Fuse : Signature("fuse", Message.FUSE) {
  override fun cast(cast: Cast): Boolean {
    if (cast.foe(12.0) == null) return false
    val duration = ticks(cast, 3.0, 0.03, 6.0)
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 1))
    cast.boss.entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, duration, 1))
    cast.at.world.spawnParticle(Particle.ELECTRIC_SPARK, cast.at.clone().add(0.0, 1.0, 0.0), 30, 0.4, 0.6, 0.4, 0.1)
    cast.sound(Sound.ENTITY_CREEPER_PRIMED, 1f, 0.8f)
    return true
  }
}
