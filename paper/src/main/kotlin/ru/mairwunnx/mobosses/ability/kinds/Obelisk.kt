package ru.mairwunnx.mobosses.ability.kinds

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The boss raises an end crystal a few blocks away that heals it through a beam, as the dragon's do:
 * a share of its max health each second, by level, while it stays in range. Players break it with a
 * few hits; nothing else harms it, and it never explodes. It isn't saved, so a restart takes it.
 */
object Obelisk : Ability {
  override val key = "obelisk"
  override val message = Message.OBELISK
  override fun enabled(settings: AbilitySettings) = settings.obelisk.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.obelisk
  override fun cooldown(settings: AbilitySettings) = settings.obelisk.cooldown to settings.obelisk.cooldownBias

  /** The standing obelisk of each boss. */
  private val standing = HashMap<UUID, EnderCrystal>()

  /** A hurt boss without one standing. */
  override fun usable(cast: Cast): Boolean {
    val max = cast.boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return false
    return cast.boss.entity.health < max * 0.9 && standing[cast.boss.uuid]?.isValid != true
  }

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.obelisk
    val spot = spot(cast.at) ?: return false
    val hits = Rolls.linear(cfg.hitsBase, cfg.hitsPerLevel, cfg.hitsMax, cast.level).coerceAtLeast(1)
    val crystal = spot.world.spawn(spot, EnderCrystal::class.java) {
      it.isShowingBottom = true
      it.isPersistent = false
      it.beamTarget = beamAt(cast.boss)
      it.persistentDataContainer.set(cast.plugin.keys.obelisk, PersistentDataType.STRING, cast.boss.uuid.toString())
      it.persistentDataContainer.set(cast.plugin.keys.obeliskHits, PersistentDataType.INTEGER, hits)
    }
    standing[cast.boss.uuid] = crystal
    spot.world.spawnParticle(Particle.REVERSE_PORTAL, spot.clone().add(0.0, 1.0, 0.0), 60, 0.4, 1.0, 0.4, 0.05)
    cast.sound(Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 0.6f, spot)
    cast.sound(Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f, spot)

    val share = Rolls.linear(cfg.healBase, cfg.healPerLevel, cfg.healMax, cast.level)
    var seconds = 0
    cast.every(20) { task ->
      val boss = cast.boss.entity
      if (!crystal.isValid || !boss.isValid || boss.isDead || ++seconds > cfg.seconds) {
        task.cancel()
        if (crystal.isValid) fade(crystal)
        standing.remove(cast.boss.uuid, crystal)
        return@every
      }
      if (boss.world != crystal.world || boss.location.distanceSquared(crystal.location) > cfg.range * cfg.range) {
        crystal.beamTarget = null
        return@every
      }
      val max = boss.getAttribute(Attribute.MAX_HEALTH)?.value ?: return@every
      crystal.beamTarget = beamAt(cast.boss)
      if (boss.health < max) {
        boss.heal(max * share)
        cast.plugin.bars.update(cast.boss)
        boss.world.spawnParticle(Particle.HEART, boss.location.add(0.0, boss.height + 0.3, 0.0), 2, 0.3, 0.2, 0.3, 0.0)
        beam(crystal.location.add(0.0, 1.0, 0.0), boss.location.add(0.0, boss.height / 2, 0.0))
      }
    }
    return true
  }

  /** A player's hit on an obelisk: one fewer left, and it breaks at none. */
  fun hit(plugin: MoBossesPlugin, crystal: EnderCrystal, player: Player) {
    val data = crystal.persistentDataContainer
    val left = (data.get(plugin.keys.obeliskHits, PersistentDataType.INTEGER) ?: 1) - 1
    crystal.world.playSound(crystal.location, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 0.6f + 0.1f * left.coerceAtLeast(0))
    crystal.world.spawnParticle(Particle.CRIT, crystal.location.add(0.0, 1.0, 0.0), 12, 0.3, 0.5, 0.3, 0.2)
    if (left > 0) return data.set(plugin.keys.obeliskHits, PersistentDataType.INTEGER, left)

    val boss = data.get(plugin.keys.obelisk, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let(plugin.bosses::get)
    crystal.world.spawnParticle(Particle.EXPLOSION, crystal.location.add(0.0, 1.0, 0.0), 2)
    crystal.world.playSound(crystal.location, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f)
    crystal.world.playSound(crystal.location, Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 1.4f)
    crystal.remove()
    plugin.achievements.grant(player, Achievement.OBELISK_BREAKER)
    if (boss != null) {
      standing.remove(boss.uuid, crystal)
      val near = (boss.damage.keys.mapNotNull(org.bukkit.Bukkit::getPlayer) + player).distinct().filter { it.world == crystal.world }
      plugin.texts.sendAll(near, Message.OBELISK_BROKEN) { code -> arrayOf(Placeholder.unparsed("player", player.name), *plugin.texts.boss(code, boss)) }
    }
  }

  /** Every obelisk, for shutdown. */
  fun removeAll() {
    standing.values.filter { it.isValid }.forEach { it.remove() }
    standing.clear()
  }

  /** Solid ground four to six blocks from the boss, where a crystal fits. */
  private fun spot(at: Location): Location? {
    repeat(8) {
      val angle = Random.nextDouble(0.0, Math.PI * 2)
      val distance = Random.nextDouble(4.0, 6.0)
      val x = at.x + cos(angle) * distance
      val z = at.z + sin(angle) * distance
      val ground = Targets.ground(at.world, x, at.y, z, 4) ?: return@repeat
      if (Targets.isOpen(ground.getRelative(0, 2, 0))) return Location(at.world, ground.x + 0.5, ground.y + 1.0, ground.z + 0.5)
    }
    return null
  }

  private fun beamAt(boss: Boss) = boss.entity.location.add(0.0, boss.entity.height / 2, 0.0)

  /** Sparks along the beam, so the healing shows even where the beam is hard to see. */
  private fun beam(from: Location, to: Location) {
    val step = to.toVector().subtract(from.toVector())
    val points = (step.length() * 2).toInt().coerceIn(2, 32)
    step.multiply(1.0 / points)
    val at = from.clone()
    repeat(points) { from.world.spawnParticle(Particle.HAPPY_VILLAGER, at.add(step), 1, 0.0, 0.0, 0.0, 0.0) }
  }

  private fun fade(crystal: EnderCrystal) {
    crystal.world.spawnParticle(Particle.REVERSE_PORTAL, crystal.location.add(0.0, 1.0, 0.0), 40, 0.4, 0.8, 0.4, 0.05)
    crystal.world.playSound(crystal.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f)
    crystal.remove()
  }
}
