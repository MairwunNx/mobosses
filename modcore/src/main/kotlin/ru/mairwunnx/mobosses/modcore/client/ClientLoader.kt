package ru.mairwunnx.mobosses.modcore.client

import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * What a loader hands the client side of Mo’Bosses: sending to the server a player plays on, and whether that server
 * has Mo’Bosses' channel. Only a player's game has one; a dedicated server never loads this package.
 */
interface ClientLoader {
  fun send(payload: CustomPacketPayload)

  /** Whether the server the game is on takes [type]: false in the main menu, and on a server without the mod. */
  fun canSend(type: CustomPacketPayload.Type<*>): Boolean

  companion object {
    lateinit var current: ClientLoader
  }
}
