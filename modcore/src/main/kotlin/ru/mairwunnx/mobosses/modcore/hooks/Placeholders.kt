package ru.mairwunnx.mobosses.modcore.hooks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.text.Chat
import java.util.UUID

/**
 * Text Placeholder API both ways, when it's there, as PlaceholderAPI on Paper. `<papi:name>` in a message asks for one
 * placeholder for the player who reads it: a tag, never `%…%` replaced over a whole text, so a percent sign or a
 * player's words stay as they are. The name is Text Placeholder API's, like `player:name`; a PlaceholderAPI-style one
 * without a colon reads its first `_` as the colon, so `player_name` asks for `player:name`. And `%mobosses:level%`,
 * `%mobosses:xp%`, and `%mobosses:kills%` tell other mods (TAB, scoreboards, Styled Chat) about the hunt.
 */
object Placeholders {
  /** Patbox's on Fabric, its port on NeoForge (`placeholder_api_neoforge`); both keep the API's classes. */
  private val available: Boolean by lazy { Mod.loader.loaded("placeholder-api") || Mod.loader.loaded("placeholder_api_neoforge") }
  private var registered = false

  fun resolver(mod: MoBosses, player: ServerPlayer?): TagResolver = TagResolver.resolver("papi") { args, _ ->
    val name = args.popOr("<papi> needs a placeholder name, like <papi:player:name>").value()
    // Only touch Text Placeholder API's classes when it's there; they aren't on the classpath otherwise.
    val text = if (available && player != null) Api.resolve(player, id(name))?.let { Chat.adventure(it, mod.server.registryAccess()) } else null
    Tag.selfClosingInserting(text ?: Component.empty())
  }

  /** Text Placeholder API's id for a name in `<papi:…>`: as written with a colon, else the first `_` becomes one. */
  fun id(name: String): String = if (':' in name) name else name.replaceFirst('_', ':')

  /** Registers `%mobosses:…%` once a game: Text Placeholder API keeps them until the game quits, so they read whichever server runs. */
  fun register(): Boolean {
    if (!available) return false
    if (!registered) Api.register()
    registered = true
    return true
  }

  /** Other mods may ask from their own threads, so this reads only the progress cache, which holds players who are online. */
  fun value(name: String, player: UUID?): String {
    val mod = Mod.current ?: return ""
    val id = player ?: return ""
    return when (name) {
      "level" -> mod.progression.level(id).toString()
      "xp", "experience" -> mod.progression.of(id).experience.toString()
      "kills" -> mod.progression.of(id).kills.toString()
      else -> ""
    }
  }

  // Only touched when the API is loaded.
  private object Api {
    fun resolve(player: ServerPlayer, id: String): net.minecraft.network.chat.Component? {
      val identifier = Identifier.tryParse(id) ?: return null
      val context = eu.pb4.placeholders.api.ServerPlaceholderContext.of(player)
      val result = eu.pb4.placeholders.api.Placeholders.getServerPlaceholder(identifier)?.onPlaceholderRequest(context, null)
        ?: eu.pb4.placeholders.api.Placeholders.getCommonPlaceholder(identifier)?.onPlaceholderRequest(context, null)
        ?: return null
      return if (result.isValid) result.component() else null
    }

    fun register() = listOf("level", "xp", "experience", "kills").forEach { name ->
      eu.pb4.placeholders.api.Placeholders.registerServer<Unit>(Identifier.fromNamespaceAndPath("mobosses", name)) { context, _ ->
        val online = if (context.hasPlayer()) context.player() as? ServerPlayer else null
        val id = online?.uuid ?: if (context.hasGameProfile()) context.gameProfile()?.id else null
        eu.pb4.placeholders.api.PlaceholderResult.value(value(name, id))
      }
    }
  }
}
