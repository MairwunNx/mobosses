package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.progress.BossEvent
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Dying to a boss, by its blow, its shot, or its ability, burns the progress toward the next level, never the level
 * itself, as on Paper; from `death_demotion` deaths in a row, with no kill that counts between, each one costs a level
 * too; every death counts, penalty or not. The game's damage source names the boss behind a shot or a fang, so nothing
 * is left to guess.
 */
class DeathPenalty(private val mod: MoBosses) {
  fun died(player: ServerPlayer, source: DamageSource) {
    val boss = source.entity?.let(mod.bosses::get) ?: return
    mod.achievements.diedTo(player, boss)
    mod.progression.count(boss.type.key, boss.clazz.key, BossEvent.KILLED_A_PLAYER)
    val levelBefore = mod.progression.level(player.uuid)
    mod.progression.died(player.uuid, player.plainTextName) { death ->
      mod.worldApi.diedTo(player, boss, death)
      if (death.demoted) mod.worldApi.demoted(player, levelBefore, mod.progression.level(player.uuid), death.after.streak)
      // A player who respawned is a new entity by now: whoever plays under that name hears it.
      val now = mod.server.playerList.getPlayer(player.uuid) ?: return@died
      if (death.lost <= 0) return@died
      val level = mod.progression.level(now.uuid)
      val resolvers = mod.texts.boss(mod.texts.languageOf(now), boss)
      val lost = Placeholder.unparsed("lost", death.lost.toString())
      if (death.demoted) mod.texts.send(now, Message.DEATH_DEMOTION, lost, Placeholder.unparsed("new_level", level.toString()),
        Placeholder.unparsed("deaths", death.after.streak.toString()), *resolvers)
      else mod.texts.send(now, Message.DEATH_PENALTY, lost, Placeholder.unparsed("next", (level + 1).toString()), *resolvers)
    }
  }
}
