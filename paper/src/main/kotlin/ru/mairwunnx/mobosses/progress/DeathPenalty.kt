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
 */
class DeathPenalty(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR)
  fun onDeath(event: PlayerDeathEvent) {
    val player = event.player
    val by = (player.lastDamageCause as? EntityDamageByEntityEvent)?.damager ?: return
    val source: Entity? = (by as? Projectile)?.shooter as? Entity ?: (by as? EvokerFangs)?.owner ?: by
    val boss = source?.let(plugin.bosses::get) ?: return
    plugin.achievements.diedTo(player, boss)
    if (!plugin.catalog.settings.progression.deathPenalty) return plugin.worldApi.diedTo(player, boss, 0)
    plugin.progression.penalize(player.uniqueId, player.name) { lost ->
      plugin.worldApi.diedTo(player, boss, lost)
      if (!player.isOnline) return@penalize
      plugin.texts.send(player, Message.DEATH_PENALTY, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("next", (plugin.progression.level(player.uniqueId) + 1).toString()), *plugin.texts.boss(plugin.texts.languageOf(player), boss))
    }
  }
}
