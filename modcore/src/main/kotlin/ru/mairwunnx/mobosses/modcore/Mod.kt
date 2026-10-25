package ru.mairwunnx.mobosses.modcore

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Mo’Bosses for each server the game starts: a dedicated one, or the one under a singleplayer or LAN world. Both mods
 * set [loader] first and hand in their loader's events: the server's start and stop, its ticks, players joining and
 * leaving, and the command tree. Everything for a server lives in [MoBosses], made at the start and dropped at the
 * stop, so a second world in the same game starts clean.
 */
object Mod {
  private val logger = LoggerFactory.getLogger("Mo’Bosses")

  /** The game's log, for what happens outside a server, or before its settings are read. */
  val log = Log(object : Log.Sink {
    override fun info(message: String) = logger.info(message)
    override fun warn(message: String) = logger.warn(message)
    override fun error(message: String, cause: Throwable?) = logger.error(message, cause)
  }) { current?.debug == true }

  lateinit var loader: Loader

  /** Mo’Bosses on the server that runs now; null between servers, as in a singleplayer game's main menu. */
  @Volatile var current: MoBosses? = null; private set

  fun start(server: MinecraftServer) {
    val mod = MoBosses(server, loader)
    try {
      mod.enable()
      current = mod
    } catch (error: Throwable) {
      log.error("Mo’Bosses couldn't start", error)
      runCatching { mod.disable() }.onFailure { log.error("Mo’Bosses couldn't stop what it started", it) }
    }
  }

  fun stop() {
    val mod = current ?: return
    current = null
    mod.disable()
  }

  /** The end of every server tick. */
  fun tick() {
    current?.tick()
  }

  fun joined(player: ServerPlayer) {
    current?.joined(player)
  }

  fun left(player: ServerPlayer) {
    current?.left(player)
  }

  /** A player's game asks for the settings screen's files. */
  fun settingsWanted(player: ServerPlayer) {
    current?.settings?.wanted(player)
  }

  /** A player's game changes settings. */
  fun settingsChanged(player: ServerPlayer, batch: Payloads.SettingsChanged) {
    current?.settings?.changed(player, batch)
  }

  /** The command tree, which the game builds before a server starts; the commands find the server when they run. */
  fun commands(dispatcher: CommandDispatcher<CommandSourceStack>) = MoBossesCommand.register(dispatcher)
}
