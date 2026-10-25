package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.Targets
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.math.cos
import kotlin.math.sin

/**
 * The boss vanishes in smoke and comes back among copies of itself: same look, gear, name, and glow, at one health
 * each. It stands in one of the spots at random. Copies drop only what their mob would and 1 experience, and vanish
 * after `doubles_seconds`. Mini bosses don't split.
 */
object Doubles : Ability {
  override val key = "doubles"
  override val message = Message.DOUBLES
  override fun enabled(settings: AbilitySettings) = settings.doubles.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.doubles
  override fun cooldown(settings: AbilitySettings) = settings.doubles.cooldown to settings.doubles.cooldownBias

  override fun usable(cast: Cast) = cast.boss.summoner == null && cast.foe(24.0) != null

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.doubles
    val boss = cast.entity
    val count = Rolls.linear(cfg.countBase, cfg.countPerLevel, cfg.countMax, cast.level).coerceIn(1, cfg.countMax.coerceAtLeast(1))
    // The real one may stand in any spot, so each must fit the boss and lie outside claims.
    val spots = (spots(cast.world, cast.at, cfg.radius, count).filter { TeleportStrike.fits(cast, it) } + cast.at).shuffled()
    if (spots.size < 2) return false
    spots.forEach { poof(cast.world, it) }
    cast.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 0.9f)
    cast.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 1f, 1f)

    val real = spots.first()
    boss.teleportTo(cast.world, real.x, real.y, real.z, emptySet(), boss.yRot, boss.xRot, true)
    val copies = spots.drop(1).mapNotNull { copy(cast, it) }
    // Not cast.later: that skips when the boss is gone, and the copies must go anyway.
    cast.mod.scheduler.later(cfg.seconds.coerceAtLeast(1) * 20L) {
      copies.filter { it.isAlive }.forEach {
        (it.level() as? ServerLevel)?.let { level -> poof(level, it.position()) }
        cast.mod.decoys.remove(it)
      }
    }
    return copies.isNotEmpty()
  }

  private fun copy(cast: Cast, at: Vec3): LivingEntity? {
    val boss = cast.entity
    val decoy = boss.type.create(cast.world, EntitySpawnReason.MOB_SUMMONED) as? LivingEntity ?: return null
    // Set before it enters the world, so it never rolls into a boss of its own; it isn't saved either.
    Data(decoy)[Keys.DECOY] = cast.boss.uuid.toString()
    decoy.snapTo(at.x, at.y, at.z, boss.yRot, boss.xRot)
    if (decoy is Mob && boss is Mob) decoy.setBaby(boss.isBaby)
    SLOTS.forEach { slot ->
      decoy.setItemSlot(slot, boss.getItemBySlot(slot).copy())
      // Only what the mob itself drops: none of the boss's gear.
      if (decoy is Mob) decoy.setDropChance(slot, 0f)
    }
    if (!cast.world.addFreshEntity(decoy)) return null
    decoy.getAttribute(Attributes.MAX_HEALTH)?.baseValue = 1.0
    decoy.health = 1f
    boss.customName?.let { decoy.customName = it }
    decoy.isCustomNameVisible = boss.isCustomNameVisible
    if (boss is Mob && decoy is Mob) decoy.target = boss.target
    if (boss.hasGlowingTag()) {
      val scoreboard = cast.mod.server.scoreboard
      scoreboard.getPlayersTeam(boss.uuid.toString())?.let { scoreboard.addPlayerToTeam(decoy.uuid.toString(), it) }
      decoy.setGlowingTag(true)
    }
    cast.mod.decoys.add(decoy)
    return decoy
  }

  /** [count] spots on the ground around the boss, evenly around a circle. */
  private fun spots(level: ServerLevel, at: Vec3, radius: Double, count: Int): List<Vec3> = (0 until count).mapNotNull { i ->
    val angle = Math.PI * 2 * i / count
    val x = at.x + cos(angle) * radius
    val z = at.z + sin(angle) * radius
    val ground = Targets.ground(level, x, at.y, z, 4) ?: return@mapNotNull null
    if (!Targets.isOpen(level, ground.above(2))) return@mapNotNull null
    Vec3(ground.x + 0.5, ground.y + 1.0, ground.z + 0.5)
  }

  private val SLOTS = listOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

  private fun poof(level: ServerLevel, at: Vec3) = World.particles(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, 1.0, 0.0), 20, 0.3, 0.6, 0.3, 0.02)
}
