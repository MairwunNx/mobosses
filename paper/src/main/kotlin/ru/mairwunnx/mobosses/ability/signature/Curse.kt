package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message

/** The guardian lays the elder guardian's curse on players who can see it: mining fatigue for a long while. */
object Curse : Signature("curse", Message.CURSE) {
  override fun cast(cast: Cast): Boolean {
    val players = cast.players(cast.at, 24.0).filter { cast.boss.entity.hasLineOfSight(it) }.ifEmpty { return false }
    players.forEach {
      it.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, ticks(cast, 30.0, 0.3, 60.0), 2))
      it.showElderGuardian(true)
    }
    return true
  }
}
