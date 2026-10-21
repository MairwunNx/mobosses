package ru.mairwunnx.mobosses.ability.signature

import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.lang.Message

/** The piglin rallies its kind: it and the piglins near it get strength and speed for a while. */
object WarCry : Signature("war_cry", Message.WAR_CRY) {
  private val KIN = setOf(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE)

  override fun cast(cast: Cast): Boolean {
    if (cast.foe(16.0) == null) return false
    val duration = ticks(cast, 8.0, 0.1, 16.0)
    val allies = cast.at.world.getNearbyLivingEntities(cast.at, 12.0) { it is Mob && it.type in KIN }
    allies.forEach {
      it.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, duration, if (cast.level >= 50) 1 else 0))
      it.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 0))
      it.world.spawnParticle(Particle.ANGRY_VILLAGER, it.location.add(0.0, 2.0, 0.0), 3, 0.3, 0.2, 0.3, 0.0)
    }
    cast.sound(Sound.ENTITY_PIGLIN_ANGRY, 1f, 0.7f)
    return true
  }
}
