package ru.mairwunnx.mobosses.fabric.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import ru.mairwunnx.mobosses.modcore.client.ClientLoader
import ru.mairwunnx.mobosses.modcore.client.SettingsSource
import ru.mairwunnx.mobosses.modcore.net.Payloads

/** Mo’Bosses in a player's game on Fabric: the server's answers for the settings screen, and the way to send it. */
class MoBossesClient : ClientModInitializer {
  override fun onInitializeClient() {
    ClientLoader.current = object : ClientLoader {
      override fun send(payload: CustomPacketPayload) = ClientPlayNetworking.send(payload)
      override fun canSend(type: CustomPacketPayload.Type<*>) = ClientPlayNetworking.canSend(type)
    }
    ClientPlayNetworking.registerGlobalReceiver(Payloads.Settings.TYPE) { payload, _ -> SettingsSource.Remote.received(payload) }
  }
}
