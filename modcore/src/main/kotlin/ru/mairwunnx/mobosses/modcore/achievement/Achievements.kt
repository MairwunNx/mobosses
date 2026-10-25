package ru.mairwunnx.mobosses.modcore.achievement

import com.mojang.serialization.JsonOps
import net.minecraft.advancements.Advancement as GameAdvancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.HolderLookup
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.achievement.Advancement
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.progress.BestiaryEntry

/**
 * Mo’Bosses' advancements, in a tab of their own, as on Paper: the game shows them with its toasts and chat lines, and
 * keeps each player's progress in its own files. They join the game's list each time it builds one, at the start and
 * after `/reload` ([build]), in `achievements.language` for everyone. Each kill also meets `mobosses:boss_killed`
 * ([BossKilled]) for a server's own advancements.
 */
class Achievements(private val mod: MoBosses) {
  private val enabled get() = mod.catalog.settings.achievements.enabled

  fun grant(player: ServerPlayer, achievement: Achievement) {
    if (!enabled) return
    val advancement = mod.server.advancements.get(id(achievement)) ?: return
    if (player.advancements.getOrStartProgress(advancement).isDone) return
    player.advancements.award(advancement, Advancement.CRITERION)
    mod.worldApi.achievement(player, achievement.id)
  }

  /** Level and kill milestones, from the player's progress now: after a kill, and at login for what came before. */
  fun progress(player: ServerPlayer) {
    val progress = mod.progression.of(player.uuid)
    if (progress.kills > 0) {
      grant(player, Achievement.HUNT_BEGINS)
      grant(player, Achievement.FIRST_BLOOD)
    }
    val level = mod.progression.level(player.uuid)
    Achievement.LEVELS.filter { (at, _) -> level >= at }.forEach { (_, it) -> grant(player, it) }
    Achievement.KILLS.filter { (at, _) -> progress.kills >= at }.forEach { (_, it) -> grant(player, it) }
  }

  /** A player's part in a kill: [took] when their share was enough for the kill to be theirs. */
  fun killed(player: ServerPlayer, boss: Boss, took: Boolean, counted: Boolean, alone: Boolean) {
    if (!took) return
    BossKilled.TRIGGER.trigger(player, boss.type.key, boss.clazz.key, boss.level)
    if (!enabled) return
    if (counted) grant(player, Achievement.FIRST_BLOOD)
    Achievement.CLASSES[boss.clazz.key]?.let { grant(player, it) }
    if (boss.phaseTwo) grant(player, Achievement.SECOND_LIFE)
    if (alone && boss.level >= 50) grant(player, Achievement.LONE_WOLF)
    if (boss.level >= 30 && player.uuid.toString() !in hurt(boss)) grant(player, Achievement.UNTOUCHED)
    if ((boss.entity.level() as? ServerLevel)?.let { mod.fullMoon.isUp(it, boss.entity.position()) } == true) grant(player, Achievement.FULL_MOON_HUNTER)
    if (mod.decoys.all.any { it.isAlive && mod.decoys.bossOf(it)?.uuid == boss.uuid }) grant(player, Achievement.FIND_THE_REAL)
    val data = Data(player)
    val avenge = data.string(Keys.AVENGE)?.split('|')
    if (avenge != null && avenge[0] == boss.type.key && mod.now() - (avenge.getOrNull(1)?.toLongOrNull() ?: 0L) in 0..AVENGE_MILLIS) {
      grant(player, Achievement.AVENGER)
      data.remove(Keys.AVENGE)
    }
    progress(player)
  }

  /** The bestiary after a kill: every boss type, and every one as a Mythic. */
  fun bestiary(player: ServerPlayer, entries: List<BestiaryEntry>) {
    if (!enabled || entries.isEmpty()) return
    val types = mod.catalog.types.values.filter { it.spec.enabled }.map { it.key }.toSet()
    if (types.isEmpty()) return
    if (entries.mapTo(HashSet()) { it.type }.containsAll(types)) grant(player, Achievement.BESTIARY)
    if (entries.filter { it.clazz == "mythic" }.mapTo(HashSet()) { it.type }.containsAll(types)) grant(player, Achievement.MYTHIC_COLLECTOR)
  }

  /** A player who died to a boss may avenge it on one of its type for a while. */
  fun diedTo(player: ServerPlayer, boss: Boss) {
    Data(player)[Keys.AVENGE] = "${boss.type.key}|${mod.now()}"
  }

  fun joined(player: ServerPlayer) {
    progress(player)
    mod.progression.bestiary(player.uuid).thenAccept { entries -> mod.server.execute { if (!player.hasDisconnected()) bestiary(player, entries) } }
  }

  /** A boss hurt a player, by its blows, its shots, or its abilities: no “untouched” for them. */
  fun hurt(player: ServerPlayer, source: DamageSource) {
    val boss = source.entity?.let(mod.bosses::get) ?: return
    val hurt = hurt(boss)
    if (player.uuid.toString() !in hurt) Data(boss.entity)[Keys.HURT] = (hurt + player.uuid.toString()).joinToString(";")
  }

  private fun hurt(boss: Boss): Set<String> = Data(boss.entity).string(Keys.HURT).orEmpty().split(';').filter { it.isNotBlank() }.toSet()

  companion object {
    /** How long after dying to a boss a player may avenge it. */
    private const val AVENGE_MILLIS = 10 * 60_000L

    fun id(achievement: Achievement): Identifier = Identifier.fromNamespaceAndPath("mobosses", achievement.id)

    /**
     * The advancements for the game's list, as it builds one: in the language of the server that runs, or, at the
     * start, before Mo’Bosses has read its files, in the one the config names.
     */
    fun build(registries: HolderLookup.Provider): List<AdvancementHolder> {
      val (enabled, code, languages) = Mod.current?.let { Triple(it.catalog.settings.achievements.enabled, it.catalog.settings.achievements.language.ifBlank { it.languages.default }, it.languages) }
        ?: MoBosses.readLanguages(Mod.loader) ?: return emptyList()
      if (!enabled) return emptyList()
      val ops = registries.createSerializationContext(JsonOps.INSTANCE)
      return Achievement.entries.mapNotNull { achievement ->
        val json = Advancement.json(achievement, code, languages, { id(it).toString() })
        GameAdvancement.CODEC.parse(ops, json).resultOrPartial { Mod.log.warn("Achievement ${achievement.id}: $it") }.orElse(null)?.let { AdvancementHolder(id(achievement), it) }
      }
    }
  }
}
