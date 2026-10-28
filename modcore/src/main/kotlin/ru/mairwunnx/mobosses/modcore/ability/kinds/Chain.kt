package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.algebra.Reel
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.boss.Boss
import java.util.UUID

/**
 * The boss chains the farthest archer it fights and sees, and hauls them in like a rope ([Reel]): past its length,
 * which shortens as it goes, it pulls them back in heaves and hurts once a second. Three melee blows on the boss break
 * it, as on Paper.
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

  private val LINK = DustParticleOptions(0x5A5A64, 1.2f)

  override fun usable(cast: Cast) = target(cast) != null

  private fun target(cast: Cast): ServerPlayer? {
    val cfg = cast.settings.chain
    return cast.foes(cfg.range)
      .filter { it.uuid !in links && it.position().distanceTo(cast.at) in cfg.minDistance..cfg.range && cast.entity.hasLineOfSight(it) }
      .maxByOrNull { it.position().distanceToSqr(cast.at) }
  }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.chain
    val target = target(cast) ?: return false
    val link = Link(cast.boss.uuid, cfg.breakHits.coerceAtLeast(1))
    links[target.uuid] = link
    val ticks = (Rolls.linear(cfg.secondsBase, cfg.secondsPerLevel, cfg.secondsMax, cast.level) * 20).toInt()
    val damage = Rolls.linear(cfg.damageBase, cfg.damagePerLevel, cfg.damageMax, cast.level)
    cast.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.6f)
    cast.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.6f, target.position())

    var tick = 0
    var lastHurt = -20
    cast.every(2) { task ->
      tick += 2
      val boss = cast.entity
      val gone = !boss.isAlive || target.hasDisconnected() || target.isDeadOrDying || target.level() != boss.level() || !cast.canHit(target)
      if (gone || tick >= ticks || links[target.uuid] !== link) {
        task.cancel()
        links.remove(target.uuid, link)
        return@every
      }
      val from = boss.position().add(0.0, boss.bbHeight * 0.6, 0.0)
      val to = target.position().add(0.0, 1.0, 0.0)
      if (tick % 4 == 0) draw(cast.world, from, to)
      val away = to.subtract(from)
      val distance = away.length()
      val seconds = tick / 20.0
      val length = Reel.length(cfg, seconds)
      if (distance <= length) return@every
      val back = away.scale(-1.0).normalize()
      cast.push(target, if (distance > length + cfg.slack) back.scale(1.1).multiply(1.0, 0.0, 1.0).add(0.0, 0.35, 0.0) else {
        val pull = Reel.pull(cfg, cast.level, seconds)
        Vec3(back.x * pull, (back.y * pull).coerceIn(-0.08, 0.2), back.z * pull)
      })
      if (tick - lastHurt >= 20) {
        lastHurt = tick
        target.hurtServer(cast.world, cast.world.damageSources().mobAttack(boss), damage.toFloat())
        cast.sound(SoundEvents.CHAIN_BREAK, 1f, 0.5f, target.position())
      }
    }
    return true
  }

  /** A player's blow to [boss] in melee: one fewer to break their chain, if it holds them. */
  fun struck(mod: MoBosses, boss: Boss, player: ServerPlayer) {
    val link = links[player.uuid]?.takeIf { it.boss == boss.uuid } ?: return
    val level = player.level() as? ServerLevel ?: return
    if (--link.hitsLeft > 0) return World.sound(level, player.position(), SoundEvents.CHAIN_HIT, 1f, 1.2f)
    links.remove(player.uuid)
    mod.achievements.grant(player, Achievement.CHAIN_BREAKER)
    World.sound(level, player.position(), SoundEvents.CHAIN_BREAK, 1.2f, 1.4f)
    World.particles(level, ParticleTypes.CRIT, player.position().add(0.0, 1.0, 0.0), 20, 0.4, 0.5, 0.4, 0.2)
    mod.texts.send(player, Message.CHAIN_BROKEN, Placeholder.unparsed("player", player.plainTextName), *mod.texts.boss(mod.texts.languageOf(player), boss))
  }

  fun clear() = links.clear()

  /** Links of dust from the boss to the player, a point every half block. */
  private fun draw(level: ServerLevel, from: Vec3, to: Vec3) {
    val step = to.subtract(from)
    val points = (step.length() * 2).toInt().coerceIn(2, 48)
    val each = step.scale(1.0 / points)
    var at = from
    repeat(points) {
      at = at.add(each)
      World.particles(level, LINK, at, 1, 0.0, 0.0, 0.0, 0.0)
    }
  }
}
