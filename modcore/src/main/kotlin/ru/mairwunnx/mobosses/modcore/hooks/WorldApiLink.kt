package ru.mairwunnx.mobosses.modcore.hooks

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.progress.Deaths
import java.util.UUID

/**
 * Mo’Bosses' data in WorldAPI, when it runs, as on Paper: the `mobosses` namespace and its events. [BossesExpansion]
 * speaks WorldAPI's expansion API; this class holds no type of it, so it loads without WorldAPI. Worlds go by their
 * dimension's id, as WorldAPI names them on the mods.
 */
class WorldApiLink(private val mod: MoBosses) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when WorldAPI is installed; nothing happens without it. */
  fun connect() {
    if (!mod.loader.loaded(WORLD_API)) return
    try {
      BossesExpansion.mod = mod
      BossesExpansion.register()
      connected = true
      mod.log.info("WorldAPI found: hunters, cards, bestiaries, bosses, and events at /extensions/${BossesExpansion.id()}")
    } catch (error: ExceptionInInitializerError) {
      mod.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.cause?.message ?: error}")
    } catch (error: LinkageError) {
      // WorldAPI before 4.5.0 has no expansion API 2.
      mod.log.warn("WorldAPI is too old for Mo’Bosses' data: it needs 4.5.0 or newer")
    } catch (error: Exception) {
      mod.log.warn("WorldAPI didn't take Mo’Bosses' data: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { BossesExpansion.unregister() }
  }

  fun bossKilled(boss: Boss, shares: Map<UUID, Double>, experience: Int) = publish("boss.killed") { BossesExpansion.bossKilled(boss, shares, experience) }

  fun levelUp(player: ServerPlayer, from: Int, to: Int) = publish("player.level_up") { BossesExpansion.levelUp(player, from, to) }

  fun achievement(player: ServerPlayer, id: String) = publish("player.achievement") { BossesExpansion.achievement(player, id) }

  fun phaseTwo(boss: Boss) = publish("boss.phase_two") { BossesExpansion.phaseTwo(boss) }

  fun diedTo(player: ServerPlayer, boss: Boss, death: Deaths.Death) = publish("player.died_to_boss") { BossesExpansion.diedTo(player, boss, death) }

  fun demoted(player: ServerPlayer, from: Int, to: Int, streak: Int) = publish("player.demoted") { BossesExpansion.demoted(player, from, to, streak) }

  fun fullMoon(world: String) = publish("full_moon") { BossesExpansion.fullMoon(world) }

  /** WorldAPI names a world on the mods by its dimension: `overworld`, or another mod's as `namespace:path`. */
  fun world(entity: Entity): String {
    val id = entity.level().dimension().identifier()
    return if (id.namespace == "minecraft") id.path else id.toString()
  }

  /** An event for WorldAPI's clients; dropped quietly while WorldAPI isn't there or is too old. */
  private inline fun publish(event: String, send: () -> Unit) {
    if (!connected) return
    try {
      send()
    } catch (error: Throwable) {
      mod.log.debug { "WorldAPI didn't take $event: ${error.message}" }
    }
  }

  private companion object {
    const val WORLD_API = "worldapi"
  }
}
