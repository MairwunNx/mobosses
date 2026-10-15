package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The phantom brings the night down: darkness and a moment of blindness. */
object Terror : Signature("terror", Message.TERROR) {
  override fun cast(cast: Cast): Boolean {
    if (!afflict(cast, 12.0, PotionEffect(PotionEffectType.DARKNESS, ticks(cast, 5.0, 0.05, 10.0), 0), PotionEffect(PotionEffectType.BLINDNESS, 30, 0))) return false
    cast.sound(Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.6f)
    return true
  }
}
