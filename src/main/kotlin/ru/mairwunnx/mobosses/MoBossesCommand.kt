package ru.mairwunnx.mobosses

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.hooks.PlanData
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.menu.LanguagePage
import ru.mairwunnx.mobosses.menu.Menu

/**
 * `/mobosses` (the menu), `info`, `top`, `reset`, and `language [code|auto]` for players; `reload`,
 * `spawn <entity> <level> <class> [x y z [world]]`, and `killall` for admins, each under its own
 * permission, all of them under `mobosses.admin`. A player spawns a boss two blocks ahead of them; the console gives coordinates.
 */
class MoBossesCommand(private val plugin: MoBossesPlugin) : BasicCommand {
  override fun canUse(sender: CommandSender) = SUBCOMMANDS.values.any(sender::hasPermission)

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    val sub = args.firstOrNull()?.lowercase()
    if (sub == null && sender is Player && sender.hasPermission(USE)) return Menu.open(plugin, sender)
    val permission = SUBCOMMANDS[sub] ?: return plugin.texts.send(sender, Message.USAGE)
    if (!sender.hasPermission(permission)) return plugin.texts.send(sender, Message.NO_PERMISSION)

    when (sub) {
      "info" -> info(sender, args.getOrNull(1))
      "top" -> top(sender)
      "reset" -> reset(sender, args.getOrNull(1))
      "language" -> language(sender, args.getOrNull(1))
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

  /** A player's card: yours, or with `mobosses.info.others`, anyone's who ever played. */
  private fun info(sender: CommandSender, name: String?) {
    val target: OfflinePlayer = when {
      name == null -> sender as? Player ?: return plugin.texts.send(sender, Message.USAGE)
      !sender.hasPermission(INFO_OTHERS) && !name.equals(sender.name, ignoreCase = true) -> return plugin.texts.send(sender, Message.NO_PERMISSION)
      else -> Bukkit.getPlayerExact(name) ?: Bukkit.getOfflinePlayerIfCached(name)
        ?: return plugin.texts.send(sender, Message.PLAYER_NOT_FOUND, Placeholder.unparsed("player", name))
    }
    plugin.menuStats.card(target).whenComplete { card, error ->
      Bukkit.getScheduler().runTask(plugin, Runnable {
        if (error != null) return@Runnable plugin.texts.send(sender, Message.NO_DATABASE)
        plugin.texts.send(sender, Message.INFO, *plugin.menuStats.resolvers(plugin.texts.languageOf(sender), card))
      })
    }
  }

  private fun top(sender: CommandSender) {
    plugin.menuStats.top().whenComplete { hunters, error ->
      Bukkit.getScheduler().runTask(plugin, Runnable {
        if (error != null) return@Runnable plugin.texts.send(sender, Message.NO_DATABASE)
        if (hunters.isEmpty()) return@Runnable plugin.texts.send(sender, Message.TOP_EMPTY)
        plugin.texts.send(sender, Message.TOP_HEADER)
        hunters.forEachIndexed { i, hunter ->
          plugin.texts.send(
            sender, Message.TOP_LINE, Placeholder.unparsed("rank", (i + 1).toString()), Placeholder.unparsed("player", plugin.menuStats.name(hunter)),
            Placeholder.unparsed("level", plugin.menuStats.level(hunter.progress).toString()), Placeholder.unparsed("kills", hunter.progress.kills.toString()),
          )
        }
      })
    }
  }

  /**
   * Your own progress after `confirm`, so a stray command wipes nothing; with `mobosses.reset.others`,
   * anyone's at once. Cooldowns stay: a reset doesn't bring the next boss any sooner.
   */
  private fun reset(sender: CommandSender, arg: String?) {
    if (arg == null || arg.equals(CONFIRM, ignoreCase = true)) {
      val player = sender as? Player ?: return plugin.texts.send(sender, Message.USAGE)
      return if (arg == null) plugin.texts.send(sender, Message.RESET_ASK) else reset(sender, player)
    }
    if (!sender.hasPermission(RESET_OTHERS)) return plugin.texts.send(sender, Message.NO_PERMISSION)
    val target = Bukkit.getPlayerExact(arg) ?: Bukkit.getOfflinePlayerIfCached(arg)
      ?: return plugin.texts.send(sender, Message.PLAYER_NOT_FOUND, Placeholder.unparsed("player", arg))
    reset(sender, target)
  }

  private fun reset(sender: CommandSender, target: OfflinePlayer) {
    plugin.progression.reset(target.uniqueId).whenComplete { _, error ->
      Bukkit.getScheduler().runTask(plugin, Runnable {
        if (error != null) {
          plugin.log.warn("Can't reset the progress of ${target.name}: ${error.message}")
          return@Runnable plugin.texts.send(sender, Message.NO_DATABASE)
        }
        plugin.log.info("${sender.name} reset the Mo’Bosses progress of ${target.name ?: target.uniqueId}")
        val online = target.player
        if (online != null && PlanData.available) PlanData.update(online)
        online?.let { plugin.texts.send(it, Message.RESET_DONE) }
        if (online != sender) plugin.texts.send(sender, Message.RESET_DONE_OTHER, Placeholder.unparsed("player", target.name ?: target.uniqueId.toString()))
      })
    }
  }

  /** A language for a player to read the plugin in, `auto` to leave it to the plugin, or with nothing, the picker. */
  private fun language(sender: CommandSender, arg: String?) {
    val player = sender as? Player ?: return plugin.texts.send(sender, Message.USAGE)
    if (arg == null) return LanguagePage.open(plugin, player)
    if (arg.equals(LanguageChoices.AUTO, ignoreCase = true)) return plugin.playerLanguages.choose(player, null)
    val codes = plugin.playerLanguages.codes()
    val code = LanguageChoices.match(arg, codes) ?: return plugin.texts.send(
      sender, Message.LANGUAGE_UNKNOWN, Placeholder.unparsed("language", arg),
      Placeholder.unparsed("languages", codes.joinToString(", ") { "$it (${LanguageChoices.nativeName(it)})" }),
    )
    plugin.playerLanguages.choose(player, code)
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
    boss.pinned = true
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
      args.size == 2 && args[0].equals("info", ignoreCase = true) && sender.hasPermission(INFO_OTHERS) -> Bukkit.getOnlinePlayers().map { it.name }
      args.size == 2 && args[0].equals("language", ignoreCase = true) && sender is Player -> plugin.playerLanguages.codes() + LanguageChoices.AUTO
      args.size == 2 && args[0].equals("reset", ignoreCase = true) ->
        listOf(CONFIRM) + if (sender.hasPermission(RESET_OTHERS)) Bukkit.getOnlinePlayers().map { it.name } else emptyList()
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
    const val USE = "mobosses.use"
    const val INFO_OTHERS = "mobosses.info.others"
    const val RESET_OTHERS = "mobosses.reset.others"
    const val CONFIRM = "confirm"
    val SUBCOMMANDS = linkedMapOf("info" to USE, "top" to USE, "reset" to "mobosses.reset", "language" to USE, "reload" to "mobosses.reload", "spawn" to "mobosses.spawn", "killall" to "mobosses.killall")
  }
}
