package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.lang.LevelProgress
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.progress.BossCount
import ru.mairwunnx.mobosses.progress.Hunter
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** What `/mobosses info`, `/mobosses top`, and the menu show, gathered off the server thread from the database. */
class Stats(private val plugin: MoBossesPlugin) {
  class Card(val name: String, val progress: Progress, val level: Int, val rank: Int?, val nextBossInSeconds: Long, val survivalMinutes: Int)

  /** A player's card: the cache for players online, the database for anyone else. */
  fun card(player: OfflinePlayer): CompletableFuture<Card> {
    val id = player.uniqueId
    val known = plugin.progression.known(id)
    val progress = if (known != null) CompletableFuture.completedFuture(known) else plugin.progression.load(id)
    // Read on the server thread, where the player's data lives.
    val until = player.player?.persistentDataContainer?.get(plugin.keys.nextBoss, PersistentDataType.LONG) ?: 0L
    val survival = player.player?.let(plugin.survival::minutes) ?: 0
    return progress.thenCombine(plugin.progression.rank(id)) { p, rank ->
      val level = Leveling.playerLevel(plugin.catalog.settings.progression.levelCurve, p.experience)
      Card(player.name ?: id.toString().take(8), p, level, rank, ((until - System.currentTimeMillis()) / 1000).coerceAtLeast(0), survival)
    }
  }

  fun top(limit: Int = 10): CompletableFuture<List<Hunter>> = plugin.progression.top(limit)

  fun counts(): CompletableFuture<List<BossCount>> = plugin.progression.counts()

  /** A hunter's name: the one saved with their last kill, or what the server knows. */
  fun name(hunter: Hunter): String = hunter.name ?: runCatching { Bukkit.getOfflinePlayer(UUID.fromString(hunter.uuid)).name }.getOrNull() ?: hunter.uuid.take(8)

  fun level(progress: Progress) = Leveling.playerLevel(plugin.catalog.settings.progression.levelCurve, progress.experience)

  /** `<player>`, `<level>`, `<max>`, `<rank>`, `<experience>`, `<next_level>`, `<kills>`, `<cooldown>`, and `<bonus>`, `<hours>`, `<minutes>` of the survival bonus for a card. */
  fun resolvers(code: String, card: Card): Array<TagResolver> = arrayOf(
    Placeholder.unparsed("player", card.name),
    Placeholder.unparsed("level", card.level.toString()),
    Placeholder.unparsed("max", plugin.catalog.settings.progression.levelCurve.maxLevel.toString()),
    Placeholder.unparsed("rank", card.rank?.toString() ?: "—"),
    Placeholder.unparsed("experience", LevelProgress.grouped(card.progress.experience)),
    LevelProgress.resolver(plugin.languages, code, plugin.catalog.settings.progression.levelCurve, card.progress.experience),
    Placeholder.unparsed("kills", card.progress.kills.toString()),
    Placeholder.unparsed("bonus", plugin.survival.percent(plugin.catalog.settings.survivalBonus, card.survivalMinutes).toString()),
    *plugin.survival.time(card.survivalMinutes),
    Placeholder.component("cooldown", if (card.nextBossInSeconds <= 0) plugin.languages.render(code, Message.INFO_READY) ?: Component.empty() else Component.text("${card.nextBossInSeconds / 60}:${"%02d".format(card.nextBossInSeconds % 60)}")),
  )
}
