package ru.mairwunnx.mobosses.neoforge

import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.achievement.BossKilled
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Mo’Bosses on NeoForge: hands NeoForge's events to the shared mod ([Mod]), for each server the game starts, a
 * dedicated one or the one under a singleplayer or LAN world. What NeoForge has no event for, modcore's mixins catch
 * on both loaders.
 */
@net.neoforged.fml.common.Mod("mobosses")
class MoBossesMod(modBus: IEventBus) {
  init {
    Mod.loader = OnNeoForge
    modBus.addListener { event: RegisterEvent -> event.register(Registries.TRIGGER_TYPE, BossKilled.ID) { BossKilled.TRIGGER } }
    // What a player's game with Mo’Bosses says to the server, and back; optional, so a vanilla game joins as it is.
    modBus.addListener { event: RegisterPayloadHandlersEvent ->
      event.registrar("1").optional()
        .playToServer(Payloads.SettingsWanted.TYPE, Payloads.SettingsWanted.CODEC) { _, context -> (context.player() as? ServerPlayer)?.let(Mod::settingsWanted) }
        .playToServer(Payloads.SettingsChanged.TYPE, Payloads.SettingsChanged.CODEC) { change, context -> (context.player() as? ServerPlayer)?.let { Mod.settingsChanged(it, change) } }
        .playToClient(Payloads.Settings.TYPE, Payloads.Settings.CODEC)
        .playToClient(Payloads.LootPages.TYPE, Payloads.LootPages.CODEC)
    }
    val bus = NeoForge.EVENT_BUS
    bus.addListener { event: ServerStartedEvent -> Mod.start(event.server) }
    bus.addListener { _: ServerStoppingEvent -> Mod.stop() }
    bus.addListener { _: ServerTickEvent.Post -> Mod.tick() }
    bus.addListener { event: RegisterCommandsEvent -> Mod.commands(event.dispatcher) }
    bus.addListener { event: PermissionGatherEvent.Nodes -> OnNeoForge.nodes(event) }
    bus.addListener { event: PlayerEvent.PlayerLoggedInEvent -> (event.entity as? ServerPlayer)?.let(Mod::joined) }
    bus.addListener { event: PlayerEvent.PlayerLoggedOutEvent -> (event.entity as? ServerPlayer)?.let(Mod::left) }
  }
}
