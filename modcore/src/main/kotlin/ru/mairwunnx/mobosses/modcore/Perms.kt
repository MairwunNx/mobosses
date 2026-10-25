package ru.mairwunnx.mobosses.modcore

import net.minecraft.commands.CommandSourceStack
import ru.mairwunnx.mobosses.Permissions
import net.minecraft.server.permissions.Permissions as Game

/**
 * Mo’Bosses' permissions on the mods, as Allay's: through the loader's permissions API when a permissions mod answers
 * (LuckPerms does), else as `paper-plugin.yml` gives them by default ([Permissions.byDefault]). `mobosses.admin` holds
 * every other one. The owner of a singleplayer world may reload and change the settings without cheats; spawning and
 * removing bosses still ask for cheats.
 */
object Perms {
  fun check(source: CommandSourceStack, node: String): Boolean {
    if (node in OWNERS && owner(source)) return true
    val permissions = source.permissions()
    val operator = permissions.hasPermission(Game.COMMANDS_GAMEMASTER)
    val fallback = Permissions.byDefault(node, operator, permissions.hasPermission(Game.COMMANDS_OWNER))
    if (Mod.loader.permission(source, node, fallback)) return true
    return node in Permissions.CHILDREN && Mod.loader.permission(source, Permissions.ADMIN, operator)
  }

  private val OWNERS = setOf(Permissions.RELOAD, Permissions.CONFIG)

  /** The player whose game runs this singleplayer world, opened to LAN or not. */
  fun owner(source: CommandSourceStack): Boolean {
    val player = source.player ?: return false
    return !source.server.isDedicatedServer && source.server.isSingleplayerOwner(player.nameAndId())
  }
}
