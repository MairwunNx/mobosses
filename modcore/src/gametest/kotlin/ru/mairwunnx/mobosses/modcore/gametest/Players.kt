package ru.mairwunnx.mobosses.modcore.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import java.util.UUID

/** Real players for the game tests, as Allay's: a survival player on an embedded connection, who joins as a client would. */
object Players {
  class Survivor(server: MinecraftServer, level: ServerLevel, cookie: CommonListenerCookie) :
    ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
    /** Everything Mo’Bosses or the game said to this player in chat, as plain text. */
    val chat = mutableListOf<String>()

    lateinit var channel: EmbeddedChannel

    override fun sendSystemMessage(message: Component) {
      chat += message.string
      super.sendSystemMessage(message)
    }
  }

  /** A survival player standing at [at] in the test's structure. */
  fun survivor(helper: GameTestHelper, at: Vec3 = Vec3(1.5, 2.0, 1.5)): Survivor {
    val server = helper.level.server
    val cookie = CommonListenerCookie.createInitial(UUID.randomUUID().let { GameProfile(it, "mb-${it.toString().take(8)}") }, false)
    val player = Survivor(server, helper.level, cookie)
    val connection = Connection(PacketFlow.SERVERBOUND)
    player.channel = EmbeddedChannel(connection)
    server.playerList.placeNewPlayer(connection, player, cookie)
    player.setGameMode(GameType.SURVIVAL)
    player.connection.handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket())
    val world = helper.absoluteVec(at)
    player.teleportTo(world.x, world.y, world.z)
    return player
  }

  /** Mo’Bosses on the test server. */
  val mod: MoBosses get() = Mod.current ?: error("Mo’Bosses isn't on the test server")
}
