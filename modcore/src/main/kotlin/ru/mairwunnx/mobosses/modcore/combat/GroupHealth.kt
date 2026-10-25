package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.ai.attributes.Attributes
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.BossFactory
import kotlin.math.roundToInt

/**
 * A boss grows as more players truly fight it, as on Paper: a player counts once they've dealt `fighter_share` of its
 * starting health, and each one past the first adds `per_player` of it. Its health grows in step, so a boss at half
 * health stays at half. Someone who only stood by changes nothing, and the boss never shrinks back.
 */
class GroupHealth(private val mod: MoBosses) {
  fun update(boss: Boss) {
    val cfg = mod.catalog.settings.groupHealth
    if (!cfg.enabled) return
    val attribute = boss.entity.getAttribute(Attributes.MAX_HEALTH) ?: return
    val base = boss.baseHealth ?: attribute.baseValue
    val fighters = boss.damage.values.count { it >= base * cfg.fighterShare }.coerceIn(1, cfg.maxPlayers.coerceAtLeast(1))
    if (fighters <= boss.fighters) return

    val before = attribute.value
    boss.baseHealth = base
    boss.fighters = fighters
    attribute.baseValue = (base * (1.0 + cfg.perPlayer.coerceAtLeast(0.0) * (fighters - 1))).coerceAtMost(BossFactory.MAX_HEALTH)
    val grown = attribute.value - before
    if (grown <= 0.0) return
    boss.addedHealth += grown * boss.entity.health / before
    boss.entity.health = (boss.entity.health * attribute.value / before).toFloat().coerceAtMost(attribute.value.toFloat())
    mod.bars.update(boss)

    val level = boss.entity.level() as? ServerLevel ?: return
    World.particles(level, ParticleTypes.ENCHANTED_HIT, boss.entity.position().add(0.0, boss.entity.bbHeight / 2.0, 0.0), 30, 0.5, 0.6, 0.5, 0.2)
    World.sound(level, boss.entity.position(), SoundEvents.IRON_GOLEM_REPAIR, 1f, 0.6f)
    val near = boss.damage.keys.mapNotNull(mod.server.playerList::getPlayer).filter { it.level() == level }
    val percent = ((attribute.value / base - 1.0) * 100).roundToInt()
    near.forEach { player ->
      mod.texts.actionBar(player, Message.GROUP_HEALTH, Placeholder.unparsed("players", fighters.toString()), Placeholder.unparsed("percent", percent.toString()),
        *mod.texts.boss(mod.texts.languageOf(player), boss))
    }
  }

  /** The health the shares split: what it started with, and what groups added while it was hurt. */
  fun whole(boss: Boss, maxHealth: Double): Double = boss.baseHealth?.let { it + boss.addedHealth } ?: maxHealth
}
