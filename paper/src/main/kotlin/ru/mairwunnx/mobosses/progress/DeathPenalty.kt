package ru.mairwunnx.mobosses.progress

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Entity
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message

/**
 * Dying to a boss, by its blow, its shot, or its ability, burns the progress toward the next level:
 * the level stays, the way to the next one starts over. A pair to the survival bonus: something to lose.
 * From `death_demotion` deaths in a row, with no kill that counts between, each one costs a level too. Every death
 * counts, for the player's card and the boss's statistics, penalty or not ([Deaths]).
 */
class DeathPenalty(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR)
  fun onDeath(event: PlayerDeathEvent) {
    val player = event.player
    val by = (player.lastDamageCause as? EntityDamageByEntityEvent)?.damager ?: return
    val source: Entity? = (by as? Projectile)?.shooter as? Entity ?: (by as? EvokerFangs)?.owner ?: by
    val boss = source?.let(plugin.bosses::get) ?: return
    plugin.achievements.diedTo(player, boss)
    plugin.progression.count(boss.type.key, boss.clazz.key, BossEvent.KILLED_A_PLAYER)
    val levelBefore = plugin.progression.level(player.uniqueId)
    plugin.progression.died(player.uniqueId, player.name) { death ->
      plugin.worldApi.diedTo(player, boss, death)
      if (death.demoted) plugin.worldApi.demoted(player, levelBefore, plugin.progression.level(player.uniqueId), death.after.streak)
      if (!player.isOnline || death.lost <= 0) return@died
      val level = plugin.progression.level(player.uniqueId)
      val resolvers = plugin.texts.boss(plugin.texts.languageOf(player), boss)
      val lost = Placeholder.unparsed("lost", death.lost.toString())
      if (death.demoted) plugin.texts.send(player, Message.DEATH_DEMOTION, lost, Placeholder.unparsed("new_level", level.toString()),
        Placeholder.unparsed("deaths", death.after.streak.toString()), *resolvers)
      else plugin.texts.send(player, Message.DEATH_PENALTY, lost, Placeholder.unparsed("next", (level + 1).toString()), *resolvers)
    }
  }
}
