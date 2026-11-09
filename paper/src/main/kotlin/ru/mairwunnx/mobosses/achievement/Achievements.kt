package ru.mairwunnx.mobosses.achievement

import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.progress.BestiaryEntry

/**
 * Mo’Bosses' advancements: the game shows them in a tab of their own, with its toasts and chat lines.
 * They load when the plugin starts and again after `/minecraft:reload`, which drops them; the game
 * keeps each player's progress in its own files. The text comes in one language for everyone, as the
 * game shows every player the same advancements.
 */
class Achievements(private val plugin: MoBossesPlugin) : Listener {
  private val enabled get() = plugin.catalog.settings.achievements.enabled

  fun key(achievement: Achievement) = NamespacedKey(plugin, achievement.id)

  /** Loads the advancements the server doesn't have yet. Ones already loaded stay, so nobody's progress shifts mid-session. */
  @Suppress("DEPRECATION")
  fun load() {
    if (!enabled) return
    val code = plugin.catalog.settings.achievements.language.ifBlank { plugin.languages.default }
    val missing = Achievement.entries.filter { Bukkit.getAdvancement(key(it)) == null }
    if (missing.isEmpty()) return
    val jsons = missing.associate { (key(it) as Key) to json(it, code) }
    runCatching { Bukkit.getUnsafe().loadAdvancements(jsons, true) }
      .onSuccess { plugin.log.info("Achievements: ${it.size} advancements in their own tab, in $code") }
      .onFailure { plugin.log.error("Can't load the achievements: ${it.message}") }
  }

  @EventHandler
  fun onResourcesReloaded(event: ServerResourcesReloadedEvent) = load()

  fun grant(player: Player, achievement: Achievement) {
    if (!enabled) return
    val advancement = Bukkit.getAdvancement(key(achievement)) ?: return
    val progress = player.getAdvancementProgress(advancement)
    if (progress.isDone) return
    progress.awardCriteria(CRITERION)
    plugin.worldApi.achievement(player, achievement.id)
  }

  /** Level and kill milestones, from the player's progress now: after a kill, and at login for what came before. */
  fun progress(player: Player) {
    val progress = plugin.progression.of(player.uniqueId)
    if (progress.kills > 0) {
      grant(player, Achievement.HUNT_BEGINS)
      grant(player, Achievement.FIRST_BLOOD)
    }
    val level = plugin.progression.level(player.uniqueId)
    Achievement.LEVELS.filter { (at, _) -> level >= at }.forEach { (_, it) -> grant(player, it) }
    kills(player, progress.kills)
  }

  /** The kill tiers for a player's [kills], each showing how many of its goal are in. */
  fun kills(player: Player, kills: Int) = Achievement.KILLS.forEach { (goal, it) -> count(player, it, goal, kills) }

  /**
   * Ticks off a kill tier's kills up to [kills], which the game shows as N of [goal]. At the goal one criterion meets
   * them all, and the kills' own go, so a player's advancement file keeps one line for the tier, not a thousand.
   */
  private fun count(player: Player, achievement: Achievement, goal: Int, kills: Int) {
    if (!enabled) return
    val advancement = Bukkit.getAdvancement(key(achievement)) ?: return
    val progress = player.getAdvancementProgress(advancement)
    if (progress.isDone) return
    Advancement.counted(goal, kills, progress.remainingCriteria).forEach(progress::awardCriteria)
    if (!progress.isDone) return
    if (CRITERION in progress.awardedCriteria) progress.awardedCriteria.filter { it != CRITERION }.forEach(progress::revokeCriteria)
    plugin.worldApi.achievement(player, achievement.id)
  }

