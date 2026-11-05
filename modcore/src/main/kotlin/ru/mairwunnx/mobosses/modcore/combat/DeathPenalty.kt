package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/**
 * Dying to a boss, by its blow, its shot, or its ability, burns the progress toward the next level, never the level
 * itself, as on Paper; from `death_demotion` deaths in a row, with no kill that counts between, each one costs a level
 * too. The game's damage source names the boss behind a shot or a fang, so nothing is left to guess.
 */
class DeathPenalty(private val mod: MoBosses) {
  fun died(player: ServerPlayer, source: DamageSource) {
    val boss = source.entity?.let(mod.bosses::get) ?: return
    mod.achievements.diedTo(player, boss)
    val settings = mod.catalog.settings.progression
    if (!settings.deathPenalty) return mod.worldApi.diedTo(player, boss, 0)
    val data = Data(player)
    val deaths = (data.int(Keys.DEATH_STREAK) ?: 0) + 1
    data[Keys.DEATH_STREAK] = deaths
    val demote = settings.deathDemotion > 0 && deaths >= settings.deathDemotion
    mod.progression.penalize(player.uuid, player.plainTextName, demote) { lost ->
      mod.worldApi.diedTo(player, boss, lost)
      // A player who respawned is a new entity by now: whoever plays under that name hears it.
      val now = mod.server.playerList.getPlayer(player.uuid) ?: return@penalize
      val level = mod.progression.level(now.uuid)
      val resolvers = mod.texts.boss(mod.texts.languageOf(now), boss)
      if (demote) mod.texts.send(now, Message.DEATH_DEMOTION, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("level", level.toString()), Placeholder.unparsed("deaths", deaths.toString()), *resolvers)
      else mod.texts.send(now, Message.DEATH_PENALTY, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("next", (level + 1).toString()), *resolvers)
    }
  }

  /** A kill that counts ends [player]'s streak of deaths. */
  fun won(player: ServerPlayer) = Data(player).remove(Keys.DEATH_STREAK)
}
