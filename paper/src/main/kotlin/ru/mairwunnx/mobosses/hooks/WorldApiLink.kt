package ru.mairwunnx.mobosses.hooks

import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.progress.Deaths
import java.util.UUID

/**
 * Mo’Bosses' data in WorldAPI, when it runs: the `mobosses` namespace with the best hunters, players'
 * cards and bestiaries, the bosses alive, and the counts, and events for kills, levels, achievements,
 * second phases, deaths to bosses, and full moons. [BossesData] speaks WorldAPI's expansion API;
 * this class holds no type of it, so it loads without WorldAPI. Mo’Bosses keeps its data; WorldAPI reads
 * it on request.
 */
class WorldApiLink(private val plugin: MoBossesPlugin) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when WorldAPI is installed; nothing happens without it. */
  fun connect() {
    if (plugin.server.pluginManager.getPlugin(WORLD_API) == null) return
    try {
      val actions = BossesData.connect(plugin)
      connected = true
      plugin.log.info("WorldAPI found: hunters, cards, bestiaries, bosses, and events at /extensions/${BossesData.current.id()}")
      if (!actions) plugin.log.warn("This WorldAPI takes no actions: WorldAPI 4.6.0 or newer lets its clients spawn and remove bosses")
    } catch (error: ExceptionInInitializerError) {
      plugin.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.cause?.message ?: error}")
    } catch (error: LinkageError) {
      // WorldAPI before 4.5.0 has no expansion API 2.
      plugin.log.warn("WorldAPI is too old for Mo’Bosses' data: it needs 4.5.0 or newer")
    } catch (error: Exception) {
      plugin.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { BossesData.current.unregister() }
  }

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) = publish("boss.killed") { BossesData.current.bossKilled(boss, shares, experience) }

  fun levelUp(player: Player, from: Int, to: Int) = publish("player.level_up") { BossesData.current.levelUp(player, from, to) }

  fun achievement(player: Player, id: String) = publish("player.achievement") { BossesData.current.achievement(player, id) }

  fun phaseTwo(boss: Boss) = publish("boss.phase_two") { BossesData.current.phaseTwo(boss) }

  fun diedTo(player: Player, boss: Boss, death: Deaths.Death) = publish("player.died_to_boss") { BossesData.current.diedTo(player, boss, death) }

  fun demoted(player: Player, from: Int, to: Int, streak: Int) = publish("player.demoted") { BossesData.current.demoted(player, from, to, streak) }

  fun fullMoon(world: String) = publish("full_moon") { BossesData.current.fullMoon(world) }

  /** An event for WorldAPI's clients; dropped quietly while WorldAPI isn't there or is too old. */
  private inline fun publish(event: String, send: () -> Unit) {
    if (!connected) return
    try {
      send()
    } catch (error: Throwable) {
      plugin.log.debug { "WorldAPI didn't take $event: ${error.message}" }
    }
  }

  private companion object {
    const val WORLD_API = "WorldAPI"
  }
}
