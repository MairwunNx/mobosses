package ru.mairwunnx.mobosses.spawn

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.roundToInt

/**
 * The survival bonus, as in Sacred: each minute a player spends in the game, playing, not idle, and
 * not dying, raises the chance that bosses spawn near them, fast at first and then slower, up to
 * `max_bonus`. Death takes it all. The minutes live on the player, so a relog or a restart keeps them.
 */
class SurvivalBonus(private val plugin: MoBossesPlugin) : Listener {
  /** Every minute: a minute more for each player who's playing. */
  fun tick() {
    val cfg = plugin.catalog.settings.survivalBonus
    if (!cfg.enabled) return
    Bukkit.getOnlinePlayers().forEach { player ->
      if (player.isDead || player.gameMode !in PLAYING || player.idleDuration.toMinutes() >= cfg.idleMinutes) return@forEach
      player.persistentDataContainer.set(plugin.keys.survival, PersistentDataType.INTEGER, minutes(player) + 1)
    }
  }

  fun minutes(player: Player): Int = player.persistentDataContainer.get(plugin.keys.survival, PersistentDataType.INTEGER) ?: 0

  /** What the bonus multiplies boss spawn chances near [player] by: 1 without it. */
  fun multiplier(player: Player): Double = Hunt.multiplier(plugin.catalog.settings.survivalBonus, minutes(player))

  @EventHandler(priority = EventPriority.MONITOR)
  fun onDeath(event: PlayerDeathEvent) {
    val player = event.player
    val minutes = minutes(player)
    if (minutes <= 0) return
    player.persistentDataContainer.remove(plugin.keys.survival)
    val cfg = plugin.catalog.settings.survivalBonus
    if (cfg.enabled && bonus(cfg, minutes) >= 0.01) {
      plugin.texts.send(player, Message.SURVIVAL_LOST, Placeholder.unparsed("bonus", percent(cfg, minutes).toString()), *time(minutes))
    }
  }

  fun percent(cfg: Settings.SurvivalBonus, minutes: Int) = (bonus(cfg, minutes) * 100).roundToInt()

  /** `<hours>` and `<minutes>` of a stretch of minutes. */
  fun time(minutes: Int) = arrayOf(Placeholder.unparsed("hours", (minutes / 60).toString()), Placeholder.unparsed("minutes", (minutes % 60).toString()))

  companion object {
    private val PLAYING = setOf(GameMode.SURVIVAL, GameMode.ADVENTURE)

    fun bonus(cfg: Settings.SurvivalBonus, minutes: Int) = Hunt.bonus(cfg, minutes)
  }
}
