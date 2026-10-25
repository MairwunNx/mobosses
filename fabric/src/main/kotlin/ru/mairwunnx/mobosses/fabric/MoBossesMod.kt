package ru.mairwunnx.mobosses.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.achievement.BossKilled
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Mo’Bosses on Fabric: hands Fabric API's events to the shared mod ([Mod]), for each server the game starts, a
 * dedicated one or the one under a singleplayer or LAN world. What Fabric API has no event for, modcore's mixins
 * catch on both loaders.
 */
class MoBossesMod : ModInitializer {
  override fun onInitialize() {
    Mod.loader = OnFabric
    Registry.register(BuiltInRegistries.TRIGGER_TYPES, BossKilled.ID, BossKilled.TRIGGER)
    ServerLifecycleEvents.SERVER_STARTED.register(Mod::start)
    ServerLifecycleEvents.SERVER_STOPPING.register { Mod.stop() }
    ServerTickEvents.END_SERVER_TICK.register { Mod.tick() }
    CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> Mod.commands(dispatcher) }
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> Mod.joined(handler.player) }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> Mod.left(handler.player) }
    // What a player's game with Mo’Bosses says to the server, and back; Fabric API runs the handlers on the server thread.
    PayloadTypeRegistry.serverboundPlay().register(Payloads.SettingsWanted.TYPE, Payloads.SettingsWanted.CODEC)
    PayloadTypeRegistry.serverboundPlay().register(Payloads.SettingsChanged.TYPE, Payloads.SettingsChanged.CODEC)
    PayloadTypeRegistry.clientboundPlay().register(Payloads.Settings.TYPE, Payloads.Settings.CODEC)
    PayloadTypeRegistry.clientboundPlay().register(Payloads.LootPages.TYPE, Payloads.LootPages.CODEC)
    ServerPlayNetworking.registerGlobalReceiver(Payloads.SettingsWanted.TYPE) { _, context -> Mod.settingsWanted(context.player()) }
    ServerPlayNetworking.registerGlobalReceiver(Payloads.SettingsChanged.TYPE) { change, context -> Mod.settingsChanged(context.player(), change) }
  }
}
