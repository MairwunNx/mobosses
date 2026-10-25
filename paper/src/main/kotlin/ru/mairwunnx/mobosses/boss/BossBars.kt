package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * Health bars. A player sees the bar of the boss they hit last, in their own language: each boss
 * has one bar per language among its viewers. The bar goes away when the boss dies or unloads, or
 * when the player leaves, changes worlds, or walks past `boss_bar.hide_distance`.
 */
class BossBars(private val plugin: MoBossesPlugin) {
  private val bars = HashMap<UUID, MutableMap<String, BossBar>>()
  /** Player → the boss whose bar they see. */
  private val viewing = HashMap<UUID, UUID>()

  fun show(player: Player, boss: Boss) {
    if (!plugin.catalog.settings.bossBar.enabled) return
    if (viewing[player.uniqueId] != boss.uuid) hide(player)
    val code = plugin.texts.languageOf(player)
    val bar = bars.getOrPut(boss.uuid) { HashMap() }.getOrPut(code) { create(boss, code) }
    bar.progress(progress(boss))
    player.showBossBar(bar)
    viewing[player.uniqueId] = boss.uuid
  }

  fun hide(player: Player) {
    val boss = viewing.remove(player.uniqueId) ?: return
    bars[boss]?.values?.forEach(player::hideBossBar)
  }

  fun update(boss: Boss) {
    val progress = progress(boss)
    bars[boss.uuid]?.values?.forEach { it.progress(progress) }
  }

  fun remove(boss: Boss) {
    val forBoss = bars.remove(boss.uuid) ?: return
    viewing.entries.removeIf { (player, viewed) ->
      (viewed == boss.uuid).also { if (it) Bukkit.getPlayer(player)?.let { p -> forBoss.values.forEach(p::hideBossBar) } }
    }
  }

  /** Once a second: health, and viewers who walked away. */
  fun tick() {
    bars.keys.mapNotNull(plugin.bosses::get).forEach(::update)
    val distance = plugin.catalog.settings.bossBar.hideDistance.toDouble()
    viewing.entries.toList().forEach { (id, bossId) ->
      val player = Bukkit.getPlayer(id)
      val boss = plugin.bosses.get(bossId)
      if (player == null || boss == null || player.world != boss.entity.world || player.location.distanceSquared(boss.entity.location) > distance * distance) {
        player?.let(::hide) ?: viewing.remove(id)
      }
    }
  }

  fun clear() {
    viewing.keys.toList().mapNotNull(Bukkit::getPlayer).forEach(::hide)
    viewing.clear()
    bars.clear()
  }

  private fun create(boss: Boss, code: String): BossBar {
    val title = plugin.languages.render(code, Message.BOSS_BAR, *plugin.texts.boss(code, boss)) ?: plugin.texts.bossName(code, boss.type)
    return BossBar.bossBar(title, progress(boss), color(boss), overlay())
  }

  private fun progress(boss: Boss): Float {
    val max = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return 1f
    return (boss.entity.health / max).toFloat().coerceIn(0f, 1f)
  }

  /** The bar colors the game has, nearest to the class color, as 1.x picked them. */
  private fun color(boss: Boss): BossBar.Color = when (NamedTextColor.nearestTo(boss.color)) {
    NamedTextColor.BLACK, NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_GRAY -> BossBar.Color.PURPLE
    NamedTextColor.DARK_BLUE, NamedTextColor.DARK_AQUA, NamedTextColor.BLUE, NamedTextColor.AQUA -> BossBar.Color.BLUE
    NamedTextColor.DARK_GREEN, NamedTextColor.GREEN -> BossBar.Color.GREEN
    NamedTextColor.DARK_RED, NamedTextColor.RED -> BossBar.Color.RED
    NamedTextColor.GOLD, NamedTextColor.YELLOW -> BossBar.Color.YELLOW
    NamedTextColor.LIGHT_PURPLE -> BossBar.Color.PINK
    else -> BossBar.Color.WHITE
  }

  private fun overlay(): BossBar.Overlay = when (plugin.catalog.settings.bossBar.style.trim().uppercase()) {
    "SOLID", "PROGRESS" -> BossBar.Overlay.PROGRESS
    "SEGMENTED_6", "NOTCHED_6" -> BossBar.Overlay.NOTCHED_6
    "SEGMENTED_10", "NOTCHED_10" -> BossBar.Overlay.NOTCHED_10
    "SEGMENTED_12", "NOTCHED_12" -> BossBar.Overlay.NOTCHED_12
    else -> BossBar.Overlay.NOTCHED_20
  }
}