  /** A player's part in a kill: [took] when their share was enough for the kill to be theirs. */
  fun killed(player: Player, boss: Boss, took: Boolean, counted: Boolean, alone: Boolean) {
    if (!enabled || !took) return
    if (counted) grant(player, Achievement.FIRST_BLOOD)
    Achievement.CLASSES[boss.clazz.key]?.let { grant(player, it) }
    if (boss.phaseTwo) grant(player, Achievement.SECOND_LIFE)
    if (alone && boss.level >= 50) grant(player, Achievement.LONE_WOLF)
    if (boss.level >= 30 && player.uniqueId.toString() !in hurt(boss)) grant(player, Achievement.UNTOUCHED)
    if (plugin.fullMoon.isUp(boss.entity.world)) grant(player, Achievement.FULL_MOON_HUNTER)
    if (plugin.decoys.all.any { it.isValid && plugin.decoys.bossOf(it)?.uuid == boss.uuid }) grant(player, Achievement.FIND_THE_REAL)
    val avenge = player.persistentDataContainer.get(plugin.keys.avenge, PersistentDataType.STRING)?.split('|')
    if (avenge != null && avenge[0] == boss.type.key && System.currentTimeMillis() - (avenge.getOrNull(1)?.toLongOrNull() ?: 0L) <= AVENGE_MILLIS) {
      grant(player, Achievement.AVENGER)
      player.persistentDataContainer.remove(plugin.keys.avenge)
    }
    progress(player)
  }

  /** The bestiary after a kill and at login: every boss type, and every one as a Mythic, one by one. */
  fun bestiary(player: Player, entries: List<BestiaryEntry>) {
    if (!enabled || entries.isEmpty()) return
    val types = enabledTypes()
    if (types.isEmpty()) return
    collect(player, Achievement.BESTIARY, entries.mapTo(HashSet()) { it.type }, types)
    collect(player, Achievement.MYTHIC_COLLECTOR, entries.filter { it.clazz == "mythic" }.mapTo(HashSet()) { it.type }, types)
  }

  /** Ticks off each boss type [killed] on [achievement]'s list, which the game shows as N of M. */
  private fun collect(player: Player, achievement: Achievement, killed: Set<String>, types: Set<String>) {
    val advancement = Bukkit.getAdvancement(key(achievement)) ?: return
    val progress = player.getAdvancementProgress(advancement)
    if (progress.isDone) return
    Advancement.collected(progress.remainingCriteria.toList(), killed, types).forEach(progress::awardCriteria)
    if (progress.isDone) plugin.worldApi.achievement(player, achievement.id)
  }

  /** A player who died to a boss may avenge it on one of its type for a while. */
  fun diedTo(player: Player, boss: Boss) =
    player.persistentDataContainer.set(plugin.keys.avenge, PersistentDataType.STRING, "${boss.type.key}|${System.currentTimeMillis()}")

  @EventHandler(priority = EventPriority.MONITOR)
  fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    progress(player)
    plugin.progression.bestiary(player.uniqueId).thenAccept { entries ->
      Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) bestiary(player, entries) })
    }
  }

  /** Who a boss hurt, by its blows, its shots, and its abilities, for “untouched”. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onHurt(event: EntityDamageByEntityEvent) {
    val player = event.entity as? Player ?: return
    val by = event.damager
    val source: Entity? = (by as? Projectile)?.shooter as? Entity ?: (by as? EvokerFangs)?.owner ?: by
    val boss = source?.let(plugin.bosses::get) ?: return
    val data = boss.entity.persistentDataContainer
    val hurt = hurt(boss)
    if (player.uniqueId.toString() !in hurt) data.set(plugin.keys.hurt, PersistentDataType.STRING, (hurt + player.uniqueId.toString()).joinToString(";"))
  }

  private fun hurt(boss: Boss): Set<String> =
    boss.entity.persistentDataContainer.get(plugin.keys.hurt, PersistentDataType.STRING).orEmpty().split(';').filter { it.isNotBlank() }.toSet()

  private fun json(achievement: Achievement, code: String): String =
    Advancement.json(achievement, code, plugin.languages, { key(it).toString() }, requirements = Advancement.requirements(achievement, enabledTypes())).toString()

  private fun enabledTypes() = plugin.catalog.types.values.filter { it.spec.enabled }.map { it.key }.toSet()

  private companion object {
    const val CRITERION = Advancement.CRITERION
    /** How long after dying to a boss a player may avenge it. */
    const val AVENGE_MILLIS = 10 * 60_000L
  }
}
