package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.lang.LevelProgress
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.progress.BossCount
import ru.mairwunnx.mobosses.progress.Hunter
import ru.mairwunnx.mobosses.progress.Progress
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** What `/mobosses info`, `/mobosses top`, and the menu show, as on Paper, gathered off the server thread from the database. */
class Stats(private val mod: MoBosses) {
  class Card(val name: String, val progress: Progress, val level: Int, val rank: Int?, val nextBossInSeconds: Long, val survivalMinutes: Int)

  /** A player's card: the cache for players online, the database for anyone else. */
  fun card(id: UUID, name: String): CompletableFuture<Card> {
    val known = mod.progression.known(id)
    val progress = if (known != null) CompletableFuture.completedFuture(known) else mod.progression.load(id)
    // Read on the server thread, where the player's data lives.
    val online = mod.server.playerList.getPlayer(id)
    val until = online?.let { Data(it).long(Keys.NEXT_BOSS) } ?: 0L
    val survival = online?.let(mod.survival::minutes) ?: 0
    val now = mod.now()
    return progress.thenCombine(mod.progression.rank(id)) { p, rank ->
      Card(name, p, level(p), rank, ((until - now) / 1000).coerceAtLeast(0), survival)
    }
  }

  fun top(limit: Int = 10): CompletableFuture<List<Hunter>> = mod.progression.top(limit)

  fun counts(): CompletableFuture<List<BossCount>> = mod.progression.counts()

  fun level(progress: Progress) = Leveling.playerLevel(mod.catalog.settings.progression.levelCurve, progress.experience)

  /** `<player>`, `<level>`, `<max>`, `<rank>`, `<experience>`, `<next_level>`, `<kills>`, `<cooldown>`, and `<bonus>`, `<hours>`, `<minutes>` of the survival bonus for a card. */
  fun resolvers(code: String, card: Card): Array<TagResolver> = arrayOf(
    Placeholder.unparsed("player", card.name),
    Placeholder.unparsed("level", card.level.toString()),
    Placeholder.unparsed("max", mod.catalog.settings.progression.levelCurve.maxLevel.toString()),
    Placeholder.unparsed("rank", card.rank?.toString() ?: "—"),
    Placeholder.unparsed("experience", LevelProgress.grouped(card.progress.experience)),
    LevelProgress.resolver(mod.languages, code, mod.catalog.settings.progression.levelCurve, card.progress.experience),
    Placeholder.unparsed("kills", card.progress.kills.toString()),
    Placeholder.unparsed("bonus", mod.survival.percent(mod.catalog.settings.survivalBonus, card.survivalMinutes).toString()),
    *mod.survival.time(card.survivalMinutes),
    Placeholder.component("cooldown", if (card.nextBossInSeconds <= 0) mod.languages.render(code, Message.INFO_READY) ?: Component.empty() else Component.text("${card.nextBossInSeconds / 60}:${"%02d".format(card.nextBossInSeconds % 60)}")),
  )
}
