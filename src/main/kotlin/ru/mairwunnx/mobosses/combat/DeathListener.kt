package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * A boss's death. Each player's share of its health decides their part of the vanilla experience
 * and of the Mo’Bosses experience, the latter weighed by how close the boss was to their level; a
 * share past the thresholds brings loot and rewards, and the kill counts toward their level.
 */
class DeathListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onDeath(event: EntityDeathEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    val shares = Shares.of(boss.damage, boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0)
    if (shares.isNotEmpty()) {
      val experience = Stats.experience(plugin.catalog.settings.formula.experience, event.droppedExp, boss.clazz.experience, boss.type.spec.expModifier, boss.level)
      event.droppedExp = 0
      shares.forEach { (id, share) -> Bukkit.getPlayer(id)?.let { pay(it, boss, share, experience) } }
      announce(boss, shares, experience)
    }
    boss.type.deathSound?.let { boss.entity.world.playSound(boss.entity.location, it, 1f, 1f) }
    plugin.bosses.forget(boss)
  }

  private fun pay(player: Player, boss: Boss, share: Double, experience: Int) {
    val settings = plugin.catalog.settings
    val progression = settings.progression
    player.giveExp((experience * share).toInt())

    val level = plugin.progression.level(player.uniqueId)
    val worth = Leveling.bossExperience(progression.expGain, boss.entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 0.0, boss.clazz.experience, boss.level)
    val gained = (worth * share * Leveling.relevance(progression.relevance, level, boss.level)).toLong()
    plugin.progression.add(player, gained, Leveling.countsAsKill(progression, share, level, boss.level))

    if (share > settings.loot.minDamageShare) plugin.loot.drop(boss, player)
    if (share > plugin.catalog.rewards.minDamageShare) plugin.rewards.give(player, boss)
    val volume = (0.2f + 0.15f * plugin.catalog.classes.rank(boss.clazz.key).coerceAtLeast(0)).coerceAtMost(1f)
    plugin.catalog.killSound?.let { player.playSound(player.location, it, volume, 1f) }
    plugin.log.debug { "${player.name} dealt ${"%.0f".format(share * 100)}% of a level ${boss.level} ${boss.clazz.key} ${boss.type.key}: +$gained Mo’Bosses experience" }
  }

  private fun announce(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    if (!plugin.catalog.settings.broadcast.kills) return
    val names = shares.keys.mapNotNull { Bukkit.getPlayer(it)?.name }.joinToString(", ")
    plugin.texts.broadcast(Message.KILL) { code ->
      arrayOf(Placeholder.unparsed("player", names), Placeholder.unparsed("experience", experience.toString()), *plugin.texts.boss(code, boss))
    }
  }
}
