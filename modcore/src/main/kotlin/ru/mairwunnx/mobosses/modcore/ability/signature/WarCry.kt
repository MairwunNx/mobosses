package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.Mob
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** A piglin rallies its kind: it and the piglins near it get strength and speed for a while, and zombified ones turn on its foe. */
object WarCry : Signature("war_cry", Message.WAR_CRY) {
  private val KIN = setOf(EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.ZOMBIFIED_PIGLIN)

  override fun cast(cast: Cast): Boolean {
    val foe = cast.foe(16.0) ?: return false
    val duration = ticks(cast, 8.0, 0.1, 16.0)
    // A box of 12 blocks around, as Bukkit's `getNearbyLivingEntities` takes it.
    val allies = cast.world.getEntitiesOfClass(Mob::class.java, cast.entity.boundingBox.inflate(12.0)) { it.type in KIN }
    allies.forEach {
      it.addEffect(MobEffectInstance(MobEffects.STRENGTH, duration, if (cast.level >= 50) 1 else 0))
      it.addEffect(MobEffectInstance(MobEffects.SPEED, duration, 0))
      cast.particles(ParticleTypes.ANGRY_VILLAGER, it.position().add(0.0, 2.0, 0.0), 3, 0.3, 0.2, 0.3)
      // Zombified piglins keep to themselves until someone hurts one of theirs: the cry turns them on the boss's foe,
      // as a hit would, so they call the rest of their crowd too.
      if (it.type == EntityTypes.ZOMBIFIED_PIGLIN && it != cast.entity) {
        it.setLastHurtByMob(foe)
        it.setTarget(foe)
      }
    }
    cast.sound(SoundEvents.PIGLIN_ANGRY, 1f, 0.7f)
    return true
  }
}
