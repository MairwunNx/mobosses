package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Dying to a boss, by its blow, its shot, or its ability, burns the progress toward the next level, never the level
 * itself, as on Paper. The game's damage source names the boss behind a shot or a fang, so nothing is left to guess.
 */
class DeathPenalty(private val mod: MoBosses) {
  fun died(player: ServerPlayer, source: DamageSource) {
    val boss = source.entity?.let(mod.bosses::get) ?: return
    mod.achievements.diedTo(player, boss)
    if (!mod.catalog.settings.progression.deathPenalty) return mod.worldApi.diedTo(player, boss, 0)
    mod.progression.penalize(player.uuid, player.plainTextName) { lost ->
      mod.worldApi.diedTo(player, boss, lost)
      // A player who respawned is a new entity by now: whoever plays under that name hears it.
      val now = mod.server.playerList.getPlayer(player.uuid) ?: return@penalize
      mod.texts.send(now, Message.DEATH_PENALTY, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("next", (mod.progression.level(now.uuid) + 1).toString()), *mod.texts.boss(mod.texts.languageOf(now), boss))
    }
  }
}
