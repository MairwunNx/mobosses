package ru.mobosses.entities

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.Drowned
import org.bukkit.entity.Husk
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.entity.ZombieVillager
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.algebra.childlevel
import ru.mairwunnx.mobosses.algebra.core.scaleDouble
import ru.mairwunnx.mobosses.algebra.core.scaleInt
import ru.mairwunnx.mobosses.algebra.minions
import ru.mairwunnx.mobosses.models.AbilitiesConfigurationModel
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.MinionsConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.managers.BossManager
import ru.mobosses.managers.EffectsManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Перечисление типов способностей для удобной проверки доступности
 */
enum class AbilityType {
  LIGHTNING,
  SUMMONING,
  REGENERATION,
  FIRE_RING,
  WEB_RING,
  TELEPORT_STRIKE,
  METEOR_SHOWER,
  ARROW_ARC,
  ARROW_RAIN
}

/**
 * Extension функция для проверки доступности способности у босса
 */
fun BossEntity.canUseAbility(abilityType: AbilityType): Boolean {
  val threshold = when (abilityType) {
    AbilityType.LIGHTNING -> clazz.abilityThresholds.lightning
    AbilityType.SUMMONING -> clazz.abilityThresholds.summoning
    AbilityType.REGENERATION -> clazz.abilityThresholds.regeneration
    AbilityType.FIRE_RING -> clazz.abilityThresholds.fireRing
    AbilityType.WEB_RING -> clazz.abilityThresholds.webRing
    AbilityType.TELEPORT_STRIKE -> clazz.abilityThresholds.teleportStrike
    AbilityType.METEOR_SHOWER -> clazz.abilityThresholds.meteorShower
    AbilityType.ARROW_ARC -> clazz.abilityThresholds.arrowArc
    AbilityType.ARROW_RAIN -> clazz.abilityThresholds.arrowRain
  }
  return level >= threshold
}

