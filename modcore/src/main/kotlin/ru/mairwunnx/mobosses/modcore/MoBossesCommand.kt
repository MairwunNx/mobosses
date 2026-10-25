package ru.mairwunnx.mobosses.modcore

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.menu.LanguagePage
import ru.mairwunnx.mobosses.modcore.menu.Menu
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * `/mobosses` (aliases `mb`, `moboss`) on the game's command tree, the same as on Paper: the menu, `info`, `top`,
 * `reset`, and `language [code|auto]` for players; `reload`, `spawn <entity> <level> <class> [x y z [world]]`, and
 * `killall` for admins, each under its own permission ([Perms]). One argument takes the rest of the line, so the
 * words parse as Paper's command parses them, and suggestions follow each word.
 */
object MoBossesCommand {
  private const val CONFIRM = "confirm"
  private val SUBCOMMANDS = linkedMapOf(
    "info" to Permissions.USE, "top" to Permissions.USE, "reset" to Permissions.RESET, "language" to Permissions.USE,
    "reload" to Permissions.RELOAD, "spawn" to Permissions.SPAWN, "killall" to Permissions.KILLALL,
  )

  fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
    val root = dispatcher.register(
      Commands.literal("mobosses")
        .requires { source -> SUBCOMMANDS.values.toSet().any { Perms.check(source, it) } }
        .executes { run(it, emptyList()) }
        .then(Commands.argument("args", StringArgumentType.greedyString()).suggests(::suggest).executes { run(it, StringArgumentType.getString(it, "args").split(' ').filter(String::isNotEmpty)) }),
    )
    listOf("mb", "moboss").forEach { alias -> dispatcher.register(Commands.literal(alias).requires(root.requirement).executes { run(it, emptyList()) }.redirect(root)) }
  }

  private fun run(context: CommandContext<CommandSourceStack>, args: List<String>): Int {
    val source = context.source
    val mod = Mod.current ?: return 0
    val sub = args.firstOrNull()?.lowercase()
    val player = source.player
    if (sub == null && player != null && Perms.check(source, Permissions.USE)) return Menu.open(mod, player).let { 1 }
    val permission = SUBCOMMANDS[sub] ?: return mod.texts.send(source, Message.USAGE).let { 0 }
    if (!Perms.check(source, permission)) return mod.texts.send(source, Message.NO_PERMISSION).let { 0 }
    when (sub) {
      "info" -> info(mod, source, args.getOrNull(1))
      "top" -> top(mod, source)
      "reset" -> reset(mod, source, args.getOrNull(1))
      "language" -> language(mod, source, args.getOrNull(1))
      "reload" -> mod.texts.send(source, if (mod.reload()) Message.RELOADED else Message.RELOAD_FAILED)
      "spawn" -> spawn(mod, source, args.drop(1))
      "killall" -> {
        val bosses = mod.bosses.all.toList()
        bosses.forEach(mod.bosses::remove)
        if (bosses.isEmpty()) mod.texts.send(source, Message.KILLED_NONE)
        else mod.texts.send(source, Message.KILLED_ALL, Placeholder.unparsed("count", bosses.size.toString()))
      }
    }
    return 1
  }

  /** A player's card: yours, or with `mobosses.info.others`, anyone's who ever played. */
  private fun info(mod: MoBosses, source: CommandSourceStack, name: String?) {
    val (id, shown) = when {
      name == null -> source.player?.let { it.uuid to it.plainTextName } ?: return mod.texts.send(source, Message.USAGE)
      !Perms.check(source, Permissions.INFO_OTHERS) && !name.equals(source.textName, ignoreCase = true) -> return mod.texts.send(source, Message.NO_PERMISSION)
      else -> mod.idOf(name)?.let { it to (mod.nameOf(it) ?: name) } ?: return mod.texts.send(source, Message.PLAYER_NOT_FOUND, Placeholder.unparsed("player", name))
    }
    mod.menuStats.card(id, shown).whenComplete { card, error ->
      mod.server.execute {
        if (error != null) return@execute mod.texts.send(source, Message.NO_DATABASE)
        mod.texts.send(source, Message.INFO, *mod.menuStats.resolvers(mod.texts.languageOf(source), card))
      }
    }
  }

  private fun top(mod: MoBosses, source: CommandSourceStack) {
    mod.menuStats.top().whenComplete { hunters, error ->
      mod.server.execute {
        if (error != null) return@execute mod.texts.send(source, Message.NO_DATABASE)
        if (hunters.isEmpty()) return@execute mod.texts.send(source, Message.TOP_EMPTY)
        mod.texts.send(source, Message.TOP_HEADER)
        hunters.forEachIndexed { i, hunter ->
          mod.texts.send(
            source, Message.TOP_LINE, Placeholder.unparsed("rank", (i + 1).toString()), Placeholder.unparsed("player", mod.nameOf(hunter)),
            Placeholder.unparsed("level", mod.menuStats.level(hunter.progress).toString()), Placeholder.unparsed("kills", hunter.progress.kills.toString()),
          )
        }
      }
    }
  }

  /**
   * Your own progress after `confirm`, so a stray command wipes nothing; with `mobosses.reset.others`, anyone's at
   * once. Cooldowns stay: a reset doesn't bring the next boss any sooner.
   */
  private fun reset(mod: MoBosses, source: CommandSourceStack, arg: String?) {
    if (arg == null || arg.equals(CONFIRM, ignoreCase = true)) {
      val player = source.player ?: return mod.texts.send(source, Message.USAGE)
      return if (arg == null) mod.texts.send(source, Message.RESET_ASK) else reset(mod, source, player.uuid)
    }
    if (!Perms.check(source, Permissions.RESET_OTHERS)) return mod.texts.send(source, Message.NO_PERMISSION)
    val target = mod.idOf(arg) ?: return mod.texts.send(source, Message.PLAYER_NOT_FOUND, Placeholder.unparsed("player", arg))
    reset(mod, source, target)
  }

  fun reset(mod: MoBosses, source: CommandSourceStack, target: UUID) {
    mod.progression.reset(target).whenComplete { _, error ->
      mod.server.execute {
        val name = mod.nameOf(target) ?: target.toString()
        if (error != null) {
          mod.log.warn("Can't reset the progress of $name: ${error.message}")
          return@execute mod.texts.send(source, Message.NO_DATABASE)
        }
        mod.log.info("${source.textName} reset the Mo’Bosses progress of $name")
        val online = mod.server.playerList.getPlayer(target)
        online?.let { mod.texts.send(it, Message.RESET_DONE) }
        if (online == null || online != source.player) mod.texts.send(source, Message.RESET_DONE_OTHER, Placeholder.unparsed("player", name))
      }
    }
  }

  /** A language for a player to read Mo’Bosses in, `auto` to leave it to Mo’Bosses, or with nothing, the picker. */
  private fun language(mod: MoBosses, source: CommandSourceStack, arg: String?) {
    val player = source.player ?: return mod.texts.send(source, Message.USAGE)
    if (arg == null) return LanguagePage.open(mod, player)
    if (arg.equals(LanguageChoices.AUTO, ignoreCase = true)) return mod.playerLanguages.choose(player, null)
    val codes = mod.playerLanguages.codes()
    val code = LanguageChoices.match(arg, codes) ?: return mod.texts.send(
      source, Message.LANGUAGE_UNKNOWN, Placeholder.unparsed("language", arg),
      Placeholder.unparsed("languages", codes.joinToString(", ") { "$it (${LanguageChoices.nativeName(it)})" }),
    )
    mod.playerLanguages.choose(player, code)
  }

  private fun spawn(mod: MoBosses, source: CommandSourceStack, args: List<String>) {
    val catalog = mod.catalog
    if (args.size !in setOf(3, 6, 7)) return usage(mod, source)
    val type = catalog.types.values.firstOrNull { it.key.equals(args[0].removePrefix("minecraft:"), ignoreCase = true) }
      ?: return mod.texts.send(source, Message.UNKNOWN_ENTITY, Placeholder.unparsed("entity", args[0]), Placeholder.unparsed("entities", typeKeys(mod).joinToString(", ")))
    val maxLevel = maxOf(100, catalog.settings.progression.levelCurve.maxLevel)
    val level = args[1].toIntOrNull()?.takeIf { it in 1..maxLevel }
      ?: return mod.texts.send(source, Message.BAD_LEVEL, Placeholder.unparsed("level", args[1]), Placeholder.unparsed("max", maxLevel.toString()))
    val clazz = catalog.classes.byKey(args[2])
      ?: return mod.texts.send(source, Message.UNKNOWN_CLASS, Placeholder.unparsed("class", args[2]), Placeholder.unparsed("classes", catalog.classes.all.joinToString(", ") { it.key }))
    val (world, at) = where(mod, source, args.drop(3)) ?: return mod.texts.send(source, Message.SPAWN_WHERE)
    val pos = net.minecraft.core.BlockPos.containing(at)
    if (!catalog.settings.protection.adminSpawnBypass && !mod.guards.canSpawnBoss(world, pos)) {
      return mod.texts.send(source, Message.SPAWN_FAILED, Placeholder.unparsed("reason", "protected"))
    }
    val entity = try {
      mod.spawns.spawningByCommand = true
      type.entity.spawn(world, pos, EntitySpawnReason.COMMAND) as? LivingEntity
    } finally {
      mod.spawns.spawningByCommand = false
    }
    if (entity == null || !entity.isAlive) return mod.texts.send(source, Message.SPAWN_FAILED, Placeholder.unparsed("reason", "the spawn was cancelled"))
    entity.snapTo(at.x, at.y, at.z, entity.yRot, entity.xRot)
    val boss = mod.bosses.make(entity, type, clazz, level).also(mod.bosses::vanillaName)
    boss.pinned = true
    mod.texts.send(source, Message.SPAWNED, *mod.texts.boss(mod.texts.languageOf(source), boss))
  }

  /** Two blocks ahead of a player, or `x y z [world]` for anyone; a world is a dimension, `overworld`, or Paper's `world`. */
  private fun where(mod: MoBosses, source: CommandSourceStack, args: List<String>): Pair<ServerLevel, Vec3>? {
    val player = source.player
    if (args.isEmpty()) {
      player ?: return null
      val ahead = player.lookAngle.multiply(1.0, 0.0, 1.0).normalize().scale(2.0)
      return player.level() to player.position().add(ahead)
    }
    val (x, y, z) = args.take(3).map { it.toDoubleOrNull() ?: return null }
    val world = args.getOrNull(3)?.let { level(mod, it) ?: return null } ?: player?.level() ?: mod.server.overworld()
    return world to Vec3(x, y, z)
  }

  private fun level(mod: MoBosses, name: String): ServerLevel? {
    val id = when (name.lowercase()) {
      "world" -> "minecraft:overworld"
      "world_nether" -> "minecraft:the_nether"
      "world_the_end" -> "minecraft:the_end"
      else -> name
    }
    return Identifier.tryParse(id)?.let { mod.server.getLevel(ResourceKey.create(Registries.DIMENSION, it)) }
  }

  private fun usage(mod: MoBosses, source: CommandSourceStack) = mod.texts.send(
    source, Message.SPAWN_USAGE, Placeholder.unparsed("entities", typeKeys(mod).joinToString(", ")),
    Placeholder.unparsed("classes", mod.catalog.classes.all.joinToString(", ") { it.key }),
  )

  private fun typeKeys(mod: MoBosses) = mod.catalog.types.values.map { it.key }.sorted()

  private fun suggest(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
    val mod = Mod.current ?: return builder.buildFuture()
    val source = context.source
    val typed = builder.remaining
    val args = typed.split(' ')
    val last = args.last()
    val player = source.player
    val names = { mod.server.playerList.players.map { it.plainTextName } }
    val options = when {
      args.size <= 1 -> SUBCOMMANDS.filterValues { Perms.check(source, it) }.keys.toList()
      args.size == 2 && args[0].equals("info", true) && Perms.check(source, Permissions.INFO_OTHERS) -> names()
      args.size == 2 && args[0].equals("language", true) && player != null -> mod.playerLanguages.codes() + LanguageChoices.AUTO
      args.size == 2 && args[0].equals("reset", true) -> listOf(CONFIRM) + if (Perms.check(source, Permissions.RESET_OTHERS)) names() else emptyList()
      !args[0].equals("spawn", true) || !Perms.check(source, Permissions.SPAWN) -> emptyList()
      args.size == 2 -> typeKeys(mod)
      args.size == 3 -> listOf("1", "10", "25", "50", "75", "100")
      args.size == 4 -> mod.catalog.classes.all.map { it.key }
      args.size in 5..7 -> player?.let { target(it) }?.let { pos -> listOf(pos.x, pos.y + 1, pos.z)[args.size - 5].toString() }?.let(::listOf).orEmpty()
      args.size == 8 -> mod.server.levelKeys().map { it.identifier().toString() }
      else -> emptyList()
    }
    val start = builder.start + typed.length - last.length
    val at = builder.createOffset(start)
    options.filter { it.startsWith(last, ignoreCase = true) }.forEach(at::suggest)
    return at.buildFuture()
  }

  /** The block the player looks at, within 16 blocks. */
  private fun target(player: ServerPlayer): net.minecraft.core.BlockPos? {
    val hit = player.level().clip(ClipContext(player.eyePosition, player.eyePosition.add(player.lookAngle.scale(16.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
    return (hit as? BlockHitResult)?.takeIf { it.type == HitResult.Type.BLOCK }?.blockPos
  }
}
