package ru.mairwunnx.mobosses.combat

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Hits on bosses: each player's share of the damage, kept on the boss, the health bar, and the
 * abilities a hit may set off. Only hits that land count; a hit cancelled in a protected area gives
 * no share.
 */
class DamageListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onDamage(event: EntityDamageEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    Bukkit.getScheduler().runTask(plugin, Runnable { plugin.bars.update(boss) })

    val by = (event as? EntityDamageByEntityEvent)?.damager ?: return
    val player = by as? Player ?: (by as? Projectile)?.shooter as? Player ?: return
    // A hit can't be worth more than the health left: overkill used to push a share past the whole boss.
    boss.addDamage(player.uniqueId, event.finalDamage.coerceIn(0.0, boss.entity.health))
    plugin.bars.show(player, boss)
    plugin.abilities.onHit(boss, player)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) = plugin.bars.hide(event.player)

  @EventHandler
  fun onChangeWorld(event: PlayerChangedWorldEvent) = plugin.bars.hide(event.player)
}
