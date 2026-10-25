package ru.mairwunnx.mobosses.modcore.boss

import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.scores.TeamColor
import ru.mairwunnx.mobosses.modcore.MoBosses
import java.util.Optional

/**
 * The glow in the class color and the particle aura, as on Paper. Both show only while a player is within
 * `effects.visibility_radius`, so idle bosses cost nothing. The glow color comes from a scoreboard team per class,
 * `mobosses_<class>`.
 */
class Visuals(private val mod: MoBosses) {
  /** Once a second. */
  fun tick() {
    val radius = mod.catalog.settings.effects.visibilityRadius.toDouble()
    mod.bosses.all.toList().forEach { boss ->
      if (!boss.entity.isAlive) return@forEach mod.bosses.forget(boss)
      if (mod.players(boss.entity, radius).isNotEmpty()) on(boss) else off(boss)
    }
  }

  private fun on(boss: Boss) {
    val effects = mod.catalog.settings.effects
    if (effects.glow.enabled && !boss.entity.hasGlowingTag() && (effects.glow.classes.isEmpty() || effects.glow.classes.any { it.equals(boss.clazz.key, true) })) {
      val scoreboard = mod.server.scoreboard
      val name = team(boss)
      val team = scoreboard.getPlayerTeam(name) ?: scoreboard.addPlayerTeam(name)
      val color = TeamColor.CODEC.byName(NamedTextColor.nearestTo(boss.color).toString())
      if (color != null && team.color != Optional.of(color)) team.setColor(Optional.of(color))
      scoreboard.addPlayerToTeam(boss.uuid.toString(), team)
      boss.entity.setGlowingTag(true)
    }
    if (effects.particles.enabled && boss.aura == null) {
      val particle = boss.type.particle ?: return
      boss.aura = mod.scheduler.every(effects.particles.updateInterval.coerceAtLeast(1).toLong(), 1) {
        val entity = boss.entity
        val level = entity.level() as? ServerLevel ?: return@every
        if (!entity.isAlive) return@every
        level.sendParticles(particle, entity.x, entity.y + 1.0, entity.z, 5, 0.5, 0.5, 0.5, 0.0)
        // A boss in its second phase burns with soul fire.
        if (boss.phaseTwo) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.x, entity.y + 1.0, entity.z, 3, 0.4, 0.6, 0.4, 0.01)
      }
    }
  }

  fun off(boss: Boss) {
    boss.aura?.cancel()
    boss.aura = null
    if (boss.entity.hasGlowingTag()) {
      val scoreboard = mod.server.scoreboard
      scoreboard.getPlayerTeam(team(boss))?.let { team -> if (scoreboard.getPlayersTeam(boss.uuid.toString()) == team) scoreboard.removePlayerFromTeam(boss.uuid.toString(), team) }
      boss.entity.setGlowingTag(false)
    }
  }

  private fun team(boss: Boss) = "mobosses_${boss.clazz.key}"
}
