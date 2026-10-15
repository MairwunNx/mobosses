package ru.mairwunnx.mobosses

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.lang.Message

/**
 * `/mobosses reload`, `/mobosses spawn <entity> <level> <class> [x y z [world]]`, and
 * `/mobosses killall`, each under its own permission; `mobosses.admin` gives all three. A player
 * spawns a boss two blocks ahead of them; the console gives coordinates.
 */
class MoBossesCommand(private val plugin: MoBossesPlugin) : BasicCommand {
  override fun canUse(sender: CommandSender) = SUBCOMMANDS.values.any(sender::hasPermission)

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    val sub = args.firstOrNull()?.lowercase()
    val permission = SUBCOMMANDS[sub] ?: return plugin.texts.send(sender, Message.USAGE)
    if (!sender.hasPermission(permission)) return plugin.texts.send(sender, Message.NO_PERMISSION)

    when (sub) {
      "reload" -> plugin.texts.send(sender, if (plugin.reload()) Message.RELOADED else Message.RELOAD_FAILED)
      "spawn" -> spawn(sender, args.drop(1))
      "killall" -> {
        val bosses = plugin.bosses.all.toList()
        bosses.forEach(plugin.bosses::remove)
        if (bosses.isEmpty()) plugin.texts.send(sender, Message.KILLED_NONE)
        else plugin.texts.send(sender, Message.KILLED_ALL, Placeholder.unparsed("count", bosses.size.toString()))
      }
    }
  }

  private fun spawn(sender: CommandSender, args: List<String>) {
    val catalog = plugin.catalog
    if (args.size !in setOf(3, 6, 7)) return usage(sender)
    val type = catalog.types.values.firstOrNull { it.key.equals(args[0].removePrefix("minecraft:"), ignoreCase = true) }
      ?: return plugin.texts.send(sender, Message.UNKNOWN_ENTITY, Placeholder.unparsed("entity", args[0]), Placeholder.unparsed("entities", typeKeys().joinToString(", ")))
    val maxLevel = maxOf(100, catalog.settings.progression.levelCurve.maxLevel)
    val level = args[1].toIntOrNull()?.takeIf { it in 1..maxLevel }
      ?: return plugin.texts.send(sender, Message.BAD_LEVEL, Placeholder.unparsed("level", args[1]), Placeholder.unparsed("max", maxLevel.toString()))
    val clazz = catalog.classes.byKey(args[2])
      ?: return plugin.texts.send(sender, Message.UNKNOWN_CLASS, Placeholder.unparsed("class", args[2]), Placeholder.unparsed("classes", catalog.classes.all.joinToString(", ") { it.key }))
    val at = where(sender, args.drop(3)) ?: return plugin.texts.send(sender, Message.SPAWN_WHERE)

    if (!catalog.settings.protection.adminSpawnBypass && !plugin.guards.canSpawnBoss(at)) {
      return plugin.texts.send(sender, Message.SPAWN_FAILED, Placeholder.unparsed("reason", plugin.guards.regionsAt(at).joinToString().ifEmpty { "protected" }))
    }
    // The console may name a spot nobody stands near; since 26.1 even the spawn chunks load only on demand.
    at.world.getChunkAt(at)
    val entity = try {
      plugin.spawns.spawningByCommand = true
      at.world.spawn(at, type.entity.entityClass!!) as? LivingEntity
    } catch (e: IllegalArgumentException) {
      return plugin.texts.send(sender, Message.SPAWN_FAILED, Placeholder.unparsed("reason", e.message ?: "?"))
    } finally {
      plugin.spawns.spawningByCommand = false
    }
    if (entity == null || !entity.isValid) return plugin.texts.send(sender, Message.SPAWN_FAILED, Placeholder.unparsed("reason", "the spawn was cancelled"))

    val boss = plugin.bosses.make(entity, type, clazz, level).also(plugin.bosses::vanillaName)
    plugin.texts.send(sender, Message.SPAWNED, *plugin.texts.boss(plugin.texts.languageOf(sender), boss))
  }

  /** Two blocks ahead of a player, or `x y z [world]` for anyone; the console's world defaults to the first one. */
  private fun where(sender: CommandSender, args: List<String>): Location? {
    val player = sender as? Player
    if (args.isEmpty()) return player?.location?.let { it.clone().add(it.direction.setY(0).normalize().multiply(2.0)) }
    val (x, y, z) = args.take(3).map { it.toDoubleOrNull() ?: return null }
    val world = args.getOrNull(3)?.let { Bukkit.getWorld(it) ?: return null } ?: player?.world ?: Bukkit.getWorlds().first()
    return Location(world, x, y, z)
  }

  private fun usage(sender: CommandSender) =
    plugin.texts.send(sender, Message.SPAWN_USAGE, Placeholder.unparsed("entities", typeKeys().joinToString(", ")), Placeholder.unparsed("classes", plugin.catalog.classes.all.joinToString(", ") { it.key }))

  private fun typeKeys() = plugin.catalog.types.values.map { it.key }.sorted()

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
    val sender = source.sender
    val last = args.lastOrNull().orEmpty()
    val options = when {
      args.size <= 1 -> SUBCOMMANDS.filterValues(sender::hasPermission).keys.toList()
      !args[0].equals("spawn", ignoreCase = true) || !sender.hasPermission(SUBCOMMANDS.getValue("spawn")) -> emptyList()
      args.size == 2 -> typeKeys()
      args.size == 3 -> listOf("1", "10", "25", "50", "75", "100")
      args.size == 4 -> plugin.catalog.classes.all.map { it.key }
      args.size in 5..7 -> (sender as? Player)?.getTargetBlockExact(16)?.let { block -> listOf(block.x, block.y + 1, block.z)[args.size - 5].toString() }?.let(::listOf).orEmpty()
      args.size == 8 -> Bukkit.getWorlds().map { it.name }
      else -> emptyList()
    }
    return options.filter { it.startsWith(last, ignoreCase = true) }
  }

  private companion object {
    val SUBCOMMANDS = linkedMapOf("reload" to "mobosses.reload", "spawn" to "mobosses.spawn", "killall" to "mobosses.killall")
  }
}
