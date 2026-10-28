package ru.mairwunnx.mobosses.combat

import com.destroystokyo.paper.entity.RangedEntity
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Blaze
import org.bukkit.entity.Boat
import org.bukkit.entity.Breeze
import org.bukkit.entity.Entity
import org.bukkit.entity.Flying
import org.bukkit.entity.Guardian
import org.bukkit.entity.Minecart
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Vex
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.event.entity.EntityPortalEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.ability.kinds.Grapple
import ru.mairwunnx.mobosses.ability.kinds.TeleportStrike
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.boss.WorldBosses
import java.util.UUID

/**
 * Tricks that beat bosses without a fight. A boss never gets into a boat or a minecart and never goes through a
 * portal. A boss a player keeps hitting from beyond its reach, from a pillar, over a wall, or while it sits in a hole
 * or under a trapdoor, steps out next to them, or pulls them off when nothing there fits ([Cheese] decides when).
 * Flyers, the dragon and the wither among them, and guardians are left out: nothing traps them the same way, and a
 * guardian can't leave its water.
 */
class AntiCheese(private val plugin: MoBossesPlugin) : Listener {
  private val watches = HashMap<UUID, Cheese>()

  private val settings get() = plugin.catalog.settings.antiCheese

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onMount(event: EntityMountEvent) {
    if (settings.vehicles && isCart(event.mount) && plugin.bosses.get(event.entity) != null) event.isCancelled = true
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onPortal(event: EntityPortalEvent) {
    if (settings.portals && plugin.bosses.get(event.entity) != null) event.isCancelled = true
  }

  /** Once a second. A boss already in a boat or a minecart, as a mob that sat in one when it turned, gets out. */
  fun tick() {
    if (settings.vehicles) plugin.bosses.all.forEach { if (it.entity.vehicle?.let(::isCart) == true) it.entity.leaveVehicle() }
    val stepOut = settings.stepOut
    val now = System.currentTimeMillis()
    if (!stepOut.enabled) return watches.clear()
    plugin.bosses.all.forEach { boss ->
      val entity = boss.entity as? Mob ?: return@forEach
      if (!entity.isValid || entity is Flying || entity is Vex || entity is Blaze || entity is Breeze || entity is Guardian || WorldBosses.isWorldBoss(boss.type.key)) return@forEach
      val at = entity.location
      val target = boss.hitWithin(PRESSED_MILLIS).mapNotNull(Bukkit::getPlayer)
        .filter { it.world == at.world && it.gameMode != GameMode.SPECTATOR && Targets.canHit(it, plugin.guards) }
        .minByOrNull { it.location.distanceSquared(at) }
      val far = target != null && target.location.distanceSquared(at) > stepOut.reach * stepOut.reach
      // An archer that sees its target fights back from where it stands.
      val pressed = far && !(ranged(entity) && entity.hasLineOfSight(target!!))
      val blocked = pressed && entity.pathfinder.findPath(target!!)?.canReachFinalPoint() != true
      val watch = watches.getOrPut(boss.uuid, ::Cheese)
      if (!watch.sample(stepOut, at.x, at.y, at.z, now, pressed, blocked)) return@forEach
      if (stepOut(boss, target!!)) watch.stepped(now)
    }
  }

  fun forget(boss: Boss) = watches.remove(boss.uuid)

  /** Next to [target], where the boss fits and they can see it; or else they come down to it. */
  private fun stepOut(boss: Boss, target: Player): Boolean {
    val center = target.location.toBlockLocation()
    val spot = SPOTS.asSequence()
      .map { (dx, dy, dz) -> center.clone().add(dx + 0.5, dy.toDouble(), dz + 0.5) }
      .firstOrNull { TeleportStrike.fits(boss.entity, plugin.guards, it) && target.hasLineOfSight(it) }
    plugin.log.debug { "${boss.type.key} (${boss.uuid}) was out of reach of ${target.name}; ${if (spot != null) "steps out" else "pulls them"}" }
    if (spot == null) {
      val abilities = plugin.catalog.abilities
      return Grapple.pull(target, boss.entity.location, abilities.grappleStrength, abilities.grappleRange)
    }
    val from = boss.entity.location
    spot.direction = target.location.toVector().subtract(spot.toVector())
    from.world.spawnParticle(Particle.PORTAL, from.add(0.0, 1.0, 0.0), 30, 0.5, 1.0, 0.5, 0.1)
    if (!boss.entity.teleport(spot)) return false
    spot.world.playSound(spot, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f)
    return true
  }

  private fun isCart(entity: Entity) = entity is Boat || entity is Minecart

  private fun ranged(entity: Mob) = entity is RangedEntity

  private companion object {
    /** A player who hit the boss within this long still presses it. */
    const val PRESSED_MILLIS = 3000L

    /** Spots around a player, nearest first: their own level, then a little up, then down toward the ground. */
    val SPOTS: List<Triple<Int, Int, Int>> = buildList {
      for (dy in listOf(0, 1, -1, -2, -3)) for (r in 1..3) for (dx in -r..r) for (dz in -r..r) {
        if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) == r) add(Triple(dx, dy, dz))
      }
    }
  }
}
