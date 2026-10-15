package ru.mairwunnx.mobosses.ability

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Ageable
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
import ru.mairwunnx.mobosses.boss.Boss
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The abilities themselves. Each returns whether it did anything, so a boss with nobody in reach
 * keeps the ability for later. Abilities hurt only players (see [Targets]); the boss's own minions,
 * pets, and villagers stay out of it. Blocks change only through [ru.mairwunnx.mobosses.protect.BlockChanges].
 */
class Casts(private val plugin: MoBossesPlugin) {
  private val settings get() = plugin.catalog.abilities
  private val guard get() = plugin.guards

  fun cast(ability: Ability, boss: Boss, attacker: Player): Boolean = when (ability) {
    Ability.LIGHTNING -> lightning(boss)
    Ability.SUMMONING -> summon(boss)
    Ability.REGENERATION -> regeneration(boss)
    Ability.FIRE_RING -> fireRing(boss)
    Ability.WEB_RING -> webRing(boss)
    Ability.TELEPORT_STRIKE -> teleportStrike(boss)
    Ability.METEOR_SHOWER -> meteorShower(boss)
    Ability.ARROW_ARC -> arrowArc(boss)
    Ability.ARROW_RAIN -> arrowRain(boss)
  }.also { if (it) sound(ability, boss.entity.location) }

  /** Strikes around a lightning rod or copper nearby, else around the nearest player, else around the boss. */
  private fun lightning(boss: Boss): Boolean {
    val cfg = settings
    val at = boss.entity.location
    val world = at.world
    val radius = min(cfg.lightningRadius + boss.level * cfg.lightningRadiusModifier, cfg.lightningRadiusMax)
    val strikes = Formulas.linear(cfg.lightningStrikes, cfg.lightningStrikesModifier, cfg.lightningStrikesMax, boss.level)
    val damage = Formulas.linear(cfg.lightningDamage, cfg.lightningDamageModifier, cfg.lightningDamageMax, boss.level)

    val center = attractor(at, min(5.0, radius)) ?: Targets.nearest(at, radius, guard)?.location ?: at
    val points = listOf(center) + List((strikes - 1).coerceAtLeast(0)) {
      val x = center.x + Random.nextDouble(-radius, radius)
      val z = center.z + Random.nextDouble(-radius, radius)
      val y = Targets.ground(world, x, center.y, z)?.let { it.y + 1.0 } ?: center.y
      Location(world, x, y, z)
    }
    points.forEach { point ->
      // Only the flash: a real bolt would set fire to whatever it hits.
      world.strikeLightningEffect(point)
      Targets.players(point, 2.5, guard).forEach { it.damage(damage, boss.entity) }
    }
    return true
  }

  /** The block that draws lightning within [radius] around the fight's height: a rod first, then copper, nearest first. */
  private fun attractor(at: Location, radius: Double): Location? {
    val r = radius.toInt()
    var best: Location? = null
    var bestRod = false
    for (dx in -r..r) for (dz in -r..r) for (dy in -3..3) {
      if (dx * dx + dz * dz > radius * radius) continue
      val block = at.world.getBlockAt(at.blockX + dx, at.blockY + dy, at.blockZ + dz)
      val rod = block.type == Material.LIGHTNING_ROD
      if (!rod && block.type !in COPPER) continue
      val target = block.location.add(0.5, 1.0, 0.5)
      if (best == null || (rod && !bestRod) || (rod == bestRod && target.distanceSquared(at) < best.distanceSquared(at))) {
        best = target
        bestRod = rod
      }
    }
    return best
  }

  /** Minions from `minions.yml` at the boss's feet; some may roll into weaker bosses. */
  private fun summon(boss: Boss): Boolean {
    val minion = plugin.catalog.minions[boss.entity.type] ?: return false
    val count = Formulas.minions(plugin.catalog.settings.formula.minions, boss.level, Random)
    val at = boss.entity.location
    val name = plugin.texts.minionName(plugin.languages.default, minion)
    repeat(count) {
      val entity = at.world.spawn(at, minion.entity.entityClass!!, SpawnReason.CUSTOM) { spawned ->
        spawned.persistentDataContainer.set(plugin.keys.summoner, PersistentDataType.STRING, boss.uuid.toString())
      }
      if (entity is Ageable && entity.type in BABIES) entity.setBaby()
      if (plugin.bosses.get(entity) == null && name != null) {
        entity.customName(name)
        entity.isCustomNameVisible = true
      }
    }
    boss.type.summonSound?.let { at.world.playSound(at, it, 1f, 1f) }
    return count > 0
  }