sealed class BossAbility(val name: String) {
  abstract fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager)

  object Lightning : BossAbility("lightning") {
    // ——— список блоков-«приманок» ———
    private val ATTRACTORS = setOf(
      Material.LIGHTNING_ROD,
      Material.COPPER_BLOCK, Material.CUT_COPPER,
      Material.EXPOSED_COPPER, Material.EXPOSED_CUT_COPPER,
      Material.OXIDIZED_COPPER, Material.OXIDIZED_CUT_COPPER,
      Material.WAXED_COPPER_BLOCK, Material.WAXED_CUT_COPPER,
      Material.WAXED_EXPOSED_COPPER, Material.WAXED_EXPOSED_CUT_COPPER,
      Material.WAXED_OXIDIZED_COPPER, Material.WAXED_OXIDIZED_CUT_COPPER
    )

    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      val cfg = plugin.configuration[AbilitiesConfigurationModel::class.java]
      val loc = boss.entity.location
      val lvl = boss.level
      val world = loc.world ?: return

      val finalRadius = min(cfg.lightningRadius + (lvl * cfg.lightningRadiusModifier), cfg.lightningRadiusMax)
      val strikesTotal = scaleInt(cfg.lightningStrikes, cfg.lightningStrikesModifier, cfg.lightningStrikesMax, lvl)
      val dmg = scaleDouble(cfg.lightningDamage, cfg.lightningDamageModifier, cfg.lightningDamageMax, lvl)

      // 1) Ищем «приманку» поблизости
      val searchR = min(5.0, finalRadius)
      var bestTarget: Location? = null
      var bestIsRod = false
      loop@ for (dx in -searchR.toInt()..searchR.toInt()) {
        for (dz in -searchR.toInt()..searchR.toInt()) {
          if (dx * dx + dz * dz > searchR * searchR) continue
          val bx = loc.blockX + dx
          val bz = loc.blockZ + dz
          val by = world.getHighestBlockYAt(bx, bz)
          val block = world.getBlockAt(bx, by, bz)
          if (block.type in ATTRACTORS) {
            val target = block.location.add(0.5, 1.0, 0.5)
            val isRod = block.type == Material.LIGHTNING_ROD
            if (bestTarget == null ||
              (isRod && !bestIsRod) ||
              (target.distanceSquared(loc) < bestTarget.distanceSquared(loc) && isRod == bestIsRod)
            ) {
              bestTarget = target
              bestIsRod = isRod
            }
          }
        }
      }

      // 2) Центр ударов: медь/громоотвод → ближайший игрок → босс
      val strikeCenter: Location = bestTarget
        ?: world.getNearbyEntities(loc, finalRadius, finalRadius, finalRadius)
          .filterIsInstance<Player>()
          .minByOrNull { it.location.distanceSquared(loc) }
          ?.location
        ?: loc

      // 3) Формируем список точек ударов (первая точно по центру)
      val strikes = mutableListOf<Location>()
      strikes += strikeCenter.clone()
      repeat((strikesTotal - 1).coerceAtLeast(0)) {
        val sx = strikeCenter.x + Random.nextDouble(-finalRadius, finalRadius)
        val sz = strikeCenter.z + Random.nextDouble(-finalRadius, finalRadius)
        val sy = world.getHighestBlockYAt(sx.toInt(), sz.toInt()).toDouble() + 1.0
        strikes += Location(world, sx, sy, sz)
      }

      // 4) Визуал + урон по области КАЖДОГО удара (а не вокруг босса)
      val damageRadius = 2.5 // радиус поражения вокруг места удара
      strikes.forEach { p ->
        world.strikeLightningEffect(p) // без поджога окружения; хочешь реальную молнию — замени на strikeLightning(p)
        world.getNearbyEntities(p, damageRadius, damageRadius, damageRadius)
          .filterIsInstance<LivingEntity>()
          .filter { it != boss.entity }
          .forEach { it.damage(dmg, boss.entity) }
      }

      effectsManager.playAbilityEffect(loc, name)
    }
  }

  object SummonMinions : BossAbility("summon_minions") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing SummonMinions ability for boss ${boss.bossType.name} lvl ${boss.level}" }

      val minions = plugin.configuration[MinionsConfigurationModel::class.java]
      val general = plugin.configuration[GeneralConfigurationModel::class.java]

      val minionType = minions.minions.find { it.summoner == boss.entity.type }
      if (minionType == null) {
        plugin.logger.debug { "No minion type found for ${boss.entity.type}, aborting summon" }
        return
      }

      effectsManager.playAbilityEffect(boss.entity.location, name)

      val count = with(general.formula.minions) { minions(boss.level) }
      plugin.logger.debug { "Summoning $count minions of type ${minionType.entity}" }

      repeat(count) {
        val location = boss.entity.location
        val world = location.world ?: return
        val minionEntity = world.spawnEntity(location, minionType.entity) as LivingEntity

        if (bossManager.isBoss(minionEntity)) {
          val curve = general.progression.miniBossesLevelCurve
          val targetLevel = childlevel(boss.level, curve)

          bossManager.relevelBoss(bossManager.getBoss(minionEntity)!!, targetLevel.coerceAtLeast(1))
        }

        minionEntity.customName = minionType.name
        minionEntity.isCustomNameVisible = true

        if (minionEntity is Zombie || minionEntity is Husk || minionEntity is Drowned || minionEntity is Piglin || minionEntity is ZombieVillager) {
          minionEntity.setBaby()
        }
      }

      plugin.logger.debug { "SummonMinions ability completed: spawned $count ${minionType.entity} minions" }
    }
  }

  object Regeneration : BossAbility("regeneration") {
    private const val AURA_SCAN_PERIOD_TICKS = 10L

    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      val cfg = plugin.configuration[AbilitiesConfigurationModel::class.java]
      if (!cfg.isRegenerationEnabled || !boss.canUseAbility(AbilityType.REGENERATION)) return

      val world = boss.entity.world
      val lvl = boss.level

      fun scaleInt(base: Int, perLvl: Double, cap: Int, level: Int) =
        (base + perLvl * level).toInt().coerceIn(1, cap)

      val durSecBase = scaleInt(cfg.regenDurationSecondsBase, cfg.regenDurationSecondsPerLevel, cfg.regenDurationSecondsMax, lvl)
      val durSecBiased = (durSecBase * Random.nextDouble(1.0, cfg.regenerationDurationBias))
        .coerceAtMost(cfg.regenDurationSecondsMax.toDouble())
      val durationTicks = (durSecBiased * 20.0).toInt().coerceAtLeast(1)

      val amp = scaleInt(cfg.regenAmplifierBase, cfg.regenAmplifierPerLevel, cfg.regenAmplifierMax, lvl).coerceAtLeast(0)

      val auraRadius = cfg.regenAuraRadius
      val resistSec = cfg.regenExtraResistanceSeconds
      val cleanse = cfg.regenCleanseNegatives
      val antiProj = cfg.regenAntiProjectiles
      val slowPlayers = cfg.regenSlowPlayers

      val start = boss.entity.location.clone()
      effectsManager.playAbilityEffect(start, name)
      world.playSound(start, Sound.ITEM_TOTEM_USE, 1.0f, 1.05f)
      world.spawnParticle(Particle.TOTEM_OF_UNDYING, start, 40, 0.6, 1.0, 0.6, 0.1)

      if (cleanse) {
        val negatives = setOf(
          PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.HUNGER,
          PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS,
          PotionEffectType.BLINDNESS, PotionEffectType.UNLUCK, PotionEffectType.DARKNESS
        )
        negatives.forEach { boss.entity.removePotionEffect(it) }
        boss.entity.fireTicks = 0
      }

      // Основной Regen на всё время визуалов
      boss.entity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, durationTicks, amp, true, true, true))

      // Короткий рывок Resistance (как было)
      if (resistSec > 0) {
        boss.entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, resistSec * 20, 0, true, true, true))
      }

      // "канальный" Resistance на всё окно визуалов >>
      val channelResistAmplifier = amp.coerceAtMost(2) // 0..2  -> Resistance I..III
      boss.entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, durationTicks, channelResistAmplifier, true, true, true))

      var tick = 0
      lateinit var task: org.bukkit.scheduler.BukkitTask
      task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
        if (!boss.entity.isValid || boss.entity.isDead) {
          // Чистим эффекты при досрочном завершении
          boss.entity.removePotionEffect(PotionEffectType.REGENERATION)
          boss.entity.removePotionEffect(PotionEffectType.RESISTANCE)
          task.cancel()
          return@Runnable
        }

        val base = boss.entity.location
        if (tick % 2 == 0) world.spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(0.0, 1.0, 0.0), 6, 0.6, 0.6, 0.6, 0.0)
        if (tick % 8 == 0) world.spawnParticle(Particle.HEART, base.clone().add(0.0, 1.2, 0.0), 2, 0.2, 0.3, 0.2, 0.0)

        if (tick % AURA_SCAN_PERIOD_TICKS.toInt() == 0) {
          world.getNearbyEntities(base, auraRadius, auraRadius, auraRadius).forEach {
            when (it) {
              is AbstractArrow -> if (antiProj) {
                world.spawnParticle(Particle.SMOKE, it.location, 4, 0.1, 0.1, 0.1, 0.02)
                world.playSound(it.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.6f)
                it.remove()
              }

              is Player -> if (slowPlayers) {
                it.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false, true))
              }
            }
          }
        }

        tick++
        if (tick >= durationTicks) {
          // Жёстко завершаем окно и убираем эффекты
          boss.entity.removePotionEffect(PotionEffectType.REGENERATION)
          boss.entity.removePotionEffect(PotionEffectType.RESISTANCE)
          task.cancel()

          val endLoc = boss.entity.location
          world.playSound(endLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.9f, 1.0f)
          world.spawnParticle(Particle.END_ROD, endLoc, 24, 0.5, 1.0, 0.5, 0.02)
        }
      }, 0L, 1L)
    }
  }

  object FireRing : BossAbility("fire_ring") {
    private const val POINTS = 64               // «64 блока по кругу»
    private const val SEARCH_WINDOW_Y = 8       // ищем поверхность в окне ±8 блоков от центра

    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      val cfg = plugin.configuration[AbilitiesConfigurationModel::class.java]
      val world = boss.entity.world

      val centerEntity = nearestPlayer(world, boss.entity.location, 32.0) ?: boss.entity
      val center = centerEntity.location.clone()
      effectsManager.playAbilityEffect(center, name)

      val radius = Random.nextDouble(cfg.fireRingMinRadius, cfg.fireRingMaxRadius)
      val density = cfg.fireRingDensity.coerceIn(0.0, 1.0)

      plugin.logger.debug { "FireRing center=${centerEntity.name} r=${"%.2f".format(radius)} points=$POINTS density=$density" }

      for (i in 0 until POINTS) {
        if (Random.nextDouble() > density) continue

        val angle = (Math.PI * 2.0) * (i.toDouble() / POINTS.toDouble())
        val x = center.x + radius * cos(angle)
        val z = center.z + radius * sin(angle)

        val surfaceY = findSurfaceNearY(world, x, center.y, z, SEARCH_WINDOW_Y) ?: continue
        val bx = kotlin.math.floor(x).toInt()
        val bz = kotlin.math.floor(z).toInt()
        val fireY = surfaceY + 1

        // не поджигаем собственный хитбокс босса
        val px = bx + 0.5
        val py = fireY + 0.0
        val pz = bz + 0.5
        if (boss.entity.boundingBox.contains(px, py, pz)) continue

        val below = world.getBlockAt(bx, surfaceY, bz)
        val above = world.getBlockAt(bx, fireY, bz)

        if (below.type.isSolid && canReplaceWithFire(above.type)) {
          // снесём траву/цветы, если есть, и поставим огонь
          above.type = Material.FIRE
        }
      }
    }

    private fun nearestPlayer(world: World, at: Location, radius: Double): Player? {
      var best: Player? = null
      var bestD = Double.MAX_VALUE
      for (e in world.getNearbyEntities(at, radius, radius, radius)) {
        if (e is Player && e.isValid && !e.isDead) {
          val d = e.location.distanceSquared(at)
          if (d < bestD) {
            bestD = d; best = e
          }
        }
      }
      return best
    }

    /**
     * Ищем «поверхность» рядом по Y: снизу/сверху в окне ±window.
     * Условие поверхности: блок ниже — твёрдый, блок наверху — воздух или заменяемое растение.
     */
    private fun findSurfaceNearY(world: World, x: Double, startY: Double, z: Double, window: Int): Int? {
      val bx = kotlin.math.floor(x).toInt()
      val bz = kotlin.math.floor(z).toInt()
      val minY = (startY.toInt() - window).coerceAtLeast(world.minHeight + 1)
      val maxY = (startY.toInt() + window).coerceAtMost(world.maxHeight - 2)

      // сначала ищем вниз от старта — ближайшую поверхность над текущим Y
      var y = startY.toInt().coerceIn(minY, maxY)
      while (y >= minY) {
        val below = world.getBlockAt(bx, y - 1, bz)
        val above = world.getBlockAt(bx, y, bz)
        if (below.type.isSolid && canReplaceWithFire(above.type)) return y - 1
        y--
      }
      // затем вверх (если стоим в «воздушном кармане» на листве/лестницах и т.п.)
      y = startY.toInt().coerceIn(minY, maxY)
      while (y <= maxY) {
        val below = world.getBlockAt(bx, y - 1, bz)
        val above = world.getBlockAt(bx, y, bz)
        if (below.type.isSolid && canReplaceWithFire(above.type)) return y - 1
        y++
      }
      return null
    }

    /**
     * Разрешаем ставить огонь в воздух и заменяемые растения/траву/цветы/снег.
     * (Tag.REPLACEABLE — для травы/папоротников и подобного; SMALL_FLOWERS/TALL_FLOWERS — для цветов.)
     */
    private fun canReplaceWithFire(mat: Material): Boolean {
      if (mat == Material.AIR) return true
      return org.bukkit.Tag.REPLACEABLE.isTagged(mat)
        || org.bukkit.Tag.SMALL_FLOWERS.isTagged(mat)
        || mat == Material.SHORT_GRASS
        || mat == Material.FERN
        || mat == Material.LARGE_FERN
        || mat == Material.SNOW // тонкий слой снега
    }
  }

  object WebRing : BossAbility("web_ring") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing WebRing ability for boss ${boss.bossType.name} lvl ${boss.level}" }

      val abilities = plugin.configuration[AbilitiesConfigurationModel::class.java]
      val radius = abilities.webRingRadius
      effectsManager.playAbilityEffect(boss.entity.location, name)

      // Находим ближайшего игрока
      val nearbyPlayers = boss.entity.location.world?.getNearbyEntities(boss.entity.location, 20.0, 20.0, 20.0)
        ?.filterIsInstance<Player>()
      val targetPlayer = nearbyPlayers?.minByOrNull { it.location.distanceSquared(boss.entity.location) }
      if (targetPlayer == null) {
        plugin.logger.debug { "No target player found for WebRing ability" }
        return
      }
      val playerLocation = targetPlayer.location

      val webLocations = mutableListOf<Location>()
      var soundCount = 0

      // Создаем кольцо паутины вокруг игрока
      for (i in 0..360 step 10) {
        val angle = Math.toRadians(i.toDouble())
        val x = playerLocation.x + radius * cos(angle)
        val z = playerLocation.z + radius * sin(angle)
        val webLocation = playerLocation.world?.getHighestBlockAt(x.toInt(), z.toInt())?.location

        if (webLocation != null) {
          val blockAbove = webLocation.clone().add(0.0, 1.0, 0.0)
          val blockType = blockAbove.block.type
          // Заменяем только воздух и траву, не ломаем постройки
          if (blockType == Material.AIR || blockType == Material.SHORT_GRASS || blockType == Material.TALL_GRASS) {
            blockAbove.block.type = Material.COBWEB
            webLocations.add(blockAbove)

            // Звук размещения паутины (каждый 3-й блок чтобы не спамить)
            if (soundCount % 3 == 0) {
              playerLocation.world?.playSound(blockAbove, Sound.BLOCK_WOOL_PLACE, 0.3f, 0.8f)
            }
            soundCount++
          }
        }
      }

      // Применяем эффект замедления игрокам в зоне паутины с прогрессией по уровню
      if (abilities.webRingPotionEnabled) {
        val nearbyPlayersInWeb = playerLocation.world?.getNearbyEntities(playerLocation, radius + 1, radius + 1, radius + 1)
          ?.filterIsInstance<Player>()
        nearbyPlayersInWeb?.forEach { player ->
          // Рассчитываем длительность эффекта с прогрессией
          val baseDuration = abilities.webRingPotionBaseDuration
          val maxDuration = abilities.webRingPotionMaxDuration
          val levelModifier = abilities.webRingPotionLevelModifier

          val duration = min(baseDuration + (boss.level * levelModifier), maxDuration.toDouble())
          val durationTicks = (duration * 20).toInt() // конвертируем секунды в тики

          player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1))
        }
      }

      plugin.logger.debug { "WebRing ability created ${webLocations.size} web blocks around ${targetPlayer.name}" }

      // Удаляем паутину через некоторое время
      Bukkit.getScheduler().runTaskLater(bossManager.plugin, Runnable {
        webLocations.forEach { location ->
          if (location.block.type == Material.COBWEB) {
            location.block.type = Material.AIR
          }
        }
        plugin.logger.debug { "WebRing ability completed: removed ${webLocations.size} web blocks" }
      }, abilities.webRingDuration * 20L) // конвертируем секунды в тики
    }
  }

  object TeleportStrike : BossAbility("teleport_strike") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing TeleportStrike ability for boss ${boss.bossType.name} lvl ${boss.level}" }

      val nearbyPlayers = boss.entity.location.world?.getNearbyEntities(boss.entity.location, 15.0, 15.0, 15.0)
        ?.filterIsInstance<Player>()
      val targetPlayer = nearbyPlayers?.minByOrNull { it.location.distanceSquared(boss.entity.location) }
      if (targetPlayer == null) {
        plugin.logger.debug { "No target player found for TeleportStrike ability" }
        return
      }

      val oldLocation = boss.entity.location.clone()
      effectsManager.playAbilityEffect(oldLocation, name)

      // Эффект эндермена на старом месте
      oldLocation.world?.spawnParticle(Particle.PORTAL, oldLocation, 30, 0.5, 1.0, 0.5, 0.1)
      oldLocation.world?.playSound(oldLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

      // Находим позицию за спиной игрока
      val playerDirection = targetPlayer.location.direction
      val behindPlayer = targetPlayer.location.clone()
        .subtract(playerDirection.multiply(2.0)) // 2 блока за спиной
        .add(0.0, 0.1, 0.0) // немного приподнимаем

      // Убеждаемся что позиция безопасна
      val safeLocation = findSafeLocation(behindPlayer) ?: behindPlayer
      boss.entity.teleport(safeLocation)

      // Звук телепортации в новом месте
      safeLocation.world?.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f)

      plugin.logger.debug { "TeleportStrike ability completed: teleported behind ${targetPlayer.name}" }
    }

    private fun findSafeLocation(location: Location): Location? {
      val world = location.world ?: return null
      for (y in -2..3) {
        val testLocation = location.clone().add(0.0, y.toDouble(), 0.0)
        val block = world.getBlockAt(testLocation)
        val blockAbove = world.getBlockAt(testLocation.clone().add(0.0, 1.0, 0.0))

        if (!block.type.isSolid && !blockAbove.type.isSolid) {
          return testLocation
        }
      }
      return null
    }
  }

  object MeteorShower : BossAbility("meteor_shower") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing MeteorShower ability for boss ${boss.bossType.name} lvl ${boss.level}" }

      val cfg = plugin.configuration[AbilitiesConfigurationModel::class.java]
      if (!cfg.isMeteorShowerEnabled || !boss.canUseAbility(AbilityType.METEOR_SHOWER)) {
        plugin.logger.debug { "MeteorShower ability disabled or level too low for ${boss.clazz.key} class" }
        return
      }

      val lvl = boss.level
      val waves = scaleInt(cfg.wavesBase, cfg.wavesPerLevel, cfg.wavesMax, lvl)
      val meteorsPerWave = scaleInt(cfg.meteorsPerWaveBase, cfg.meteorsPerWavePerLevel, cfg.meteorsPerWaveMax, lvl)
      val radius = scaleDouble(cfg.radiusBase, cfg.radiusPerLevel, cfg.radiusMax, lvl)
      val impactDamage = scaleDouble(cfg.impactDamageBase, cfg.impactDamagePerLevel, cfg.impactDamageMax, lvl)

      val world = boss.entity.world
      val center = boss.entity.location.clone()
      val lifetimeTicks = (cfg.meteorLifetimeSeconds.coerceAtLeast(1)) * 20L

      fun validPlayers(): List<Player> =
        world.players.filter { it.gameMode != GameMode.SPECTATOR && it.isOnline && !it.isDead && it.world == world }

      // 1) Ищем ближайшего в радиусе; если нет — ближайшего вообще; если всё равно нет — выходим
      val nearby = world.getNearbyEntities(center, radius, radius, radius).filterIsInstance<Player>()
        .filter { it.gameMode != GameMode.SPECTATOR && it.isOnline && !it.isDead && it.world == world }

      var target: Player? = (nearby.ifEmpty { validPlayers() })
        .minByOrNull { it.location.distanceSquared(center) }

      if (target == null) {
        plugin.logger.debug { "No targets found for MeteorShower ability" }
        return
      }

      plugin.logger.debug {
        "MeteorShower parameters: waves=$waves, meteors_per_wave=$meteorsPerWave, radius=${radius.toInt()}, target=${target.name}"
      }
      effectsManager.playAbilityEffect(center, name)

      // Вспомогалка: актуальная точка при спавне (с небольшим разбросом)
      fun currentTargetLocOrRetarget(): Location? {
        val t = target
        if (t != null && t.isOnline && !t.isDead && t.world == world) return t.location
        // Попытка ретаргета (на ближайшего к боссу в радиусе, иначе по миру)
        target = (nearby.ifEmpty { validPlayers() })
          .minByOrNull { it.location.distanceSquared(center) }
        return target?.location
      }

      repeat(waves) { waveIdx ->
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
          repeat(meteorsPerWave) {
            val tLoc = currentTargetLocOrRetarget() ?: return@repeat
            val ox = Random.nextDouble(-0.8, 0.8)
            val oz = Random.nextDouble(-0.8, 0.8)

            val sx = tLoc.x + ox
            val sz = tLoc.z + oz
            val sy = tLoc.y + cfg.meteorShowerHeight

            val spawn = Location(world, sx, sy, sz)
            val falling = world.spawnFallingBlock(spawn, Material.MAGMA_BLOCK.createBlockData()).apply {
              dropItem = false
              setHurtEntities(false)
              velocity = Vector(0.0, -cfg.fallSpeed, 0.0)
            }

            // Автодеспавн
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
              if (!falling.isDead && falling.isValid) falling.remove()
            }, lifetimeTicks)

            // Тикер приземления
            val taskId = AtomicInteger(-1)
            taskId.set(Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
              if (falling.isDead || falling.isOnGround) {
                val hit = falling.location
                world.spawnParticle(Particle.EXPLOSION, hit, 2)
                world.playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.1f)

                world.getNearbyEntities(hit, cfg.fireRadius, cfg.fireRadius, cfg.fireRadius).forEach { e ->
                  if (e is LivingEntity && e != boss.entity) {
                    e.damage(impactDamage, boss.entity)
                    e.fireTicks = max(e.fireTicks, cfg.igniteSeconds * 20)
                  }
                }

                falling.remove()
                Bukkit.getScheduler().cancelTask(taskId.get())
              }
            }, 1L, 1L).taskId)
          }
        }, cfg.waveIntervalTicks * waveIdx)
      }

      plugin.logger.debug {
        "MeteorShower ability initiated: $waves waves with $meteorsPerWave meteors each targeting ${target?.name ?: "none"}"
      }
    }
  }

  object ArrowArc : BossAbility("arrow_arc") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing ArrowArc ability for boss ${boss.bossType.name} lvl ${boss.level}" }

      val root = plugin.configuration[AbilitiesConfigurationModel::class.java]
      if (!root.isArrowArcEnabled || !boss.canUseAbility(AbilityType.ARROW_ARC)) {
        plugin.logger.debug { "ArrowArc ability disabled or level too low for ${boss.clazz.key} class" }
        return
      }

      val lvl = boss.level

      val arrows = scaleInt(root.arrowsBase, root.arrowsPerLevel, root.arrowsMax, lvl)
      val damage = scaleDouble(root.damageBase, root.damagePerLevel, root.damageMax, lvl)
      val lifetimeTicks = if (root.arrowLifetimeSeconds > 0) root.arrowLifetimeSeconds * 20L else -1L
      val rings = root.rings.coerceAtLeast(1)

      val world = boss.entity.world
      val origin = boss.entity.location.clone()
      val box = boss.entity.boundingBox
      val height = (box.maxY - box.minY).coerceAtLeast(0.5)

      // допустимые негативные эффекты (без spectral/glowing)
      val allowedEffects = if (lvl >= root.randomEffectLevelThreshold)
        root.negativeEffects.filterNot { it === PotionEffectType.GLOWING }
      else emptyList()

      val effectDuration = root.effectDurationSeconds.coerceAtLeast(1) * 20
      val effectAmplMin = root.effectAmplifierMin.coerceAtLeast(0)
      val effectAmplMax = root.effectAmplifierMax.coerceAtLeast(effectAmplMin)

      effectsManager.playAbilityEffect(origin, name)
      plugin.logger.debug { "ArrowArc parameters: arrows=${arrows}, rings=${rings}, damage=${damage}" }

      val ringYs = (0 until rings).map { i ->
        val t = (i + 0.5) / rings.toDouble()
        box.minY + height * t
      }

      val stepRad = (Math.PI * 2.0) / arrows
      val speed = root.speed

      ringYs.forEachIndexed { ringIndex, ringY ->
        val ringOrigin = Location(world, origin.x, ringY, origin.z)
        val phase = if (root.staggered && ringIndex % 2 == 1) stepRad / 2.0 else 0.0

        // не более 16 «ядовитых» на кольцо (или что в конфиге)
        val badCount = if (allowedEffects.isEmpty()) 0 else min(root.randomEffectsMaxPerRing, arrows)
        val badIndexes: Set<Int> = if (badCount > 0)
          (0 until arrows).shuffled().take(badCount).toSet()
        else emptySet()

        for (i in 0 until arrows) {
          val angle = i * stepRad + phase
          val dir = Vector(cos(angle), 0.0, sin(angle)).normalize()
          val spawnLoc = ringOrigin.clone().add(dir.clone().multiply(0.3))

          val useEffect = i in badIndexes && allowedEffects.isNotEmpty()

          val arrow = world.spawn(spawnLoc, Arrow::class.java) { a ->
            a.shooter = boss.entity
            a.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            a.damage = damage
            a.isCritical = false
            if (root.noGravity) a.setGravity(false)

            if (useEffect) {
              a.basePotionType = PotionType.WATER
              val type = allowedEffects.random()
              val amp = if (effectAmplMax > effectAmplMin) Random.nextInt(effectAmplMin, effectAmplMax + 1) else effectAmplMin
              a.addCustomEffect(PotionEffect(type, effectDuration, amp), true)
            }
          }

          arrow.velocity = dir.multiply(speed)

          if (lifetimeTicks > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
              if (!arrow.isDead && arrow.isValid) arrow.remove()
            }, lifetimeTicks)
          }
        }
      }

      plugin.logger.debug { "ArrowArc ability completed: fired ${arrows * rings} arrows in $rings rings" }
    }
  }

  object ArrowRain : BossAbility("arrow_rain") {
    override fun execute(plugin: PluginUnit, boss: BossEntity, bossManager: BossManager, effectsManager: EffectsManager) {
      plugin.logger.debug { "Executing ArrowRain for ${boss.bossType.name} lvl ${boss.level}" }

      val cfg = plugin.configuration[AbilitiesConfigurationModel::class.java]
      if (!cfg.isArrowRainEnabled || !boss.canUseAbility(AbilityType.ARROW_RAIN)) {
        plugin.logger.debug { "ArrowRain disabled or level too low for ${boss.clazz.key} class" }
        return
      }

      val world = boss.entity.world
      val lvl = boss.level

      // Ищем ближайшую цель
      val searchR = cfg.arrowRainTargetSearchRadius
      val centerTarget = world.players
        .asSequence()
        .filter { it.gameMode != GameMode.SPECTATOR && it.isValid && !it.isDead }
        .minByOrNull { it.location.distanceSquared(boss.entity.location) }
        ?.takeIf { it.location.distanceSquared(boss.entity.location) <= (searchR * searchR) }
        ?: run {
          plugin.logger.debug { "ArrowRain skipped: no target within $searchR blocks" }
          return
        }

      val arrows = scaleInt(cfg.arrowRainArrowsBase, cfg.arrowRainArrowsPerLevel, cfg.arrowRainArrowsMax, lvl)
      val waves = cfg.arrowRainWaves.coerceAtLeast(1)
      val waveInterval = cfg.arrowRainWaveIntervalTicks.coerceAtLeast(1L)
      val damage = scaleDouble(cfg.arrowRainDamageBase, cfg.arrowRainDamagePerLevel, cfg.arrowRainDamageMax, lvl)

      val height = cfg.arrowRainHeight
      val halfSize = cfg.arrowRainHalfSize
      val fallSpeed = cfg.arrowRainFallSpeed.coerceAtLeast(0.1)

      val lifetimeTicks = if (cfg.arrowRainLifetimeSeconds > 0) cfg.arrowRainLifetimeSeconds * 20L else -1L

      // Допустимые негативные эффекты
      val allowedEffects = if (lvl >= cfg.arrowRainRandomEffectLevelThreshold)
        cfg.arrowRainNegativeEffects.filterNot { it === PotionEffectType.GLOWING }
      else emptyList()

      val effectDuration = cfg.arrowRainEffectDurationSeconds.coerceAtLeast(1) * 20
      val effectAmplMin = cfg.arrowRainEffectAmplifierMin.coerceAtLeast(0)
      val effectAmplMax = cfg.arrowRainEffectAmplifierMax.coerceAtLeast(effectAmplMin)

      // Визуал/звук старта у цели
      val origin = centerTarget.location.clone()
      effectsManager.playAbilityEffect(origin, name)
      world.playSound(origin, Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.2f)

      plugin.logger.debug {
        "ArrowRain params: waves=$waves, arrows_per_wave=$arrows, area=${(halfSize * 2).toInt()}x${(halfSize * 2).toInt()}, height=$height, dmg=$damage"
      }

      repeat(waves) { waveIdx ->
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
          val centerNow = centerTarget.location.clone() // «преследуем» цель по волнам

          // заранее выбираем индексы токсичных стрел
          val badCount = if (allowedEffects.isEmpty()) 0 else minOf(cfg.arrowRainRandomEffectsMaxPerWave, arrows)
          val badIndexes = if (badCount > 0) (0 until arrows).shuffled().take(badCount).toSet() else emptySet()

          for (i in 0 until arrows) {
            val ox = Random.nextDouble(-halfSize, halfSize)
            val oz = Random.nextDouble(-halfSize, halfSize)
            val sx = centerNow.x + ox
            val sz = centerNow.z + oz
            val sy = centerNow.y + height

            val spawn = Location(world, sx, sy, sz)

            val useEffect = i in badIndexes && allowedEffects.isNotEmpty()

            val arrow = world.spawn(spawn, Arrow::class.java) { a ->
              a.shooter = boss.entity
              a.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
              a.damage = damage
              a.isCritical = false
              a.setGravity(true) // дождь стрел — пускай падают естественно

              if (useEffect) {
                // превращаем в «tipped» без устаревших классов
                a.basePotionType = PotionType.WATER
                val type = allowedEffects.random()
                val amp = if (effectAmplMax > effectAmplMin) Random.nextInt(effectAmplMin, effectAmplMax + 1) else effectAmplMin
                a.addCustomEffect(PotionEffect(type, effectDuration, amp), true)
              }
            }

            // вертикальная скорость вниз (+ чутка разброса для «живости»)
            arrow.velocity = Vector(
              Random.nextDouble(-0.05, 0.05),
              -fallSpeed,
              Random.nextDouble(-0.05, 0.05)
            )

            if (lifetimeTicks > 0) {
              Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (!arrow.isDead && arrow.isValid) arrow.remove()
              }, lifetimeTicks)
            }
          }

          world.playSound(centerNow, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f)
          world.spawnParticle(Particle.CLOUD, centerNow, 12, halfSize * 0.2, 0.3, halfSize * 0.2, 0.01)
        }, waveInterval * waveIdx)
      }
    }
  }
}