package ru.mairwunnx.mobosses.hooks

import com.djrapitops.plan.capability.CapabilityService
import com.djrapitops.plan.extension.CallEvents
import com.djrapitops.plan.extension.Caller
import com.djrapitops.plan.extension.DataExtension
import com.djrapitops.plan.extension.ExtensionService
import com.djrapitops.plan.extension.NotReadyException
import com.djrapitops.plan.extension.annotation.NumberProvider
import com.djrapitops.plan.extension.annotation.PluginInfo
import com.djrapitops.plan.extension.annotation.TableProvider
import com.djrapitops.plan.extension.icon.Icon
import com.djrapitops.plan.extension.table.Table
import com.djrapitops.plan.extension.icon.Color
import com.djrapitops.plan.extension.icon.Family
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Mo’Bosses in Plan, when Plan is there: a player's page shows their Mo’Bosses level, experience, and
 * kills, and the players table gets a level column. The server page counts the bosses alive, spawned,
 * and killed, and has tables of the best hunters and of bosses by class and by type. Plan asks from
 * its own threads, so the extension reads the progress cache, a count the server thread keeps, and the
 * database, whose queries run on its own thread.
 */
object PlanData {
  private val NEEDS = listOf("DATA_EXTENSION_VALUES", "DATA_EXTENSION_SHOW_IN_PLAYER_TABLE", "DATA_EXTENSION_TABLES")

  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("Plan")

  @Volatile private var caller: Caller? = null

  /** Bosses in loaded chunks; the server thread sets it once a second. */
  @Volatile var bossesAlive = 0

  /** Registers the extension, now and again whenever Plan reloads. Call only when [available]. */
  fun register(plugin: MoBossesPlugin) {
    val capabilities = CapabilityService.getInstance()
    val missing = NEEDS.filterNot(capabilities::hasCapability)
    if (missing.isNotEmpty()) return plugin.log.warn("Plan is too old for Mo’Bosses' section (it lacks ${missing.joinToString()}); update Plan to see it")
    val extension = Extension(plugin)
    // Plan finishes enabling after the plugins that load before it start, then calls the listener.
    capabilities.registerEnableListener { enabled -> if (enabled) add(plugin, extension, quiet = false) }
    add(plugin, extension, quiet = true)
  }

  private fun add(plugin: MoBossesPlugin, extension: Extension, quiet: Boolean) {
    runCatching { ExtensionService.getInstance().register(extension).orElse(null) }
      .onSuccess { caller = it; if (it != null) plugin.log.info("Plan found: levels, experience, and kills show in its Plugins sections") }
      .onFailure { if (!quiet) plugin.log.warn("Couldn't add Mo’Bosses to Plan: ${it.message}") }
  }

  /** Tells Plan a player's values changed, after a kill. */
  fun update(player: Player) {
    caller?.updatePlayerData(player.uniqueId, player.name)
  }

  @PluginInfo(name = "Mo’Bosses", iconName = "dragon", iconFamily = Family.SOLID, color = Color.RED)
  class Extension(private val plugin: MoBossesPlugin) : DataExtension {
    override fun callExtensionMethodsOn(): Array<CallEvents> = arrayOf(CallEvents.PLAYER_JOIN, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.SERVER_PERIODICAL)

    private fun known(player: UUID) = plugin.progression.known(player) ?: throw NotReadyException()

    @NumberProvider(text = "Level", description = "Their Mo’Bosses level, which sets the level of bosses near them", priority = 100, iconName = "chart-line", iconColor = Color.RED, showInPlayerTable = true)
    fun level(player: UUID): Long = known(player).let { plugin.progression.level(player).toLong() }

    @NumberProvider(text = "Experience", description = "Mo’Bosses experience from bosses they helped kill", priority = 90, iconName = "star", iconColor = Color.PURPLE)
    fun experience(player: UUID): Long = known(player).experience

    @NumberProvider(text = "Bosses killed", description = "Kills that counted toward their level", priority = 80, iconName = "skull", iconColor = Color.GREY)
    fun kills(player: UUID): Long = known(player).kills.toLong()

    @NumberProvider(text = "Bosses alive", description = "Bosses in loaded chunks right now", priority = 100, iconName = "dragon", iconColor = Color.RED)
    fun alive(): Long = bossesAlive.toLong()

    @NumberProvider(text = "Bosses spawned", description = "Every boss that ever spawned", priority = 90, iconName = "egg", iconColor = Color.ORANGE)
    fun spawned(): Long = counts().sumOf { it.spawned }

    @NumberProvider(text = "Bosses killed", description = "Every boss players killed", priority = 80, iconName = "skull", iconColor = Color.GREY)
    fun killed(): Long = counts().sumOf { it.killed }

    @TableProvider(tableColor = Color.RED)
    fun hunters(): Table {
      val table = Table.builder()
        .columnOne("Hunter", icon("user")).columnTwo("Level", icon("chart-line")).columnThree("Kills", icon("skull")).columnFour("Experience", icon("star"))
      plugin.progression.top(25).get(WAIT, TimeUnit.SECONDS).forEach {
        table.addRow(plugin.menuStats.name(it), plugin.menuStats.level(it.progress), it.progress.kills, it.progress.experience)
      }
      return table.build()
    }

    @TableProvider(tableColor = Color.ORANGE)
    fun classes(): Table {
      val table = Table.builder().columnOne("Class", icon("gem")).columnTwo("Spawned", icon("egg")).columnThree("Killed", icon("skull"))
      val byClass = counts().groupBy { it.clazz }
      plugin.catalog.classes.all.forEach { clazz ->
        val rows = byClass[clazz.key].orEmpty()
        table.addRow(clazz.key.replaceFirstChar(Char::uppercase), rows.sumOf { it.spawned }, rows.sumOf { it.killed })
      }
      return table.build()
    }

    @TableProvider(tableColor = Color.DEEP_ORANGE)
    fun types(): Table {
      val table = Table.builder().columnOne("Boss", icon("dragon")).columnTwo("Spawned", icon("egg")).columnThree("Killed", icon("skull"))
      counts().groupBy { it.type }.entries.sortedByDescending { (_, rows) -> rows.sumOf { it.spawned } }.forEach { (type, rows) ->
        table.addRow(type.replace('_', ' ').replaceFirstChar(Char::uppercase), rows.sumOf { it.spawned }, rows.sumOf { it.killed })
      }
      return table.build()
    }

    private fun counts() = plugin.progression.counts().get(WAIT, TimeUnit.SECONDS)

    private fun icon(name: String) = Icon.called(name).build()
  }

  /** Seconds Plan's thread waits for the database. */
  private const val WAIT = 10L
}
