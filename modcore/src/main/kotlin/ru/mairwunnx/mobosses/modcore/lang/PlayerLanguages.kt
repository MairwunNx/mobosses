package ru.mairwunnx.mobosses.modcore.lang

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Each player's language, as on Paper: the one they chose with `/mobosses language`, while its file is there, or else
 * the automatic one, from their country and the game's language setting. A singleplayer or LAN game's address is a
 * local one no country has, so the game's language decides there.
 */
class PlayerLanguages(private val mod: MoBosses) {
  private val countries = ConcurrentHashMap<UUID, String>()
  private val choices by lazy { LanguageChoices(mod.folder.resolve("player-languages.yml")).also { it.load() } }

  /** Off the server thread, as a player joins. */
  fun login(player: UUID, address: InetAddress?) {
    if (address == null || address.isLoopbackAddress || address.isSiteLocalAddress || address.isAnyLocalAddress) return
    mod.geoIp.country(address)?.let { countries[player] = it }
  }

  fun quit(player: UUID) {
    countries.remove(player)
  }

  fun of(player: ServerPlayer): String = chosen(player) ?: automatic(player)

  fun of(source: CommandSourceStack): String = source.player?.let(::of) ?: mod.languages.default

  /** The player's own choice, or null when Mo’Bosses picks. */
  fun chosen(player: ServerPlayer): String? = choices.of(player.uuid)?.takeIf { it in mod.languages.codes }

  /** The language Mo’Bosses picks for a player who chose none. */
  fun automatic(player: ServerPlayer): String = mod.resolver.resolve(countries[player.uuid], locale(player))

  /** The languages to choose from, the bundled ones first. */
  fun codes(): List<String> = mod.languages.codes.sortedWith(compareBy({ it !in Languages.BUNDLED }, { Languages.BUNDLED.indexOf(it) }, { it }))

  /** Sets the player's language, or with null hands it back to Mo’Bosses; they hear about it in the language they get. */
  fun choose(player: ServerPlayer, code: String?) {
    choices.set(player.uuid, code)
    // The bar of the boss they fight would stay in the old language; the next hit shows it in the new one.
    mod.bars.hide(player)
    val name = Placeholder.unparsed("language", LanguageChoices.nativeName(of(player)))
    mod.texts.send(player, if (code == null) Message.LANGUAGE_AUTO else Message.LANGUAGE_SET, name)
  }

  companion object {
    /** The game's language setting, like `pt_br`, as a locale. */
    fun locale(player: ServerPlayer): Locale? = player.clientInformation().language().takeIf { it.isNotBlank() }?.let { Locale.forLanguageTag(it.replace('_', '-')) }

    fun address(player: ServerPlayer): InetAddress? = (player.connection.remoteAddress as? InetSocketAddress)?.address
  }
}
