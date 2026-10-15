package ru.mairwunnx.mobosses.ability.kinds

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * Against archers: the boss chains the farthest player in sight who keeps away from it. For a few
 * seconds that player can't stray past `chain_length`: the chain yanks them back and hurts. A few
 * blows to the boss in melee break it early, so the way out is to close in and fight. A chain lasts
 * seconds, so it lives only in memory; a restart just ends it.
 */
object Chain : Ability {
  override val key = "chain"
  override val message = Message.CHAIN
  override fun enabled(settings: AbilitySettings) = settings.chain.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.chain
  override fun cooldown(settings: AbilitySettings) = settings.chain.cooldown to settings.chain.cooldownBias

  private class Link(val boss: UUID, var hitsLeft: Int)

  /** Chained players and what holds them. */
  private val links = HashMap<UUID, Link>()

  private val LINK = Particle.DustOptions(Color.fromRGB(0x5A, 0x5A, 0x64), 1.2f)

  override fun usable(cast: Cast) = target(cast) != null

  private fun target(cast: Cast): Player? {
    val cfg = cast.settings.chain
    return cast.players(cast.at, cfg.range)
      .filter { it.uniqueId !in links && it.location.distance(cast.at) in cfg.minDistance..cfg.range && cast.boss.entity.hasLineOfSight(it) }
      .maxByOrNull { it.location.distanceSquared(cast.at) }
  }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.chain
    val target = target(cast) ?: return false
    val link = Link(cast.boss.uuid, cfg.breakHits.coerceAtLeast(1))
    links[target.uniqueId] = link
    val ticks = (Rolls.linear(cfg.secondsBase, cfg.secondsPerLevel, cfg.secondsMax, cast.level) * 20).toInt()
    val damage = Rolls.linear(cfg.damageBase, cfg.damagePerLevel, cfg.damageMax, cast.level)
    cast.sound(Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.6f)
    cast.sound(Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.6f, target.location)

    var tick = 0
    var lastYank = -20
    cast.every(4) { task ->
      tick += 4
      val boss = cast.boss.entity
      val gone = !boss.isValid || boss.isDead || !target.isOnline || target.isDead || target.world != boss.world || !cast.canHit(target)
      if (gone || tick >= ticks || links[target.uniqueId] !== link) {
        task.cancel()
        links.remove(target.uniqueId, link)
        return@every
      }
      val from = boss.location.add(0.0, boss.height * 0.6, 0.0)
      val to = target.location.add(0.0, 1.0, 0.0)
      draw(from, to)
      val away = to.toVector().subtract(from.toVector())
      if (away.length() > cfg.length && tick - lastYank >= 20) {
        lastYank = tick
        target.velocity = away.multiply(-1).normalize().multiply(1.1).setY(0.35)
        target.damage(damage, boss)
        cast.sound(Sound.BLOCK_CHAIN_BREAK, 1f, 0.5f, target.location)
      }
    }
    return true
  }

  /** A player's blow to [boss] in melee: one fewer to break their chain, if it holds them. */
  fun struck(plugin: MoBossesPlugin, boss: Boss, player: Player) {
    val link = links[player.uniqueId]?.takeIf { it.boss == boss.uuid } ?: return
    if (--link.hitsLeft > 0) return player.world.playSound(player.location, Sound.BLOCK_CHAIN_HIT, 1f, 1.2f)
    links.remove(player.uniqueId)
    player.world.playSound(player.location, Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.4f)
    player.world.spawnParticle(Particle.CRIT, player.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.5, 0.4, 0.2)
    plugin.texts.send(player, Message.CHAIN_BROKEN, Placeholder.unparsed("player", player.name), *plugin.texts.boss(plugin.texts.languageOf(player), boss))
  }

  fun clear() = links.clear()

  /** Links of dust from the boss to the player, a point every half block. */
  private fun draw(from: Location, to: Location) {
    val step = to.toVector().subtract(from.toVector())
    val points = (step.length() * 2).toInt().coerceIn(2, 48)
    step.multiply(1.0 / points)
    val at = from.clone()
    repeat(points) { from.world.spawnParticle(Particle.DUST, at.add(step), 1, 0.0, 0.0, 0.0, 0.0, LINK) }
  }
}
