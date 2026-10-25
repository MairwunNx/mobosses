package ru.mairwunnx.mobosses.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import ru.mairwunnx.mobosses.modcore.Mod

/**
 * Mo’Bosses on Fabric: hands Fabric API's events to the shared mod ([Mod]), for each server the game starts, a
 * dedicated one or the one under a singleplayer or LAN world. Only a frame so far.
 */
class MoBossesMod : ModInitializer {
  override fun onInitialize() {
    Mod.loader = OnFabric
    ServerLifecycleEvents.SERVER_STARTED.register(Mod::start)
    ServerLifecycleEvents.SERVER_STOPPING.register { Mod.stop() }
  }
}
