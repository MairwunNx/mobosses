package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
import ru.mairwunnx.mobosses.lang.Message

/**
 * Fights and deaths. Each player's share of a boss's health decides their part of the experience,
 * and whether they get loot and rewards.
 */
class CombatListener(private val plugin: MoBossesPlugin) : Listener {
  /** Only hits that land count: a cancelled hit, in a protected area say, gives no share. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onDamage(event: EntityDamageEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    Bukkit.getScheduler().runTask(plugin, Runnable { plugin.bars.update(boss) })

    val by = (event as? EntityDamageByEntityEvent)?.damager ?: return
    val player = by as? Player ?: (by as? Projectile)?.shooter as? Player ?: return
    // A hit can't be worth more than the health left: overkill used to push a share past the whole boss.
    boss.damage.merge(player.uniqueId, event.finalDamage.coerceIn(0.0, boss.entity.health), Double::plus)
    plugin.bars.show(player, boss)
    plugin.abilities.onHit(boss, player)
  }

  @EventHandler(priority = EventPriority.HIGH)
  fun onDeath(event: EntityDeathEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    val catalog = plugin.catalog
    val settings = catalog.settings
    val maxHealth = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0

    if (boss.damage.isNotEmpty() && maxHealth > 0) {
      val experience = Formulas.experience(settings.formula.experience, event.droppedExp, boss.clazz.experience, boss.type.spec.expModifier, boss.level)
      val bossExperience = Formulas.bossExperience(settings.progression.expGain, boss.entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 0.0, boss.clazz.experience, boss.level)
      event.droppedExp = 0
      val volume = (0.2f + 0.15f * catalog.classes.rank(boss.clazz.key).coerceAtLeast(0)).coerceAtMost(1f)

      boss.damage.forEach { (id, dealt) ->
        val player = Bukkit.getPlayer(id) ?: return@forEach
        val share = (dealt / maxHealth).coerceIn(0.0, 1.0)
        player.giveExp((experience * share).toInt())
        plugin.progression.add(player, (bossExperience * share).toLong())
        if (share > settings.loot.minDamageShare) plugin.loot.drop(boss, player)
        if (share > catalog.rewards.minDamageShare) plugin.rewards.give(player, boss)
        catalog.killSound?.let { player.playSound(player.location, it, volume, 1f) }
        plugin.log.debug { "${player.name} dealt ${"%.0f".format(share * 100)}% of a level ${boss.level} ${boss.clazz.key} ${boss.type.key}" }
      }

      if (settings.broadcast.kills) {
        val names = boss.damage.keys.mapNotNull { Bukkit.getPlayer(it)?.name }.joinToString(", ")
        plugin.texts.broadcast(Message.KILL) { code ->
          arrayOf(Placeholder.unparsed("player", names), Placeholder.unparsed("experience", experience.toString()), *plugin.texts.boss(code, boss))
        }
      }
    }

    boss.type.deathSound?.let { boss.entity.world.playSound(boss.entity.location, it, 1f, 1f) }
    plugin.bosses.forget(boss)
  }

  /** Meteors break up on impact instead of turning into magma blocks. */
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  fun onMeteorLand(event: EntityChangeBlockEvent) {
    val block = event.entity as? FallingBlock ?: return
    if (block.persistentDataContainer.has(plugin.keys.meteor, PersistentDataType.BOOLEAN)) event.isCancelled = true
  }

  /** Blocks a boss or a minion changes, like an enderman picking one up or a zombie breaking a door, go to CoreProtect too. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onChangeBlock(event: EntityChangeBlockEvent) {
    if (!fromBosses(event.entity)) return
    val block = event.block
    if (!block.type.isAir) plugin.blockLog.removed(block.location, block.blockData)
    if (!event.blockData.material.isAir) plugin.blockLog.placed(block.location, event.blockData)
  }

  /** So do blocks a boss or minion blows up. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onExplode(event: EntityExplodeEvent) {
    if (!fromBosses(event.entity)) return
    event.blockList().forEach { plugin.blockLog.removed(it.location, it.blockData) }
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) = plugin.bars.hide(event.player)

  @EventHandler
  fun onChangeWorld(event: PlayerChangedWorldEvent) = plugin.bars.hide(event.player)

  private fun fromBosses(entity: Entity) =
    plugin.bosses.get(entity) != null || entity.persistentDataContainer.has(plugin.keys.summoner, PersistentDataType.STRING)
}
