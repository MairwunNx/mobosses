package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.roundToInt

/**
 * A boss grows as more players truly fight it, so four friends don't melt it four times as fast: a
 * player counts once they've dealt `fighter_share` of its starting health, and each one past the first
 * adds `per_player` of it. Its health grows in step, so a boss at half health stays at half. Someone
 * who only stood by changes nothing, and the boss never shrinks back.
 */
class GroupHealth(private val plugin: MoBossesPlugin) {
  fun update(boss: Boss) {
    val cfg = plugin.catalog.settings.groupHealth
    if (!cfg.enabled) return
    val attribute = boss.entity.getAttribute(Attribute.MAX_HEALTH) ?: return
    val base = boss.baseHealth ?: attribute.baseValue
    val fighters = boss.damage.values.count { it >= base * cfg.fighterShare }.coerceIn(1, cfg.maxPlayers.coerceAtLeast(1))
    if (fighters <= boss.fighters) return

    val before = attribute.value
    boss.baseHealth = base
    boss.fighters = fighters
    attribute.baseValue = (base * (1.0 + cfg.perPlayer.coerceAtLeast(0.0) * (fighters - 1))).coerceAtMost(MAX)
    val grown = attribute.value - before
    if (grown <= 0.0) return
    boss.addedHealth += grown * boss.entity.health / before
    boss.entity.health = (boss.entity.health * attribute.value / before).coerceAtMost(attribute.value)
    plugin.bars.update(boss)

    boss.entity.world.spawnParticle(Particle.ENCHANTED_HIT, boss.entity.location.add(0.0, boss.entity.height / 2, 0.0), 30, 0.5, 0.6, 0.5, 0.2)
    boss.entity.world.playSound(boss.entity.location, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 0.6f)
    val near = boss.damage.keys.mapNotNull(Bukkit::getPlayer).filter { it.world == boss.entity.world }
    val percent = ((attribute.value / base - 1.0) * 100).roundToInt()
    near.forEach { player ->
      plugin.texts.actionBar(player, Message.GROUP_HEALTH, Placeholder.unparsed("players", fighters.toString()), Placeholder.unparsed("percent", percent.toString()),
        *plugin.texts.boss(plugin.texts.languageOf(player), boss))
    }
  }

  /** The health the shares split: what it started with, and what groups added while it was hurt. */
  fun whole(boss: Boss, maxHealth: Double): Double = boss.baseHealth?.let { it + boss.addedHealth } ?: maxHealth

  private companion object {
    /** The game's limit for max health. */
    const val MAX = 1024.0
  }
}
