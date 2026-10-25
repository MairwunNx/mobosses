package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The wither skeleton gives off a withering cloud. */
object WitherAura : Signature("wither_aura", Message.WITHER_AURA) {
  override fun cast(cast: Cast): Boolean {
    if (!afflict(cast, 6.0, PotionEffect(PotionEffectType.WITHER, ticks(cast, 4.0, 0.05, 9.0), if (cast.level >= 60) 1 else 0))) return false
    cast.at.world.spawnParticle(Particle.LARGE_SMOKE, cast.at.clone().add(0.0, 1.0, 0.0), 60, 3.0, 0.8, 3.0, 0.01)
    cast.sound(Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.4f)
    return true
  }
}
