package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.CaveSpider
import org.bukkit.entity.Player
import org.bukkit.entity.Spider
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/** What abilities exist, when a class gets each, and how long each waits before it comes again. */
enum class Ability(
  val key: String,
  val message: Message,
  val enabled: (AbilitySettings) -> Boolean,
  val threshold: (AbilityThresholds) -> Int,
  val cooldown: (AbilitySettings) -> Pair<Long, Double>,
) {
  LIGHTNING("lightning", Message.LIGHTNING, { it.lightningEnabled }, { it.lightning }, { it.lightningCooldown to it.lightningCooldownBias }),
  SUMMONING("summoning", Message.SUMMONING, { it.summoningEnabled }, { it.summoning }, { it.summoningCooldown to it.summoningCooldownBias }),
  REGENERATION("regeneration", Message.REGENERATION, { it.regenerationEnabled }, { it.regeneration }, { it.regenerationCooldown to it.regenerationCooldownBias }),
  FIRE_RING("fire_ring", Message.FIRE_RING, { it.fireRingEnabled }, { it.fireRing }, { it.fireRingCooldown to it.fireRingCooldownBias }),
  WEB_RING("web_ring", Message.WEB_RING, { it.webRingEnabled }, { it.webRing }, { it.webRingCooldown to it.webRingCooldownBias }),
  TELEPORT_STRIKE("teleport_strike", Message.TELEPORT_STRIKE, { it.teleportStrikeEnabled }, { it.teleportStrike }, { it.teleportStrikeCooldown to it.teleportStrikeCooldownBias }),
  METEOR_SHOWER("meteor_shower", Message.METEOR_SHOWER, { it.meteorShowerEnabled }, { it.meteorShower }, { it.meteorShowerCooldown to it.meteorShowerCooldownBias }),
  ARROW_ARC("arrow_arc", Message.ARROW_ARC, { it.arrowArcEnabled }, { it.arrowArc }, { it.arrowArcCooldown to it.arrowArcCooldownBias }),
  ARROW_RAIN("arrow_rain", Message.ARROW_RAIN, { it.arrowRainEnabled }, { it.arrowRain }, { it.arrowRainCooldown to it.arrowRainCooldownBias }),
}

/**
 * Picks and casts abilities. Each time a player hits a boss, the boss may cast one it has reached
 * the level for, at random, once both the shared cooldown and that ability's cooldown have passed.
 */
class Abilities(private val plugin: MoBossesPlugin) {
  private val casts = Casts(plugin)

  fun onHit(boss: Boss, attacker: Player) {
    if (!plugin.catalog.settings.enabled) return
    val settings = plugin.catalog.abilities
    val now = System.currentTimeMillis()
    if (now - boss.lastAbility <= Formulas.biased(settings.generalCooldown.toDouble(), settings.generalCooldownBias, Random)) return

    val ready = Ability.entries.filter { ability ->
      ability.enabled(settings) &&
        boss.level >= ability.threshold(boss.clazz.abilityThresholds) &&
        // Spiders climb; teleporting one behind a player made no sense in 1.x either.
        !(ability == Ability.TELEPORT_STRIKE && (boss.entity is Spider || boss.entity is CaveSpider)) &&
        ability.cooldown(settings).let { (cooldown, bias) -> now - (boss.cooldowns[ability.key] ?: 0L) > Formulas.biased(cooldown.toDouble(), bias, Random) }
    }
    val ability = ready.randomOrNull() ?: return
    if (!casts.cast(ability, boss, attacker)) return

    boss.cooldowns[ability.key] = now
    boss.lastAbility = now
    plugin.log.debug { "${boss.type.key} (${boss.uuid}) cast ${ability.key}" }
    if (plugin.catalog.settings.broadcast.abilities) {
      val code = plugin.texts.languageOf(attacker)
      plugin.texts.send(attacker, ability.message, *plugin.texts.boss(code, boss))
    }
  }
}
