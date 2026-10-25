package ru.mairwunnx.mobosses.modcore.client

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.client.Minecraft
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.lang.LanguageFiles
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.text.Chat
import net.minecraft.network.chat.Component as Vanilla

/**
 * Mo’Bosses' own texts in a player's game, for its screens: from `config/mobosses/lang/`, as the server reads them,
 * in the game's language when Mo’Bosses speaks it, else English.
 */
object ClientTexts {
  private val quiet = Log(object : Log.Sink {
    override fun info(message: String) {}
    override fun warn(message: String) {}
    override fun error(message: String, cause: Throwable?) {}
  }) { false }

  private val languages by lazy {
    val folder = Mod.loader.config.resolve("mobosses").toFile()
    Languages(LanguageFiles.load(folder.resolve("lang"), MoBosses::bundled, quiet) {}, Languages.FALLBACK)
  }

  private val registries: RegistryAccess get() = Minecraft.getInstance().level?.registryAccess() ?: RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

  private fun code(): String {
    val game = LocaleResolver.normalize(Minecraft.getInstance().options.languageCode)
    return listOf(game, game.substringBefore('_')).firstOrNull { it in languages.codes } ?: Languages.FALLBACK
  }

  fun text(message: Message, vararg resolvers: TagResolver): Vanilla =
    Chat.vanilla(languages.render(code(), message, *resolvers) ?: Component.text(message.key), registries)

  fun plain(message: Message, vararg resolvers: TagResolver): String = text(message, *resolvers).string
}
