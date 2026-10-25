package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The guardian lays the elder guardian's curse on players who can see it: mining fatigue for a long while. */
object Curse : Signature("curse", Message.CURSE) {
  override fun cast(cast: Cast): Boolean {
    val players = cast.players(cast.at, 24.0).filter { cast.entity.hasLineOfSight(it) }.ifEmpty { return false }
    players.forEach {
      it.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE, ticks(cast, 30.0, 0.3, 60.0), 2), cast.entity)
      // The elder guardian's face and sound, as it shows them when it curses.
      it.connection.send(ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1f))
    }
    return true
  }
}
