package ru.mairwunnx.mobosses.modcore.boss

import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.boss.WorldBosses
import ru.mairwunnx.mobosses.modcore.MoBosses
import java.util.UUID

/**
 * Health bars, as on Paper. A player sees the bar of the boss they hit last, in their own language: each boss has one
 * bar per language among its viewers. The bar goes away when the boss dies or unloads, or when the player leaves,
 * changes worlds, or walks past `boss_bar.hide_distance`.
 */
class BossBars(private val mod: MoBosses) {
  private val bars = HashMap<UUID, MutableMap<String, ServerBossEvent>>()
  /** Player → the boss whose bar they see. */
  private val viewing = HashMap<UUID, UUID>()

  fun show(player: ServerPlayer, boss: Boss) {
    // The dragon and the wither keep the game's own bar, which shows the boss's name in its own color.
    if (!mod.catalog.settings.bossBar.enabled || WorldBosses.isWorldBoss(boss.type.key)) return
    if (viewing[player.uuid] != boss.uuid) hide(player)
    val code = mod.texts.languageOf(player)
    val bar = bars.getOrPut(boss.uuid) { HashMap() }.getOrPut(code) { create(boss, code) }
    bar.progress = progress(boss)
    bar.addPlayer(player)
    viewing[player.uuid] = boss.uuid
  }

  fun hide(player: ServerPlayer) {
    val boss = viewing.remove(player.uuid) ?: return
    bars[boss]?.values?.forEach { it.removePlayer(player) }
  }

  fun update(boss: Boss) {
    val progress = progress(boss)
    bars[boss.uuid]?.values?.forEach { it.progress = progress }
  }

  fun remove(boss: Boss) {
    val forBoss = bars.remove(boss.uuid) ?: return
    forBoss.values.forEach(ServerBossEvent::removeAllPlayers)
    viewing.entries.removeIf { it.value == boss.uuid }
  }

  /** Once a second: health, and viewers who walked away. */
  fun tick() {
    bars.keys.mapNotNull(mod.bosses::get).forEach(::update)
    val distance = mod.catalog.settings.bossBar.hideDistance.toDouble()
    viewing.entries.toList().forEach { (id, bossId) ->
      val player = mod.server.playerList.getPlayer(id)
      val boss = mod.bosses.get(bossId)
      if (player == null || boss == null || player.level() != boss.entity.level() || player.distanceToSqr(boss.entity) > distance * distance) {
        player?.let(::hide) ?: viewing.remove(id)
      }
    }
  }

  fun clear() {
    bars.values.flatMap { it.values }.forEach(ServerBossEvent::removeAllPlayers)
    viewing.clear()
    bars.clear()
  }

  private fun create(boss: Boss, code: String): ServerBossEvent {
    val title = mod.languages.render(code, Message.BOSS_BAR, *mod.texts.boss(code, boss)) ?: mod.texts.bossName(code, boss.type)
    return ServerBossEvent(UUID.randomUUID(), mod.texts.vanilla(title), color(boss), overlay()).also { it.progress = progress(boss) }
  }

  private fun progress(boss: Boss): Float = (boss.entity.health / boss.entity.maxHealth).coerceIn(0f, 1f)

  /** The bar colors the game has, nearest to the class color, as 1.x picked them. */
  private fun color(boss: Boss): BossEvent.BossBarColor = when (NamedTextColor.nearestTo(boss.color)) {
    NamedTextColor.BLACK, NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_GRAY -> BossEvent.BossBarColor.PURPLE
    NamedTextColor.DARK_BLUE, NamedTextColor.DARK_AQUA, NamedTextColor.BLUE, NamedTextColor.AQUA -> BossEvent.BossBarColor.BLUE
    NamedTextColor.DARK_GREEN, NamedTextColor.GREEN -> BossEvent.BossBarColor.GREEN
    NamedTextColor.DARK_RED, NamedTextColor.RED -> BossEvent.BossBarColor.RED
    NamedTextColor.GOLD, NamedTextColor.YELLOW -> BossEvent.BossBarColor.YELLOW
    NamedTextColor.LIGHT_PURPLE -> BossEvent.BossBarColor.PINK
    else -> BossEvent.BossBarColor.WHITE
  }

  private fun overlay(): BossEvent.BossBarOverlay = when (mod.catalog.settings.bossBar.style.trim().uppercase()) {
    "SOLID", "PROGRESS" -> BossEvent.BossBarOverlay.PROGRESS
    "SEGMENTED_6", "NOTCHED_6" -> BossEvent.BossBarOverlay.NOTCHED_6
    "SEGMENTED_10", "NOTCHED_10" -> BossEvent.BossBarOverlay.NOTCHED_10
    "SEGMENTED_12", "NOTCHED_12" -> BossEvent.BossBarOverlay.NOTCHED_12
    else -> BossEvent.BossBarOverlay.NOTCHED_20
  }
}
