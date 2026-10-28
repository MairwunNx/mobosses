package ru.mairwunnx.mobosses.combat

import org.bukkit.Bukkit
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Entity
import org.bukkit.entity.Golem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.ability.kinds.Chain
import ru.mairwunnx.mobosses.algebra.Stats

/**
 * Hits on bosses: each player's share of the damage, their pets' included, kept on the boss, the
 * health bar, and the abilities a hit may set off. A player, their pet, or a golem sets abilities
 * off; the pet or golem becomes fair game for them. Only hits that land count; a hit cancelled in a
 * protected area gives no share. Fire a player lit, with Flame or Fire Aspect, keeps burning in
 * their name. Bosses' own shots grow with their level here too.
 */
class DamageListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onDamage(event: EntityDamageEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    Bukkit.getScheduler().runTask(plugin, Runnable { plugin.bars.update(boss) })

    val by = (event as? EntityDamageByEntityEvent)?.damager
    val dealt = event.finalDamage.coerceIn(0.0, boss.entity.health)
    if (by == null) {
      // The game burns a mob in nobody's name: without this, a Flame bow left a third of the boss to nobody.
      val igniter = boss.igniter.takeIf { event.cause == EntityDamageEvent.DamageCause.FIRE_TICK }
      // Anything else nobody dealt by hand, a fall after a knockback, lava, poison, goes to the players fighting it,
      // or a solo player who knocked it off a ledge would lose part of their share.
      if (igniter != null) boss.addDamage(igniter.first, dealt, igniter.second) else boss.credit(dealt)
      plugin.groupHealth.update(boss)
      return
    }
    // Who's behind the hit: an arrow's shooter, the player who lit the TNT, the trident that called the lightning.
    val source = event.damageSource.causingEntity ?: (by as? Projectile)?.shooter as? Entity ?: by
    val pet = (source as? Tameable)?.takeIf { it.isTamed }
    val player = source as? Player ?: pet?.owner as? Player
    // A hit can't be worth more than the health left: overkill used to push a share past the whole boss.
    player?.let {
      boss.addDamage(it.uniqueId, dealt, byPet = pet != null)
      plugin.groupHealth.update(boss)
    }
    if (source !is Player) {
      // Only pets and golems: other mobs that hit the boss by chance don't wake its abilities.
      if (pet == null && source !is Golem) return
      Targets.markAggressor(source, boss.uuid, plugin.keys.aggressor)
    }
    player?.takeIf { it.isOnline && it.world == boss.entity.world }?.let {
      plugin.bars.show(it, boss)
      plugin.achievements.grant(it, Achievement.HUNT_BEGINS)
    }
    // A blow in melee loosens the chain on whoever strikes.
    if (by is Player) Chain.struck(plugin, boss, by)
    // The killing blow casts nothing: no minions or doubles for a boss already dead.
    if (event.finalDamage >= boss.entity.health) return
    plugin.abilities.onHit(boss, player?.takeIf { it.isOnline })
  }

  /** Who set a boss alight: a player, their arrow, or their pet; fire from anything else, like lava or the sun, burns in nobody's name. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onCombust(event: EntityCombustEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    val by = (event as? EntityCombustByEntityEvent)?.combuster
    val source = (by as? Projectile)?.shooter as? Entity ?: by
    val pet = (source as? Tameable)?.takeIf { it.isTamed }
    val player = source as? Player ?: pet?.owner as? Player
    boss.igniter = player?.let { it.uniqueId to (pet != null) }
  }

  /** A boss's arrows, fireballs, and tridents hit harder with its level, by `formula.ranged_damage` of its melee growth. Abilities' shots have their own damage. */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onShot(event: EntityDamageByEntityEvent) {
    val shot = event.damager as? Projectile ?: return
    val boss = (shot.shooter as? LivingEntity)?.let(plugin.bosses::get) ?: return
    if (shot.persistentDataContainer.has(plugin.keys.arrow, PersistentDataType.BOOLEAN) || shot.persistentDataContainer.has(plugin.keys.meteor, PersistentDataType.BOOLEAN)) return
    val formula = plugin.catalog.settings.formula
    val graded = boss.graded(plugin.catalog.settings.difficulty.weights.damage)
    event.damage *= (Stats.ranged(formula.damage, boss.clazz.damage, boss.level, formula.rangedDamage) * graded).coerceAtLeast(1.0)
  }

  /** The dragon's blows and breath have no attribute to grow with its level, so they grow here, as its fireballs do. */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onDragon(event: EntityDamageByEntityEvent) {
    if (event.damager is Projectile) return
    val dragon = event.damageSource.causingEntity as? EnderDragon ?: return
    val boss = plugin.bosses.get(dragon) ?: return
    val formula = plugin.catalog.settings.formula
    val graded = boss.graded(plugin.catalog.settings.difficulty.weights.damage)
    event.damage *= (Stats.ranged(formula.damage, boss.clazz.damage, boss.level, formula.rangedDamage) * graded).coerceAtLeast(1.0)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) = plugin.bars.hide(event.player)

  @EventHandler
  fun onChangeWorld(event: PlayerChangedWorldEvent) = plugin.bars.hide(event.player)
}
