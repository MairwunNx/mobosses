package ru.mairwunnx.mobosses.ability

import org.bukkit.Bukkit
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
import ru.mairwunnx.mobosses.ability.signature.*
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
    if (now < boss.readyAt(Boss.SHARED)) return
    val ready = PICKED.filter { ability ->
      ability.enabled(settings) && boss.level >= ability.threshold(boss.clazz.abilityThresholds) && now >= boss.readyAt(ability.key) && ability.usable(cast)
    }
    // One that finds nothing to do, like a fire ring over water, gives way to the next.
    val ability = ready.shuffled().firstOrNull { it.cast(cast) } ?: return
    // Each cooldown's random stretch is rolled once, here, so more hits don't mean more chances.
    val (cooldown, bias) = ability.cooldown(settings)
    boss.used(
      ability.key,
      now + Rolls.biased(cooldown.toDouble(), bias, Random).toLong(),
      now + Rolls.biased(settings.generalCooldown.toDouble(), settings.generalCooldownBias, Random).toLong(),
    )
    announce(ability, cast)
  }

  /** Warns everyone in the fight, not only the player whose hit set it off: whoever hit the boss and is still near. */
  private fun announce(ability: Ability, cast: Cast) {
    plugin.log.debug { "${cast.boss.type.key} (${cast.boss.uuid}) cast ${ability.key}" }
    val broadcast = plugin.catalog.settings.broadcast
    if (!broadcast.abilities) return
    val fighters = (cast.boss.damage.keys.mapNotNull(Bukkit::getPlayer) + cast.attacker).distinct()
      .filter { it.world == cast.at.world && it.location.distanceSquared(cast.at) <= FIGHT * FIGHT }
    fighters.forEach { player ->
      val resolvers = plugin.texts.boss(plugin.texts.languageOf(player), cast.boss)
      if (broadcast.abilitiesIn.equals("chat", ignoreCase = true)) plugin.texts.send(player, ability.message, *resolvers)
      else plugin.texts.actionBar(player, ability.message, *resolvers)
    }
  }

  companion object {
    /** How close a player who hit the boss must still be to hear about its abilities. */
    private const val FIGHT = 48.0

    /** The abilities a hit may set off, at random. */
    val PICKED: List<Ability> = listOf(
      Lightning, Summoning, Regeneration, FireRing, WebRing, TeleportStrike, MeteorShower, ArrowArc, ArrowRain,
      Shockwave, FrostNova, Grapple,
      // Each boss type's own, which only its types pass `usable` for.
      PotionVolley, FireballBarrage, AimedVolley, Pounce, Swap, TridentStorm, FangLine, Whirlwind, WitherAura,
      Sandstorm, Infection, WindBurst, WarCry, Terror, Phase, Stampede, Curse, Fuse,
    )
  }
}
