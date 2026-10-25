package ru.mairwunnx.mobosses.modcore.spawn

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Util
import net.minecraft.world.level.GameType
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.spawn.Hunt
import kotlin.math.roundToInt

/**
 * The survival bonus, as in Sacred and on Paper: each minute a player spends in the game, playing, not idle, and not
 * dying, raises the chance that bosses spawn near them, fast at first and then slower, up to `max_bonus`. Death takes
 * it all. The minutes live on the player, so a relog or a restart keeps them.
 */
class SurvivalBonus(private val mod: MoBosses) {
  /** Every minute: a minute more for each player who's playing. */
  fun tick() {
    val cfg = mod.catalog.settings.survivalBonus
    if (!cfg.enabled) return
    mod.server.playerList.players.forEach { player ->
      val idle = (Util.getMillis() - player.lastActionTime) / 60_000L
      if (player.isDeadOrDying || player.gameMode() !in PLAYING || idle >= cfg.idleMinutes) return@forEach
      Data(player)[Keys.SURVIVAL] = minutes(player) + 1
    }
  }

  fun minutes(player: ServerPlayer): Int = Data(player).int(Keys.SURVIVAL) ?: 0

  /** What the bonus multiplies boss spawn chances near [player] by: 1 without it. */
  fun multiplier(player: ServerPlayer): Double = Hunt.multiplier(mod.catalog.settings.survivalBonus, minutes(player))

  fun died(player: ServerPlayer) {
    val minutes = minutes(player)
    if (minutes <= 0) return
    Data(player).remove(Keys.SURVIVAL)
    val cfg = mod.catalog.settings.survivalBonus
    if (cfg.enabled && Hunt.bonus(cfg, minutes) >= 0.01) {
      mod.texts.send(player, Message.SURVIVAL_LOST, Placeholder.unparsed("bonus", percent(cfg, minutes).toString()), *time(minutes))
    }
  }

  fun percent(cfg: Settings.SurvivalBonus, minutes: Int) = (Hunt.bonus(cfg, minutes) * 100).roundToInt()

  /** `<hours>` and `<minutes>` of a stretch of minutes. */
  fun time(minutes: Int) = arrayOf(Placeholder.unparsed("hours", (minutes / 60).toString()), Placeholder.unparsed("minutes", (minutes % 60).toString()))

  private companion object {
    val PLAYING = setOf(GameType.SURVIVAL, GameType.ADVENTURE)
  }
}
