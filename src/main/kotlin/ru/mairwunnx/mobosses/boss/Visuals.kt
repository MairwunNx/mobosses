package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * The glow in the class color and the particle aura. Both show only while a player is within
 * `effects.visibility_radius`, so idle bosses cost nothing. The glow color comes from a scoreboard
 * team per class, `mobosses_<class>`.
 */
class Visuals(private val plugin: MoBossesPlugin) {
  /** Once a second. */
  fun tick() {
    val effects = plugin.catalog.settings.effects
    val radius = effects.visibilityRadius.toDouble()
    plugin.bosses.all.toList().forEach { boss ->
      if (!boss.entity.isValid) return@forEach plugin.bosses.forget(boss)
      val seen = boss.entity.world.getNearbyPlayers(boss.entity.location, radius) { it.gameMode != GameMode.SPECTATOR }.isNotEmpty()
      if (seen) on(boss) else off(boss)
    }
  }

  private fun on(boss: Boss) {
    val effects = plugin.catalog.settings.effects
    if (effects.glow.enabled && !boss.entity.isGlowing && (effects.glow.classes.isEmpty() || effects.glow.classes.any { it.equals(boss.clazz.key, true) })) {
      val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
      val name = team(boss)
      val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
      val color = NamedTextColor.nearestTo(boss.color)
      if (team.color() != color) team.color(color)
      team.addEntry(boss.uuid.toString())
      boss.entity.isGlowing = true
    }
    if (effects.particles.enabled && boss.aura == null) {
      val particle = boss.type.particle ?: return
      boss.aura = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
        if (!boss.entity.isValid) return@Runnable
        boss.entity.world.spawnParticle(particle, boss.entity.location.add(0.0, 1.0, 0.0), 5, 0.5, 0.5, 0.5, 0.0)
      }, 0L, effects.particles.updateInterval.coerceAtLeast(1).toLong())
    }
  }

  fun off(boss: Boss) {
    boss.aura?.cancel()
    boss.aura = null
    if (boss.entity.isGlowing) {
      Bukkit.getScoreboardManager().mainScoreboard.getTeam(team(boss))?.removeEntry(boss.uuid.toString())
      boss.entity.isGlowing = false
    }
  }

  private fun team(boss: Boss) = "mobosses_${boss.clazz.key}"
}
