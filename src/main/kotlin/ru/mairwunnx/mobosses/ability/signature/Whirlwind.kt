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

/** The boss spins with its weapon out: every player close by takes a blow and flies back. */
object Whirlwind : Signature("whirlwind", Message.WHIRLWIND) {
  override fun cast(cast: Cast): Boolean {
    val players = around(cast, 3.5).ifEmpty { return false }
    players.forEach {
      it.damage(scaled(cast, 4.0, 0.08, 14.0), cast.boss.entity)
      knock(cast, it, 0.9, 0.35)
    }
    cast.at.world.spawnParticle(Particle.SWEEP_ATTACK, cast.at.clone().add(0.0, 1.0, 0.0), 12, 1.5, 0.3, 1.5, 0.0)
    cast.sound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f)
    return true
  }
}
