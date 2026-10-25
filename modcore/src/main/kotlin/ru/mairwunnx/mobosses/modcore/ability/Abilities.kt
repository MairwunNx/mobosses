package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.ability.kinds.Chain
import ru.mairwunnx.mobosses.modcore.ability.kinds.Enrage
import ru.mairwunnx.mobosses.modcore.ability.kinds.Grapple
import ru.mairwunnx.mobosses.modcore.ability.kinds.Lightning
import ru.mairwunnx.mobosses.modcore.ability.kinds.PhaseTwo
import ru.mairwunnx.mobosses.modcore.ability.kinds.TeleportStrike
import ru.mairwunnx.mobosses.modcore.boss.Boss
import kotlin.random.Random

/**
 * Abilities on a hit, as on Paper: a random one among those the boss's class has reached the level for, once the
 * shared cooldown and that ability's own have passed. Enrage and the second phase come on their own, by health.
 */
class Abilities(private val mod: MoBosses) {
  fun onHit(boss: Boss, attacker: ServerPlayer?) {
    val catalog = mod.catalog
    if (!catalog.settings.enabled) return
    val settings = catalog.abilities
    val cast = Cast(mod, boss, attacker)
    // The second phase casts something at once, cooldowns or not.
    val turned = PhaseTwo.due(cast).also { if (it) PhaseTwo.cast(cast).also { announce(PhaseTwo.message, cast) } }
    if (Enrage.enabled(settings) && boss.level >= Enrage.threshold(boss.clazz.abilityThresholds) && Enrage.usable(cast) && Enrage.cast(cast)) announce(Enrage.message, cast)

    val now = mod.now()
    if (!turned && now < boss.readyAt(Boss.SHARED)) return
    val ready = PICKED.filter { ability ->
      ability.enabled(settings) && boss.level >= ability.threshold(boss.clazz.abilityThresholds) && (turned || now >= boss.readyAt(ability.key)) && ability.usable(cast)
    }
    // One that finds nothing to do, like a fire ring over water, gives way to the next.
    val ability = ready.shuffled().firstOrNull { it.cast(cast) } ?: return
    // Each cooldown's random stretch is rolled once, here, so more hits don't mean more chances.
    val (cooldown, bias) = ability.cooldown(settings)
    val factor = PhaseTwo.factor(settings, boss.phaseTwo) / boss.graded(catalog.settings.difficulty.weights.cooldowns)
    boss.used(
      ability.key,
      now + (Rolls.biased(cooldown.toDouble(), bias, Random) * factor).toLong(),
      now + (Rolls.biased(settings.generalCooldown.toDouble(), settings.generalCooldownBias, Random) * factor).toLong(),
    )
    announce(ability.message, cast)
  }

  /** Warns everyone in the fight, not only the player whose hit set it off: whoever hit the boss and is still near. */
  private fun announce(message: Message, cast: Cast) {
    mod.log.debug { "${cast.boss.type.key} (${cast.boss.uuid}) cast ${message.key}" }
    val broadcast = mod.catalog.settings.broadcast
    if (!broadcast.abilities) return
    val fighters = (cast.boss.damage.keys.mapNotNull(mod.server.playerList::getPlayer) + listOfNotNull(cast.attacker)).distinct()
      .filter { it.level() == cast.world && it.position().distanceToSqr(cast.at) <= FIGHT * FIGHT }
    fighters.forEach { player ->
      val resolvers = mod.texts.boss(mod.texts.languageOf(player), cast.boss)
      if (broadcast.abilitiesIn.equals("chat", ignoreCase = true)) mod.texts.send(player, message, *resolvers)
      else mod.texts.actionBar(player, message, *resolvers)
    }
  }

  companion object {
    /** How close a player who hit the boss must still be to hear about its abilities. */
    private const val FIGHT = 48.0

    /** The abilities a hit may set off, at random. */
    val PICKED: List<Ability> = listOf(
      Lightning, TeleportStrike, Grapple, Chain,
    )
  }
}
