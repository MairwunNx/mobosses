package ru.mairwunnx.mobosses.neoforge.client

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.NetworkRegistry
import ru.mairwunnx.mobosses.modcore.client.ClientLoader
import ru.mairwunnx.mobosses.modcore.client.ClientLoot
import ru.mairwunnx.mobosses.modcore.client.SettingsScreen
import ru.mairwunnx.mobosses.modcore.client.SettingsSource
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Mo’Bosses in a player's game on NeoForge: the settings button in the mod list, the server's answers for the
 * settings screen and its loot pages for JEI, and the way to send to it.
 */
@Mod(value = "mobosses", dist = [Dist.CLIENT])
class MoBossesClient(container: ModContainer, modBus: IEventBus) {
  init {
    ClientLoader.current = object : ClientLoader {
      override fun send(payload: CustomPacketPayload) = ClientPacketDistributor.sendToServer(payload)
      override fun canSend(type: CustomPacketPayload.Type<*>) = Minecraft.getInstance().connection?.let { NetworkRegistry.hasChannel(it, type.id()) } == true
    }
    container.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { _, parent -> SettingsScreen(parent) })
    modBus.addListener { event: RegisterClientPayloadHandlersEvent ->
      event.register(Payloads.Settings.TYPE) { payload, _ -> SettingsSource.Remote.received(payload) }
      event.register(Payloads.LootPages.TYPE) { payload, _ -> ClientLoot.received(payload) }
    }
    NeoForge.EVENT_BUS.addListener { _: ClientPlayerNetworkEvent.LoggingOut -> ClientLoot.left() }
  }
}
