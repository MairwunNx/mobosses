package ru.mairwunnx.mobosses.modcore.combat

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.RangedAttackMob
import net.minecraft.world.entity.monster.Vex
import net.minecraft.world.entity.monster.breeze.Breeze
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.boss.WorldBosses
import ru.mairwunnx.mobosses.combat.Cheese
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.ability.kinds.Grapple
import ru.mairwunnx.mobosses.modcore.ability.kinds.TeleportStrike
import ru.mairwunnx.mobosses.modcore.boss.Boss
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Tricks that beat bosses without a fight, as on Paper. A boss never gets into a boat or a minecart and never goes
 * through a portal. A boss a player keeps hitting from beyond its reach, from a pillar, over a wall, or while it sits
 * in a hole or under a trapdoor, steps out next to them, or pulls them off when nothing there fits ([Cheese] decides
 * when). Flyers and guardians are left out: nothing traps them the same way, and a guardian can't leave its water.
 */
class AntiCheese(private val mod: MoBosses) {
  private val watches = HashMap<UUID, Cheese>()

  private val settings get() = mod.catalog.settings.antiCheese

  /** Whether [entity] may get into [vehicle]. */
  fun mayRide(entity: Entity, vehicle: Entity) = !(settings.vehicles && isCart(vehicle) && mod.bosses.get(entity) != null)

  /** Whether [entity] may go through a portal. */
  fun mayTravel(entity: Entity) = !(settings.portals && mod.bosses.get(entity) != null)

  /** Once a second. A boss already in a boat or a minecart, as a mob that sat in one when it turned, gets out. */
  fun tick() {
    if (settings.vehicles) mod.bosses.all.forEach { if (it.entity.vehicle?.let(::isCart) == true) it.entity.stopRiding() }
    val stepOut = settings.stepOut
    val now = mod.now()
    if (!stepOut.enabled) return watches.clear()
    mod.bosses.all.forEach { boss ->
      val entity = boss.entity as? Mob ?: return@forEach
      if (!entity.isAlive || entity is Ghast || entity is Phantom || entity is Vex || entity is Blaze || entity is Breeze || entity is Guardian || WorldBosses.isWorldBoss(boss.type.key)) return@forEach
      val at = entity.position()
      val target = boss.hitWithin(PRESSED_MILLIS).mapNotNull(mod.server.playerList::getPlayer)
        .filter { it.level() == entity.level() && !it.isSpectator && Targets.canHit(it, mod.guards) }
        .minByOrNull { it.position().distanceToSqr(at) }
      val far = target != null && target.position().distanceToSqr(at) > stepOut.reach * stepOut.reach
      // An archer that sees its target fights back from where it stands.
      val pressed = far && !(entity is RangedAttackMob && entity.hasLineOfSight(target!!))
      val blocked = pressed && entity.navigation.createPath(target!!, 0)?.canReach() != true
      val watch = watches.getOrPut(boss.uuid, ::Cheese)
      if (!watch.sample(stepOut, at.x, at.y, at.z, now, pressed, blocked)) return@forEach
      if (stepOut(boss, target!!)) watch.stepped(now)
    }
  }

  fun forget(boss: Boss) = watches.remove(boss.uuid)

  /** Next to [target], where the boss fits and they can see it; or else they come down to it. */
  private fun stepOut(boss: Boss, target: ServerPlayer): Boolean {
    val level = boss.entity.level() as? ServerLevel ?: return false
    val center = BlockPos.containing(target.position())
    val spot = SPOTS.asSequence()
      .map { (dx, dy, dz) -> Vec3(center.x + dx + 0.5, (center.y + dy).toDouble(), center.z + dz + 0.5) }
      .firstOrNull { TeleportStrike.fits(boss.entity, level, mod.guards, it) && sees(level, target, it) }
    mod.log.debug { "${boss.type.key} (${boss.uuid}) was out of reach of ${target.plainTextName}; ${if (spot != null) "steps out" else "pulls them"}" }
    if (spot == null) {
      val abilities = mod.catalog.abilities
      return Grapple.pull(target, boss.entity.position(), abilities.grappleStrength, abilities.grappleRange)
    }
    World.particles(level, ParticleTypes.PORTAL, boss.entity.position().add(0.0, 1.0, 0.0), 30, 0.5, 1.0, 0.5, 0.1)
    val look = target.position().subtract(spot)
    boss.entity.teleportTo(level, spot.x, spot.y, spot.z, emptySet(), Math.toDegrees(atan2(-look.x, look.z)).toFloat(), 0f, true)
    World.sound(level, spot, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f)
    return true
  }

  private fun sees(level: ServerLevel, target: ServerPlayer, spot: Vec3) =
    level.clip(ClipContext(target.eyePosition, spot.add(0.0, 1.0, 0.0), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, target)).type == HitResult.Type.MISS

  private fun isCart(entity: Entity) = entity is AbstractBoat || entity is AbstractMinecart

  private companion object {
    /** A player who hit the boss within this long still presses it. */
    const val PRESSED_MILLIS = 3000L

    /** Spots around a player, nearest first: their own level, then a little up, then down toward the ground. */
    val SPOTS: List<Triple<Int, Int, Int>> = buildList {
      for (dy in listOf(0, 1, -1, -2, -3)) for (r in 1..3) for (dx in -r..r) for (dz in -r..r) {
        if (maxOf(abs(dx), abs(dz)) == r) add(Triple(dx, dy, dz))
      }
    }
  }
}
