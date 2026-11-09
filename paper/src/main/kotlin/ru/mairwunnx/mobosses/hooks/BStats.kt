package ru.mairwunnx.mobosses.hooks

import org.bstats.bukkit.Metrics
import org.bstats.charts.AdvancedPie
import org.bstats.charts.CustomChart
import org.bstats.charts.DrilldownPie
import org.bstats.charts.SingleLineChart
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.metrics.Tallies
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * bStats, while `metrics` is on and [BSTATS_ID] is set. bStats sends every 30 minutes from its own thread, so the
 * charts only drain [tallies], which the server thread fills; nothing here calls Bukkit then. Any error costs
 * one warning and never the plugin.
 */
class BStats(private val plugin: MoBossesPlugin) {
  val tallies = Tallies()

  private var metrics: Metrics? = null
  private val warned = AtomicBoolean()

  /** Starts or stops bStats to match `metrics` in config.yml; called on every load. */
  fun apply(enabled: Boolean) {
    if (!enabled || BSTATS_ID == 0) return stop()
    if (metrics != null) return
    metrics = guard { Metrics(plugin, BSTATS_ID).also { metrics -> charts().forEach(metrics::addCustomChart) } }
  }

  fun stop() {
    metrics?.let { guard { it.shutdown() } }
    metrics = null
  }

  private fun charts(): List<CustomChart> = listOf(
    AdvancedPie("player_languages", soft { tallies.languages.drain() }),
    AdvancedPie("language_source", soft { tallies.languageSources.drain() }),
    SingleLineChart("bosses_spawned", soft { tallies.spawned.drain() }),
    SingleLineChart("bosses_killed", soft { tallies.killed.drain() }),
    AdvancedPie("spawns_by_grade", soft { tallies.spawnsByGrade.drain() }),
    AdvancedPie("kills_by_grade", soft { tallies.killsByGrade.drain() }),
    DrilldownPie("kills_by_grade_mob", soft { tallies.killsByGradeMob.drain() }),
  )

  /** A chart's callback; on an error bStats gets null and skips the chart. */
  private fun <T> soft(block: () -> T): Callable<T?> = Callable { guard(block) }

  private fun <T> guard(block: () -> T): T? = try {
    block()
  } catch (error: Throwable) {
    if (warned.compareAndSet(false, true)) plugin.log.warn("bStats failed, and Mo’Bosses goes on without it: $error")
    null
  }

  companion object {
    /** The plugin's id on bstats.org, at bstats.org/plugin/bukkit/MoBosses/35229; 0 keeps bStats off. */
    const val BSTATS_ID = 35229
  }
}
