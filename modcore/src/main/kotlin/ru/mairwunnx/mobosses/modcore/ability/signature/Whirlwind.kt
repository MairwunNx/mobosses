package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The boss spins with its weapon out: every player close by, and every pet that bit it, takes a blow and flies back. */
object Whirlwind : Signature("whirlwind", Message.WHIRLWIND) {
  override fun cast(cast: Cast): Boolean {
    val victims = cast.victims(cast.at, 3.5).filter { it.position().distanceToSqr(cast.at) <= 3.5 * 3.5 }.ifEmpty { return false }
    victims.forEach {
      strike(cast, it, scaled(cast, 4.0, 0.08, 14.0))
      knock(cast, it, 0.9, 0.35)
    }
    cast.particles(ParticleTypes.SWEEP_ATTACK, cast.at.add(0.0, 1.0, 0.0), 12, 1.5, 0.3, 1.5)
    cast.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f)
    return true
  }
}
