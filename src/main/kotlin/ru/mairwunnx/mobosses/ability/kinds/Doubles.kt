package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Ageable
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.cos
import kotlin.math.sin

/**
 * The boss vanishes in smoke and comes back among copies of itself: same look, gear, name, and glow,
 * at one health each. It stands in one of the spots at random. Copies drop only what their mob would
 * and 1 experience, and vanish after `doubles_seconds`. Mini bosses don't split.
 */
object Doubles : Ability {
  override val key = "doubles"
  override val message = Message.DOUBLES
  override fun enabled(settings: AbilitySettings) = settings.doubles.enabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.doubles
  override fun cooldown(settings: AbilitySettings) = settings.doubles.cooldown to settings.doubles.cooldownBias

  override fun usable(cast: Cast) = cast.boss.summoner == null && cast.nearest(24.0) != null

  override fun cast(cast: Cast): Boolean {
    val cfg = cast.settings.doubles
    val boss = cast.boss.entity
    val count = Rolls.linear(cfg.countBase, cfg.countPerLevel, cfg.countMax, cast.level).coerceIn(1, cfg.countMax.coerceAtLeast(1))
    val spots = (spots(cast.at, cfg.radius, count) + cast.at.clone()).shuffled()
    if (spots.size < 2) return false
    spots.forEach { poof(it) }
    cast.sound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 0.9f)
    cast.sound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f)

    val real = spots.first()
    boss.teleport(real.clone().apply { yaw = boss.location.yaw; pitch = boss.location.pitch })
    val copies = spots.drop(1).mapNotNull { copy(cast, it) }
    cast.later(cfg.seconds.coerceAtLeast(1) * 20L) {
      copies.filter { it.isValid }.forEach {
        poof(it.location)
        cast.plugin.decoys.remove(it)
      }
    }
    return copies.isNotEmpty()
  }

  private fun copy(cast: Cast, at: Location): LivingEntity? {
    val boss = cast.boss.entity
    val keys = cast.plugin.keys
    val decoy = at.world.spawn(at, boss.type.entityClass!!, SpawnReason.CUSTOM) { spawned ->
      // Set before the spawn event, so it never rolls into a boss of its own.
      spawned.persistentDataContainer.set(keys.decoy, PersistentDataType.STRING, cast.boss.uuid.toString())
      spawned.isPersistent = false
    } as? LivingEntity ?: return null
    decoy.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0
    decoy.health = 1.0
    if (decoy is Ageable && boss is Ageable) { if (boss.isAdult) decoy.setAdult() else decoy.setBaby() }
    val gear = boss.equipment
    val copyGear = decoy.equipment
    if (gear != null && copyGear != null) SLOTS.forEach { slot ->
      copyGear.setItem(slot, gear.getItem(slot).clone())
      // Only what the mob itself drops: none of the boss's gear.
      copyGear.setDropChance(slot, 0f)
    }
    boss.customName()?.let { decoy.customName(it) }
    decoy.isCustomNameVisible = boss.isCustomNameVisible
    if (boss is Mob && decoy is Mob) decoy.target = boss.target
    if (boss.isGlowing) {
      Bukkit.getScoreboardManager().mainScoreboard.getEntryTeam(boss.uniqueId.toString())?.addEntry(decoy.uniqueId.toString())
      decoy.isGlowing = true
    }
    cast.plugin.decoys.add(decoy)
    return decoy
  }

  /** [count] spots on the ground around the boss, evenly around a circle. */
  private fun spots(at: Location, radius: Double, count: Int): List<Location> = (0 until count).mapNotNull { i ->
    val angle = Math.PI * 2 * i / count
    val x = at.x + cos(angle) * radius
    val z = at.z + sin(angle) * radius
    val ground = Targets.ground(at.world, x, at.y, z, 4) ?: return@mapNotNull null
    if (!Targets.isOpen(ground.getRelative(0, 2, 0))) return@mapNotNull null
    Location(at.world, ground.x + 0.5, ground.y + 1.0, ground.z + 0.5)
  }

  private val SLOTS = listOf(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

  private fun poof(at: Location) {
    at.world.spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0.0, 1.0, 0.0), 20, 0.3, 0.6, 0.3, 0.02)
  }
}
