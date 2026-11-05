package ru.mairwunnx.mobosses.progress

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Entity
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message

/**
 * Dying to a boss, by its blow, its shot, or its ability, burns the progress toward the next level:
 * the level stays, the way to the next one starts over. A pair to the survival bonus: something to lose.
 * From `death_demotion` deaths in a row, with no kill that counts between, each one costs a level too.
 */
class DeathPenalty(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR)
  fun onDeath(event: PlayerDeathEvent) {
    val player = event.player
    val by = (player.lastDamageCause as? EntityDamageByEntityEvent)?.damager ?: return
    val source: Entity? = (by as? Projectile)?.shooter as? Entity ?: (by as? EvokerFangs)?.owner ?: by
    val boss = source?.let(plugin.bosses::get) ?: return
    plugin.achievements.diedTo(player, boss)
    val settings = plugin.catalog.settings.progression
    if (!settings.deathPenalty) return plugin.worldApi.diedTo(player, boss, 0)
    val deaths = (player.persistentDataContainer.get(plugin.keys.deathStreak, PersistentDataType.INTEGER) ?: 0) + 1
    player.persistentDataContainer.set(plugin.keys.deathStreak, PersistentDataType.INTEGER, deaths)
    val demote = settings.deathDemotion > 0 && deaths >= settings.deathDemotion
    plugin.progression.penalize(player.uniqueId, player.name, demote) { lost ->
      plugin.worldApi.diedTo(player, boss, lost)
      if (!player.isOnline) return@penalize
      val level = plugin.progression.level(player.uniqueId)
      val resolvers = plugin.texts.boss(plugin.texts.languageOf(player), boss)
      if (demote) plugin.texts.send(player, Message.DEATH_DEMOTION, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("level", level.toString()), Placeholder.unparsed("deaths", deaths.toString()), *resolvers)
      else plugin.texts.send(player, Message.DEATH_PENALTY, Placeholder.unparsed("lost", lost.toString()),
        Placeholder.unparsed("next", (level + 1).toString()), *resolvers)
    }
  }

  /** A kill that counts ends [player]'s streak of deaths. */
  fun won(player: Player) = player.persistentDataContainer.remove(plugin.keys.deathStreak)
}
