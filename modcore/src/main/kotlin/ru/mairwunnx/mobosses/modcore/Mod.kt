package ru.mairwunnx.mobosses.modcore

import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

/**
 * Mo’Bosses for each server the game starts: a dedicated one, or the one under a singleplayer or LAN world. Both mods
 * set [loader] first and call [start] and [stop] from their loader's events. Only a frame so far: it says it's there
 * and makes no bosses; the Paper plugin is the one that plays.
 */
object Mod {
  private val log = LoggerFactory.getLogger("Mo’Bosses")

  lateinit var loader: Loader

  fun start(server: MinecraftServer) {
    val side = if (server.isDedicatedServer) "a dedicated server" else "a singleplayer world"
    log.info("Mo’Bosses ${loader.version} for ${loader.name} loaded on $side; bosses come to the mods in a later version")
  }

  fun stop() {}
}
