package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The breeze fires wind charges at the players it fights near it: knockback, no broken blocks. */
object WindBurst : Signature("wind_burst", Message.WIND_BURST) {
  override fun cast(cast: Cast): Boolean {
    val targets = cast.foes(20.0).take(3).ifEmpty { return false }
    targets.forEach { target ->
      launch(cast, BreezeWindCharge(EntityTypes.BREEZE_WIND_CHARGE, cast.world), aim(cast, target, 0.03).scale(1.4))
    }
    cast.particles(ParticleTypes.GUST, cast.at.add(0.0, 1.0, 0.0), 3)
    cast.sound(SoundEvents.BREEZE_SHOOT)
    return true
  }
}
