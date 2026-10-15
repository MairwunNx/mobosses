package ru.mobosses.managers

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit

class ProximityManager(private val plugin: PluginUnit) {
  private val bosses = plugin.bossManager
  private val effects = plugin.effectsManager

  private val config = plugin.configuration[GeneralConfigurationModel::class]
  private val radius = plugin.configuration[GeneralConfigurationModel::class].effects.visibilityRadius
  private val tick = object : BukkitRunnable() { override fun run() = tick() }
  private var task: BukkitTask? = null

  fun initialize() {
     task = tick.runTaskTimer(plugin, 20L, 20L)
  }

  fun tick() {
    bosses.activeBosses().forEach { (_, boss) ->
      val e = boss.entity
      val w = e.world

      val viewers = w.nearbyPlayersBox(e.location, radius.toDouble())
      val hasViewer = viewers.isNotEmpty()
      val glowingNow = e.isGlowing

      if (hasViewer && !glowingNow) {
        if (config.effects.glow.enabled) effects.applyGlowEffect(e, boss)
        if (config.effects.particles.enabled) effects.startAuraEffect(boss)
        plugin.logger.debug { "Viewers entered range of ${boss.bossType.name}, enabling glow/aura" }
      } else if (!hasViewer && glowingNow) {
        if (config.effects.glow.enabled) effects.removeGlowEffect(e, boss)
        if (config.effects.particles.enabled) effects.stopAuraEffect(boss.uuid)
        plugin.logger.debug { "No viewers near ${boss.bossType.name}, disabling glow/aura" }
      }
    }
  }

  private fun World.nearbyPlayersBox(center: Location, r: Double, extraFilter: (Player) -> Boolean = { true }): List<Player> {
    val box = BoundingBox(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r)
    return getNearbyEntities(box, {
      it is Player && it.isValid && it.isOnline && it.gameMode != GameMode.SPECTATOR && extraFilter(it)
    }).map { it as Player }
  }

  fun cleanup() {
    task?.cancel()
    task = null
  }
}