package ru.mairwunnx.mobosses.ability

import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.kinds.ArrowArc
import ru.mairwunnx.mobosses.ability.kinds.ArrowRain
import ru.mairwunnx.mobosses.ability.kinds.Enrage
import ru.mairwunnx.mobosses.ability.kinds.FireRing
import ru.mairwunnx.mobosses.ability.kinds.FrostNova
import ru.mairwunnx.mobosses.ability.kinds.Grapple
import ru.mairwunnx.mobosses.ability.kinds.Lightning
import ru.mairwunnx.mobosses.ability.kinds.MeteorShower
import ru.mairwunnx.mobosses.ability.kinds.Regeneration
import ru.mairwunnx.mobosses.ability.kinds.Shockwave
import ru.mairwunnx.mobosses.ability.kinds.Summoning
import ru.mairwunnx.mobosses.ability.kinds.TeleportStrike
import ru.mairwunnx.mobosses.ability.kinds.WebRing
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.boss.Boss
import kotlin.random.Random

/**
 * Picks abilities. Each time a player hits a boss, the boss may cast one it has reached the level
 * for, at random, once both the shared cooldown and that ability's own have passed; the cooldowns
 * live on the boss, so a restart doesn't hand it a free volley. Enrage isn't picked: it answers the
 * boss's health dropping, once.
 */
class Abilities(private val plugin: MoBossesPlugin) {
  fun onHit(boss: Boss, attacker: Player) {
    val catalog = plugin.catalog
    if (!catalog.settings.enabled) return
    val settings = catalog.abilities
    val cast = Cast(plugin, boss, attacker)
    if (Enrage.enabled(settings) && boss.level >= Enrage.threshold(boss.clazz.abilityThresholds) && Enrage.usable(cast) && Enrage.cast(cast)) announce(Enrage, cast)

    val now = System.currentTimeMillis()
    if (now - boss.lastUsed(Boss.SHARED) <= Rolls.biased(settings.generalCooldown.toDouble(), settings.generalCooldownBias, Random)) return
    val ready = PICKED.filter { ability ->
      ability.enabled(settings) && boss.level >= ability.threshold(boss.clazz.abilityThresholds) &&
        ability.cooldown(settings).let { (cooldown, bias) -> now - boss.lastUsed(ability.key) > Rolls.biased(cooldown.toDouble(), bias, Random) } &&
        ability.usable(cast)
    }
    val ability = ready.randomOrNull() ?: return
    if (!ability.cast(cast)) return
    boss.used(ability.key, now)
    announce(ability, cast)
  }

  private fun announce(ability: Ability, cast: Cast) {
    plugin.log.debug { "${cast.boss.type.key} (${cast.boss.uuid}) cast ${ability.key}" }
    if (!plugin.catalog.settings.broadcast.abilities) return
    val code = plugin.texts.languageOf(cast.attacker)
    plugin.texts.send(cast.attacker, ability.message, *plugin.texts.boss(code, cast.boss))
  }

  companion object {
    /** The abilities a hit may set off, at random. */
    val PICKED: List<Ability> = listOf(
      Lightning, Summoning, Regeneration, FireRing, WebRing, TeleportStrike, MeteorShower, ArrowArc, ArrowRain,
      Shockwave, FrostNova, Grapple,
    )
  }
}
