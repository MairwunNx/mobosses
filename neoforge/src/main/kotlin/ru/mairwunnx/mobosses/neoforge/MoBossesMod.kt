package ru.mairwunnx.mobosses.neoforge

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import ru.mairwunnx.mobosses.modcore.Mod

/**
 * Mo’Bosses on NeoForge: hands NeoForge's events to the shared mod ([Mod]), for each server the game starts, a
 * dedicated one or the one under a singleplayer or LAN world. Only a frame so far.
 */
@net.neoforged.fml.common.Mod("mobosses")
class MoBossesMod(@Suppress("UNUSED_PARAMETER") modBus: IEventBus) {
  init {
    Mod.loader = OnNeoForge
    val bus = NeoForge.EVENT_BUS
    bus.addListener { event: ServerStartedEvent -> Mod.start(event.server) }
    bus.addListener { _: ServerStoppingEvent -> Mod.stop() }
  }
}