  /**
   * Regeneration and resistance for a while, with arrows burning up and players slowed nearby.
   * Tridents are left alone: removing one would take a player's weapon.
   */
  private fun regeneration(boss: Boss): Boolean {
    val cfg = settings
    val entity = boss.entity
    val world = entity.world
    fun scaled(base: Int, perLevel: Double, cap: Int) = (base + perLevel * boss.level).toInt().coerceIn(1, cap.coerceAtLeast(1))

    val seconds = Formulas.biased(scaled(cfg.regenDurationSecondsBase, cfg.regenDurationSecondsPerLevel, cfg.regenDurationSecondsMax).toDouble(), cfg.regenerationDurationBias, Random)
      .coerceAtMost(cfg.regenDurationSecondsMax.toDouble())
    val ticks = (seconds * 20.0).toInt().coerceAtLeast(1)
    val amplifier = ((cfg.regenAmplifierBase + cfg.regenAmplifierPerLevel * boss.level).toInt()).coerceIn(0, cfg.regenAmplifierMax.coerceAtLeast(0))

    val start = entity.location
    world.playSound(start, Sound.ITEM_TOTEM_USE, 1.0f, 1.05f)
    world.spawnParticle(Particle.TOTEM_OF_UNDYING, start, 40, 0.6, 1.0, 0.6, 0.1)
    if (cfg.regenCleanseNegatives) {
      NEGATIVE.forEach(entity::removePotionEffect)
      entity.fireTicks = 0
    }
    entity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, ticks, amplifier, true, true, true))
    if (cfg.regenExtraResistanceSeconds > 0) entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, cfg.regenExtraResistanceSeconds * 20, 0, true, true, true))
    entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, ticks, amplifier.coerceAtMost(2), true, true, true))

    var tick = 0
    Bukkit.getScheduler().runTaskTimer(plugin, { task ->
      if (!entity.isValid || entity.isDead) return@runTaskTimer task.cancel()
      val base = entity.location
      if (tick % 2 == 0) world.spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(0.0, 1.0, 0.0), 6, 0.6, 0.6, 0.6, 0.0)
      if (tick % 8 == 0) world.spawnParticle(Particle.HEART, base.clone().add(0.0, 1.2, 0.0), 2, 0.2, 0.3, 0.2, 0.0)
      if (tick % 10 == 0) {
        val r = cfg.regenAuraRadius
        world.getNearbyEntities(base, r, r, r).forEach {
          if (cfg.regenAntiProjectiles && it is AbstractArrow && it !is Trident) {
            world.spawnParticle(Particle.SMOKE, it.location, 4, 0.1, 0.1, 0.1, 0.02)
            world.playSound(it.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.6f)
            it.remove()
          } else if (cfg.regenSlowPlayers && it is Player && Targets.canHit(it, guard)) {
            it.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false, true))
          }
        }
      }
      if (++tick >= ticks) {
        entity.removePotionEffect(PotionEffectType.REGENERATION)
        entity.removePotionEffect(PotionEffectType.RESISTANCE)
        world.playSound(entity.location, Sound.BLOCK_BEACON_DEACTIVATE, 0.9f, 1.0f)
        world.spawnParticle(Particle.END_ROD, entity.location, 24, 0.5, 1.0, 0.5, 0.02)
        task.cancel()
      }
    }, 0L, 1L)
    return true
  }

  /** Fire on the ground in a ring around the nearest player, or around the boss. */
  private fun fireRing(boss: Boss): Boolean {
    val cfg = settings
    val center = (Targets.nearest(boss.entity.location, 32.0, guard) ?: boss.entity).location
    val radius = if (cfg.fireRingMaxRadius > cfg.fireRingMinRadius) Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius) else cfg.fireRingMinRadius
    val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)
    val fire = Material.FIRE.createBlockData()
    var lit = 0
    for (i in 0 until 64) {
      if (Random.nextDouble() > density) continue
      val angle = 2 * PI * i / 64
      val ground = Targets.ground(center.world, center.x + radius * cos(angle), center.y, center.z + radius * sin(angle)) ?: continue
      val above = ground.getRelative(0, 1, 0)
      // Not inside the boss.
      if (boss.entity.boundingBox.contains(above.x + 0.5, above.y.toDouble(), above.z + 0.5)) continue
      if (plugin.blockChanges.set(above, fire)) lit++
    }
    return lit > 0
  }

  /** Cobwebs in a ring around the nearest player, gone after `web_ring_duration`, and slowness for players inside. */
  private fun webRing(boss: Boss): Boolean {
    val cfg = settings
    val target = Targets.nearest(boss.entity.location, 20.0, guard) ?: return false
    val center = target.location
    val web = Material.COBWEB.createBlockData()
    var placed = 0
    for (degrees in 0 until 360 step 10) {
      val angle = Math.toRadians(degrees.toDouble())
      val ground = Targets.ground(center.world, center.x + cfg.webRingRadius * cos(angle), center.y, center.z + cfg.webRingRadius * sin(angle)) ?: continue
      val above = ground.getRelative(0, 1, 0)
      if (above.type != Material.AIR && above.type != Material.CAVE_AIR && above.type != Material.SHORT_GRASS) continue
      if (plugin.blockChanges.setFor(above, web, cfg.webRingDuration * 20L)) {
        if (placed++ % 3 == 0) center.world.playSound(above.location, Sound.BLOCK_WOOL_PLACE, 0.3f, 0.8f)
      }
    }
    if (cfg.webRingPotionEnabled) {
      val seconds = min(cfg.webRingPotionBaseDuration + boss.level * cfg.webRingPotionLevelModifier, cfg.webRingPotionMaxDuration.toDouble())
      Targets.players(center, cfg.webRingRadius + 1, guard).forEach { it.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, (seconds * 20).toInt(), 1)) }
    }
    return placed > 0
  }

  /** The boss steps out of thin air two blocks behind the nearest player. */
  private fun teleportStrike(boss: Boss): Boolean {
    val target = Targets.nearest(boss.entity.location, 15.0, guard) ?: return false
    val from = boss.entity.location
    from.world.spawnParticle(Particle.PORTAL, from, 30, 0.5, 1.0, 0.5, 0.1)
    from.world.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
    val behind = target.location.clone().subtract(target.location.direction.setY(0).normalize().multiply(2.0)).add(0.0, 0.1, 0.0)
    val to = (-2..3).map { behind.clone().add(0.0, it.toDouble(), 0.0) }
      .firstOrNull { !it.block.type.isSolid && !it.clone().add(0.0, 1.0, 0.0).block.type.isSolid }
      ?: return false
    to.direction = target.location.toVector().subtract(to.toVector())
    boss.entity.teleport(to)
    to.world.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f)
    return true
  }

  /**
   * Waves of burning meteors over the nearest player in range. They break up on impact: the game
   * would turn a falling block into a block, so [ru.mairwunnx.mobosses.boss.CombatListener] stops that.
   */
  private fun meteorShower(boss: Boss): Boolean {
    val cfg = settings
    val center = boss.entity.location
    val radius = Formulas.linear(cfg.meteorRadiusBase, cfg.meteorRadiusPerLevel, cfg.meteorRadiusMax, boss.level)
    var target = Targets.nearest(center, radius, guard) ?: return false
    val waves = Formulas.linear(cfg.meteorWavesBase, cfg.meteorWavesPerLevel, cfg.meteorWavesMax, boss.level)
    val perWave = Formulas.linear(cfg.meteorsPerWaveBase, cfg.meteorsPerWavePerLevel, cfg.meteorsPerWaveMax, boss.level)
    val damage = Formulas.linear(cfg.meteorDamageBase, cfg.meteorDamagePerLevel, cfg.meteorDamageMax, boss.level)
    val lifetime = cfg.meteorLifetimeSeconds.coerceAtLeast(1) * 20L
    val magma = Material.MAGMA_BLOCK.createBlockData()

    repeat(waves) { wave ->
      Bukkit.getScheduler().runTaskLater(plugin, Runnable {
        if (!boss.entity.isValid) return@Runnable
        // The first target, while it's still in the fight; then whoever is nearest.
        if (!Targets.canHit(target, guard) || target.world != center.world) target = Targets.nearest(center, radius, guard) ?: return@Runnable
        repeat(perWave) {
          val at = target.location
          val spawn = Location(at.world, at.x + Random.nextDouble(-0.8, 0.8), at.y + cfg.meteorHeight, at.z + Random.nextDouble(-0.8, 0.8))
          val meteor = at.world.spawn(spawn, FallingBlock::class.java) { block ->
            block.blockData = magma
            block.dropItem = false
            block.setHurtEntities(false)
            block.persistentDataContainer.set(plugin.keys.meteor, PersistentDataType.BOOLEAN, true)
            block.velocity = Vector(0.0, -cfg.meteorFallSpeed, 0.0)
          }
          var age = 0L
          Bukkit.getScheduler().runTaskTimer(plugin, { task ->
            if (meteor.isValid && !meteor.isOnGround && ++age < lifetime) return@runTaskTimer
            task.cancel()
            if (age >= lifetime) return@runTaskTimer meteor.remove()
            val hit = meteor.location
            meteor.remove()
            hit.world.spawnParticle(Particle.EXPLOSION, hit, 2)
            hit.world.playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.1f)
            Targets.players(hit, cfg.meteorFireRadius, guard).forEach {
              it.damage(damage, boss.entity)
              it.fireTicks = max(it.fireTicks, cfg.meteorIgniteSeconds * 20)
            }
          }, 1L, 1L)
        }
      }, cfg.meteorWaveIntervalTicks * wave)
    }
    return true
  }

  /** Rings of arrows out of the boss in every direction; at higher levels some carry a negative effect. */
  private fun arrowArc(boss: Boss): Boolean {
    val cfg = settings
    val arrows = Formulas.linear(cfg.arcArrowsBase, cfg.arcArrowsPerLevel, cfg.arcArrowsMax, boss.level).coerceAtLeast(1)
    val damage = Formulas.linear(cfg.arcDamageBase, cfg.arcDamagePerLevel, cfg.arcDamageMax, boss.level)
    val rings = cfg.arcRings.coerceAtLeast(1)
    val effects = if (boss.level >= cfg.arcEffectLevelThreshold) plugin.catalog.arcEffects.filter { it != PotionEffectType.GLOWING } else emptyList()
    val origin = boss.entity.location
    val box = boss.entity.boundingBox
    val height = (box.maxY - box.minY).coerceAtLeast(0.5)
    val step = 2 * PI / arrows

    for (ring in 0 until rings) {
      val y = box.minY + height * (ring + 0.5) / rings
      val phase = if (cfg.arcStaggered && ring % 2 == 1) step / 2 else 0.0
      val tipped = if (effects.isEmpty()) emptySet() else (0 until arrows).shuffled().take(min(cfg.arcEffectsMaxPerRing, arrows)).toSet()
      for (i in 0 until arrows) {
        val angle = i * step + phase
        val direction = Vector(cos(angle), 0.0, sin(angle))
        val at = Location(origin.world, origin.x, y, origin.z).add(direction.clone().multiply(0.3))
        arrow(boss, at, direction.multiply(cfg.arcSpeed), damage, cfg.arcNoGravity, cfg.arcLifetimeSeconds,
          if (i in tipped) effects else emptyList(), cfg.arcEffectDurationSeconds, cfg.arcEffectAmplifierMin, cfg.arcEffectAmplifierMax)
      }
    }
    return true
  }

  /** Waves of arrows falling on a square around the nearest player. */
  private fun arrowRain(boss: Boss): Boolean {
    val cfg = settings
    val target = Targets.nearest(boss.entity.location, cfg.rainTargetSearchRadius, guard) ?: return false
    val arrows = Formulas.linear(cfg.rainArrowsBase, cfg.rainArrowsPerLevel, cfg.rainArrowsMax, boss.level).coerceAtLeast(1)
    val damage = Formulas.linear(cfg.rainDamageBase, cfg.rainDamagePerLevel, cfg.rainDamageMax, boss.level)
    val effects = if (boss.level >= cfg.rainEffectLevelThreshold) plugin.catalog.rainEffects.filter { it != PotionEffectType.GLOWING } else emptyList()
    val half = cfg.rainHalfSize.coerceAtLeast(0.5)
    val fall = cfg.rainFallSpeed.coerceAtLeast(0.1)
    target.world.playSound(target.location, Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.2f)

    repeat(cfg.rainWaves.coerceAtLeast(1)) { wave ->
      Bukkit.getScheduler().runTaskLater(plugin, Runnable {
        if (!boss.entity.isValid || !target.isOnline) return@Runnable
        // Each wave follows the target.
        val center = target.location
        val tipped = if (effects.isEmpty()) emptySet() else (0 until arrows).shuffled().take(min(cfg.rainEffectsMaxPerWave, arrows)).toSet()
        for (i in 0 until arrows) {
          val at = Location(center.world, center.x + Random.nextDouble(-half, half), center.y + cfg.rainHeight, center.z + Random.nextDouble(-half, half))
          arrow(boss, at, Vector(Random.nextDouble(-0.05, 0.05), -fall, Random.nextDouble(-0.05, 0.05)), damage, false, cfg.rainLifetimeSeconds,
            if (i in tipped) effects else emptyList(), cfg.rainEffectDurationSeconds, cfg.rainEffectAmplifierMin, cfg.rainEffectAmplifierMax)
        }
        center.world.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f)
        center.world.spawnParticle(Particle.CLOUD, center, 12, half * 0.2, 0.3, half * 0.2, 0.01)
      }, cfg.rainWaveIntervalTicks.coerceAtLeast(1) * wave)
    }
    return true
  }

  private fun arrow(
    boss: Boss, at: Location, velocity: Vector, damage: Double, noGravity: Boolean, lifetimeSeconds: Int,
    effects: List<PotionEffectType>, effectSeconds: Int, minAmplifier: Int, maxAmplifier: Int,
  ) {
    val arrow = at.world.spawn(at, Arrow::class.java) { a ->
      a.shooter = boss.entity
      a.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
      a.damage = damage
      a.isCritical = false
      if (noGravity) a.setGravity(false)
      effects.randomOrNull()?.let { type ->
        val low = minAmplifier.coerceAtLeast(0)
        val high = maxAmplifier.coerceAtLeast(low)
        a.basePotionType = PotionType.WATER
        a.addCustomEffect(PotionEffect(type, effectSeconds.coerceAtLeast(1) * 20, Random.nextInt(low, high + 1)), true)
      }
    }
    arrow.velocity = velocity
    if (lifetimeSeconds > 0) Bukkit.getScheduler().runTaskLater(plugin, Runnable { if (arrow.isValid) arrow.remove() }, lifetimeSeconds * 20L)
  }

  private fun sound(ability: Ability, at: Location) {
    val (sound, volume, pitch) = when (ability) {
      Ability.LIGHTNING -> Triple(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f)
      Ability.SUMMONING -> Triple(Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f)
      Ability.REGENERATION -> Triple(Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.6f)
      Ability.FIRE_RING -> Triple(Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f)
      Ability.WEB_RING -> Triple(Sound.BLOCK_BAMBOO_PLACE, 0.8f, 1.2f)
      Ability.TELEPORT_STRIKE -> Triple(Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f)
      Ability.METEOR_SHOWER -> Triple(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.3f)
      Ability.ARROW_ARC -> Triple(Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f)
      Ability.ARROW_RAIN -> return
    }
    at.world.playSound(at, sound, volume, pitch)
  }

  private companion object {
    /** What draws lightning, as in 1.x: the rod, copper blocks, and cut copper. */
    val COPPER = setOf(
      Material.COPPER_BLOCK, Material.CUT_COPPER, Material.EXPOSED_COPPER, Material.EXPOSED_CUT_COPPER,
      Material.OXIDIZED_COPPER, Material.OXIDIZED_CUT_COPPER, Material.WAXED_COPPER_BLOCK, Material.WAXED_CUT_COPPER,
      Material.WAXED_EXPOSED_COPPER, Material.WAXED_EXPOSED_CUT_COPPER, Material.WAXED_OXIDIZED_COPPER, Material.WAXED_OXIDIZED_CUT_COPPER,
    )

    val BABIES = setOf(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.PIGLIN, EntityType.ZOMBIE_VILLAGER)

    val NEGATIVE = listOf(
      PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER, PotionEffectType.SLOWNESS,
      PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS,
      PotionEffectType.UNLUCK, PotionEffectType.DARKNESS,
    )
  }
}
