package ru.mairwunnx.mobosses.ability.kinds

import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Spider
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message

/** The boss steps out of thin air two blocks behind the nearest player, never inside a wall. Spiders don't: they climb instead. */
object TeleportStrike : Ability {
  override val key = "teleport_strike"
  override val message = Message.TELEPORT_STRIKE
  override fun enabled(settings: AbilitySettings) = settings.teleportStrikeEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.teleportStrike
  override fun cooldown(settings: AbilitySettings) = settings.teleportStrikeCooldown to settings.teleportStrikeCooldownBias

  // CaveSpider extends Spider.
  override fun usable(cast: Cast) = cast.boss.entity !is Spider

  override fun cast(cast: Cast): Boolean {
    val target = cast.nearest(15.0) ?: return false
    val behind = target.location.clone().subtract(target.location.direction.setY(0).normalize().multiply(2.0)).add(0.0, 0.1, 0.0)
    val to = (-2..3).map { behind.clone().add(0.0, it.toDouble(), 0.0) }
      .firstOrNull { !it.block.type.isSolid && !it.clone().add(0.0, 1.0, 0.0).block.type.isSolid } ?: return false
    val from = cast.at
    from.world.spawnParticle(Particle.PORTAL, from, 30, 0.5, 1.0, 0.5, 0.1)
    cast.sound(Sound.ENTITY_ENDERMAN_TELEPORT)
    to.direction = target.location.toVector().subtract(to.toVector())
    // Its name labels ride along.
    if (!cast.boss.entity.teleport(to, TeleportFlag.EntityState.RETAIN_PASSENGERS)) return false
    cast.sound(Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f, to)
    return true
  }
}
