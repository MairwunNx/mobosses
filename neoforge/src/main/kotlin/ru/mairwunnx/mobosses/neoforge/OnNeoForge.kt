package ru.mairwunnx.mobosses.neoforge

import net.minecraft.commands.CommandSourceStack
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.modcore.Loader
import ru.mairwunnx.mobosses.modcore.Mod
import java.nio.file.Path
import net.minecraft.server.permissions.Permissions as Game

/** NeoForge as modcore's loader: FML's mod list and folders, and NeoForge's PermissionAPI. */
object OnNeoForge : Loader {
  override val id = "neoforge"
  override val name = "NeoForge"
  // FML wants a number, so a dev build carries 0.0.0-dev; Mo’Bosses calls it dev on every platform.
  override val version: String
    get() = ModList.get().getModContainerById("mobosses").map { it.modInfo.version.toString() }.orElse("dev").removePrefix("0.0.0-")
  override val config: Path
    get() = FMLPaths.CONFIGDIR.get()

  override fun resource(path: String): ByteArray? =
    ModList.get().getModFileById("mobosses")?.file?.contents?.takeIf { it.containsFile(path) }?.readFile(path)

  override fun loaded(mod: String) = ModList.get().isLoaded(mod)

  /** PermissionAPI answers only for nodes named before the server starts: every one Mo’Bosses has. */
  @Volatile private var nodes: Map<String, PermissionNode<Boolean>> = emptyMap()

  fun nodes(event: PermissionGatherEvent.Nodes) {
    nodes = Permissions.ALL.associateWith { name ->
      PermissionNode("mobosses", name.removePrefix("mobosses."), PermissionTypes.BOOLEAN, { player, _, _ ->
        val permissions = player?.permissions()
        Permissions.byDefault(name, permissions?.hasPermission(Game.COMMANDS_GAMEMASTER) == true, permissions?.hasPermission(Game.COMMANDS_OWNER) == true)
      })
    }
    event.addNodes(nodes.values)
  }

  // A command block or the console has no player to ask PermissionAPI about.
  override fun permission(source: CommandSourceStack, node: String, fallback: Boolean): Boolean {
    val player = source.player ?: return fallback
    val known = nodes[node] ?: return fallback
    return try {
      PermissionAPI.getPermission(player, known)
    } catch (error: Exception) {
      Mod.log.warn("Couldn't ask NeoForge's PermissionAPI about $node: ${error.message}")
      fallback
    }
  }
}
