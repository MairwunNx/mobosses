package ru.mobosses.managers

import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.EntityType.CAVE_SPIDER
import org.bukkit.entity.EntityType.CREEPER
import org.bukkit.entity.EntityType.DROWNED
import org.bukkit.entity.EntityType.ENDERMAN
import org.bukkit.entity.EntityType.EVOKER
import org.bukkit.entity.EntityType.HUSK
import org.bukkit.entity.EntityType.PIGLIN
import org.bukkit.entity.EntityType.PILLAGER
import org.bukkit.entity.EntityType.SKELETON
import org.bukkit.entity.EntityType.SPIDER
import org.bukkit.entity.EntityType.STRAY
import org.bukkit.entity.EntityType.VINDICATOR
import org.bukkit.entity.EntityType.WITCH
import org.bukkit.entity.EntityType.WITHER_SKELETON
import org.bukkit.entity.EntityType.ZOMBIE
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.mairwunnx.mobosses.algebra.armorPoints
import ru.mairwunnx.mobosses.algebra.armorToughness
import ru.mairwunnx.mobosses.algebra.bosslevel
import ru.mairwunnx.mobosses.algebra.damage
import ru.mairwunnx.mobosses.algebra.health
import ru.mairwunnx.mobosses.algebra.playerlevel
import ru.mairwunnx.mobosses.models.AbilitiesConfigurationModel
import ru.mairwunnx.mobosses.models.BossesConfigurationModel
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel.BossClass
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.MessagesConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.AbilityType
import ru.mobosses.entities.BossAbility
import ru.mobosses.entities.BossEntity
import ru.mobosses.entities.canUseAbility
import java.util.UUID
import kotlin.random.Random
import kotlin.random.Random.Default

class BossManager(
  val plugin: PluginUnit,
  val bossBarManager: BossBarManager,
  private val effectsManager: EffectsManager,
  private val progressionManager: ProgressionManager,
  private val messages: MessageManager
) {
  private val bosses = plugin.configuration[BossesConfigurationModel::class.java]
  private val generalSettings = plugin.configuration[GeneralConfigurationModel::class.java]
  private val messagesSettings = plugin.configuration[MessagesConfigurationModel::class.java]
  private val abilitiesSettings = plugin.configuration[AbilitiesConfigurationModel::class.java]

  private val activeBosses = mutableMapOf<UUID, BossEntity>()
  val abilitiesManager = AbilitiesManager()

  private fun getBossConfig(entityType: EntityType): BossesConfigurationModel.BossConfig? {
    return when (entityType) {
      ZOMBIE -> bosses.zombie.takeIf { it.enabled }
      SKELETON -> bosses.skeleton.takeIf { it.enabled }
      PIGLIN -> bosses.piglin.takeIf { it.enabled }
      PILLAGER -> bosses.pillager.takeIf { it.enabled }
      SPIDER -> bosses.spider.takeIf { it.enabled }
      ENDERMAN -> bosses.enderman.takeIf { it.enabled }
      WITHER_SKELETON -> bosses.witherSkeleton.takeIf { it.enabled }
      VINDICATOR -> bosses.vindicator.takeIf { it.enabled }
      STRAY -> bosses.stray.takeIf { it.enabled }
      CREEPER -> bosses.creeper.takeIf { it.enabled }
      HUSK -> bosses.husk.takeIf { it.enabled }
      WITCH -> bosses.witch.takeIf { it.enabled }
      DROWNED -> bosses.drowned.takeIf { it.enabled }
      EVOKER -> bosses.evoker.takeIf { it.enabled }
      else -> null
    }
  }

  inner class AbilitiesManager {
    private val abilityCooldowns = mutableMapOf<Pair<UUID, String>, Long>()
    private val generalCooldowns = mutableMapOf<UUID, Long>()

    fun useRandomAbility(boss: BossEntity, attacker: Player) {
      val availableAbilities = getAvailableAbilities(boss)
      availableAbilities.filter { canUseAbility(boss, it) }.randomOrNull()?.let { ability ->
        ability.execute(plugin, boss, this@BossManager, effectsManager)
        setCooldown(boss, ability)
        val placeholders = mapOf("boss_name" to boss.bossType.name)

        val msg = when (ability) {
          is BossAbility.FireRing -> messagesSettings.boss.abilities.fireRing
          is BossAbility.Lightning -> messagesSettings.boss.abilities.lightning
          is BossAbility.Regeneration -> messagesSettings.boss.abilities.regeneration
          is BossAbility.SummonMinions -> messagesSettings.boss.abilities.summonMinions
          is BossAbility.TeleportStrike -> messagesSettings.boss.abilities.teleportStrike
          is BossAbility.WebRing -> messagesSettings.boss.abilities.webRing
          is BossAbility.MeteorShower -> messagesSettings.boss.abilities.meteorShower
          is BossAbility.ArrowArc -> messagesSettings.boss.abilities.arrowArc
          is BossAbility.ArrowRain -> messagesSettings.boss.abilities.arrowRain
        }
        messages.whisper(attacker, msg, placeholders)
      }
    }

    private fun getAvailableAbilities(boss: BossEntity): List<BossAbility> {
      return listOfNotNull(
        BossAbility.Lightning.takeIf {
          abilitiesSettings.isLightningEnabled && boss.canUseAbility(AbilityType.LIGHTNING)
        },
        BossAbility.SummonMinions.takeIf {
          abilitiesSettings.isSummoningEnabled && boss.canUseAbility(AbilityType.SUMMONING)
        },
        BossAbility.Regeneration.takeIf {
          abilitiesSettings.isRegenerationEnabled && boss.canUseAbility(AbilityType.REGENERATION)
        },
        BossAbility.FireRing.takeIf {
          abilitiesSettings.isFireRingEnabled && boss.canUseAbility(AbilityType.FIRE_RING)
        },
        BossAbility.WebRing.takeIf {
          abilitiesSettings.isWebRingEnabled && boss.canUseAbility(AbilityType.WEB_RING)
        },
        BossAbility.TeleportStrike.takeIf {
          abilitiesSettings.isTeleportStrikeEnabled &&
            boss.canUseAbility(AbilityType.TELEPORT_STRIKE) &&
            boss.entity.type != SPIDER && boss.entity.type != CAVE_SPIDER
        },
        BossAbility.MeteorShower.takeIf {
          abilitiesSettings.isMeteorShowerEnabled && boss.canUseAbility(AbilityType.METEOR_SHOWER)
        },
        BossAbility.ArrowArc.takeIf {
          abilitiesSettings.isArrowArcEnabled && boss.canUseAbility(AbilityType.ARROW_ARC)
        },
        BossAbility.ArrowRain.takeIf {
          abilitiesSettings.isArrowRainEnabled && boss.canUseAbility(AbilityType.ARROW_RAIN)
        }
      )
    }

    private fun canUseAbility(boss: BossEntity, ability: BossAbility): Boolean {
      val currentTime = System.currentTimeMillis()

      val lastGeneralUsed = generalCooldowns[boss.uuid] ?: 0L
      val generalCooldown = (abilitiesSettings.generalCooldown * Random.nextDouble(1.0, abilitiesSettings.generalCooldownBias)).toLong()
      if (currentTime - lastGeneralUsed <= generalCooldown) return false

      val (cooldown, bias) = when (ability) {
        is BossAbility.Lightning -> abilitiesSettings.lightningCooldown to abilitiesSettings.lightningCooldownBias
        is BossAbility.SummonMinions -> abilitiesSettings.summoningCooldown to abilitiesSettings.summoningCooldownBias
        is BossAbility.Regeneration -> abilitiesSettings.regenerationCooldown to abilitiesSettings.regenerationCooldownBias
        is BossAbility.FireRing -> abilitiesSettings.fireRingCooldown to abilitiesSettings.fireRingCooldownBias
        is BossAbility.WebRing -> abilitiesSettings.webRingCooldown to abilitiesSettings.webRingCooldownBias
        is BossAbility.TeleportStrike -> abilitiesSettings.teleportStrikeCooldown to abilitiesSettings.teleportStrikeCooldownBias
        is BossAbility.MeteorShower -> abilitiesSettings.meteorShowerCooldown to abilitiesSettings.meteorShowerCooldownBias
        is BossAbility.ArrowArc -> abilitiesSettings.arrowArcCooldown to abilitiesSettings.arrowArcCooldownBias
        is BossAbility.ArrowRain -> abilitiesSettings.arrowRainCooldown to abilitiesSettings.arrowRainCooldownBias
      }

      val lastUsed = abilityCooldowns[boss.uuid to ability.name] ?: 0L
      val finalCooldown = (cooldown * Random.nextDouble(1.0, bias)).toLong()
      return currentTime - lastUsed > finalCooldown
    }

    private fun setCooldown(boss: BossEntity, ability: BossAbility) {
      val currentTime = System.currentTimeMillis()
      abilityCooldowns[boss.uuid to ability.name] = currentTime
      generalCooldowns[boss.uuid] = currentTime
    }

    fun clearCooldowns(bossUuid: UUID) {
      // Удаляем все кулдауны для данного босса
      abilityCooldowns.keys.removeIf { it.first == bossUuid }
      generalCooldowns.remove(bossUuid)
    }

    fun clearAllCooldowns() {
      // Очищаем все кулдауны при перезагрузке плагина
      abilityCooldowns.clear()
      generalCooldowns.clear()
    }
  }

  fun handleMonsterSpawn(entity: LivingEntity) {
    if (!entity.hasAI() || entity !is Monster) return

    val config = generalSettings
    if (!config.spawn.enabled) {
      plugin.logger.debug { "Boss spawning is disabled in config" }
      return
    }

    if (activeBosses.size >= config.spawn.maxBossesPerWorld) {
      plugin.logger.debug { "Max bosses per world reached (${activeBosses.size}/${config.spawn.maxBossesPerWorld})" }
      return
    }

    val radius = config.spawn.spawnRadius.toDouble()
    val nearestPlayer = entity.world.getNearbyEntities(entity.location, radius, radius, radius)
      .filterIsInstance<Player>()
      .minByOrNull { it.location.distanceSquared(entity.location) } ?: return

    if (config.spawn.checkVerticalZone) {
      plugin.logger.warn { "Checking vertical zone for spawning is experimental! May have errors!" }

      val threshold = config.spawn.verticalZoneThreshold
      val highestBlockYPlayer = nearestPlayer.world.getHighestBlockYAt(nearestPlayer.location)
      val highestBlockYEntity = entity.world.getHighestBlockYAt(entity.location)

      val isPlayerOnSurface = nearestPlayer.location.y >= highestBlockYPlayer - threshold
      val isEntityOnSurface = entity.location.y >= highestBlockYEntity - threshold

      if (isPlayerOnSurface != isEntityOnSurface) {
        return // Отменяем спавн, если игрок и моб в разных зонах (один на поверхности, другой под землей)
      }
    }

    val bossConfig = getBossConfig(entity.type) ?: return
    val worldName = entity.location.world?.name ?: return
    val worldMultiplier = config.spawn.worldMultipliers[worldName] ?: 1.0
    val time = entity.world.time
    val timeMultiplier = when {
      time in 0..12999 -> config.spawn.timeMultipliers.day
      else -> config.spawn.timeMultipliers.night
    }

    val weatherMultiplier = when {
      entity.world.isThundering -> config.spawn.weatherMultipliers.thunder
      entity.world.hasStorm() -> config.spawn.weatherMultipliers.rain
      else -> config.spawn.weatherMultipliers.clear
    }

    val finalChance = bossConfig.spawnChance * worldMultiplier * timeMultiplier * weatherMultiplier

    plugin.logger.debug { "Boss spawn chance calculation for ${entity.type}: base=${bossConfig.spawnChance}, final=${finalChance} (world=${worldMultiplier}, time=${timeMultiplier}, weather=${weatherMultiplier})" }

    if (Random.nextDouble() < finalChance) {
      plugin.logger.debug { "Transforming ${entity.type} into boss near player ${nearestPlayer.name}" }
      transformIntoBoss(entity, bossConfig, nearestPlayer)
    }
  }

  private fun transformIntoBoss(entity: LivingEntity, bossConfig: BossesConfigurationModel.BossConfig, player: Player) {
    val level = calculateBossLevel(player)
    plugin.logger.debug { "Transforming ${entity.type} into lvl ${level} boss for player ${player.name}" }

    val healthCfg = generalSettings.formula.health
    val damageCfg = generalSettings.formula.damage

    val clazz = getRandomClass()
    plugin.logger.debug { "Selected boss class: ${clazz.displayName} (${clazz.chance} spawn weight)" }

    val defaultMaxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
    val defaultDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!!.baseValue
    val health = with(healthCfg) { health(baseHealth = defaultMaxHealth, classHealthMul = clazz.health, level = level) }
    val damage = with(damageCfg) { damage(baseDamage = defaultDamage, classDamageMul = clazz.health, level = level) }

    val bossEntity = BossEntity(entity, bossConfig, clazz, level)

    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = health
    entity.health = health
    entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = damage
    val rarityColor = clazz.color
    val bossName = "$rarityColor[Lv. $level] [${clazz.displayName}] ${bossConfig.name}"
    entity.customName = bossName
    entity.isCustomNameVisible = true

    if (entity is org.bukkit.entity.Creeper && bossConfig.isPowered == true) {
      entity.isPowered = true
    }

    entity.isPersistent = true
    entity.location.chunk.isForceLoaded = true

    equipBoss(entity, bossConfig, level, clazz)

    activeBosses[entity.uniqueId] = bossEntity
    plugin.logger.info { "✅ Boss spawned: ${bossConfig.name} (${entity.type}) lvl ${level} [${clazz.displayName}] at ${entity.location.world?.name} (${entity.location.blockX}, ${entity.location.blockY}, ${entity.location.blockZ})" }
  }

  fun relevelBoss(target: BossEntity, newLevel: Int) {
    val level = newLevel
    val ent = target.entity
    if (ent.isDead || !ent.isValid) return
    val prevLevel = target.level
    val lvl = newLevel.coerceAtLeast(1)
    if (lvl == prevLevel) return

    val healthCfg = generalSettings.formula.health
    val damageCfg = generalSettings.formula.damage

    val clazz = getRandomClass()
    plugin.logger.debug { "Selected releveled boss class: ${clazz.displayName} (${clazz.chance} spawn weight)" }

    val defaultMaxHealth = 10.0
    val defaultDamage = 5.0
    val health = with(healthCfg) { health(baseHealth = defaultMaxHealth, classHealthMul = clazz.health, level = level) }
    val damage = with(damageCfg) { damage(baseDamage = defaultDamage, classDamageMul = clazz.health, level = level) }

    val bossConfig = target.bossType
    val entity = target.entity
    val bossEntity = BossEntity(entity, bossConfig, clazz, level)

    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = health
    entity.health = health
    entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = damage

    if (entity is org.bukkit.entity.Creeper && bossConfig.isPowered == true) {
      entity.isPowered = true
    }

    entity.isPersistent = true
    entity.location.chunk.isForceLoaded = true

    equipBoss(entity, bossConfig, level, clazz)

    activeBosses[entity.uniqueId] = bossEntity // это заменит текущего босса, без создания нового. uuid то не меняелся
    plugin.logger.info { "✅ Boss releveled: ${bossConfig.name} (${entity.type}) lvl ${level} [${clazz.displayName}] at ${entity.location.world?.name} (${entity.location.blockX}, ${entity.location.blockY}, ${entity.location.blockZ})" }
  }

  private fun equipBoss(entity: LivingEntity, bossConfig: BossesConfigurationModel.BossConfig, bossLevel: Int, bossClass: BossClass) {
    val equipment = entity.equipment
    if (equipment == null) {
      plugin.logger.debug { "No equipment slot available for ${entity.type}" }
      return
    }

    plugin.logger.debug { "Equipping boss ${entity.type} lvl ${bossLevel}" }

    // Оружие
    bossConfig.equipment?.weapons
      ?.filter { it.minLvl <= bossLevel }
      ?.takeIf { it.isNotEmpty() }
      ?.let { availableWeapons ->
        val maxLvl = availableWeapons.maxOf { it.minLvl }
        availableWeapons
          .filter { it.minLvl >= maxLvl - 7 }
          .randomOrNull()
          ?.let { weaponInfo ->
            val item = ItemStack(weaponInfo.material)
            weaponInfo.enchantments?.forEach { (enchant, level) ->
              item.addUnsafeEnchantment(enchant, level)
            }
            equipment.setItemInMainHand(item)
          }
      }

    // Броня
    var hasArmor = false
    bossConfig.equipment?.armors
      ?.filter { it.minLvl <= bossLevel }
      ?.takeIf { it.isNotEmpty() }
      ?.let { availableArmors ->
        val maxLvl = availableArmors.maxOf { it.minLvl }
        availableArmors
          .filter { it.minLvl >= maxLvl - 7 }
          .randomOrNull()
          ?.let { armorSet ->
            equipment.helmet = createArmorPiece(armorSet.helmet)
            equipment.chestplate = createArmorPiece(armorSet.chestplate)
            equipment.leggings = createArmorPiece(armorSet.leggings)
            equipment.boots = createArmorPiece(armorSet.boots)
            hasArmor = true
          }
      }

    // Применяем базовые атрибуты защиты, если нет экипированной брони
    if (!hasArmor) {
      applyArmorAttributes(entity, bossLevel, bossClass)
    }
  }

  private fun createArmorPiece(piece: BossesConfigurationModel.ArmorPiece?): ItemStack? {
    piece ?: return null
    val item = ItemStack(piece.material)
    piece.enchantments?.forEach { (enchant, level) ->
      item.addUnsafeEnchantment(enchant, level)
    }
    return item
  }

  /**
   * Применяет базовые атрибуты защиты для мобов без экипированной брони
   */
  private fun applyArmorAttributes(entity: LivingEntity, bossLevel: Int, bossClass: BossClass) {
    val armorFormula = generalSettings.formula.armor
    val toughnessFormula = generalSettings.formula.toughness

    // Базовые значения защиты для мобов
    val baseArmor = entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue ?: plugin.logger.warn { "Can't get generic armor value, fallback is 4" }.let { 4.0 }
    val baseToughness =
      entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.baseValue ?: plugin.logger.warn { "Can't get generic armor toughness value, fallback is 4" }.let { 4.0 }

    // Вычисляем значения через формулы
    val armorPointsValue = with(armorFormula) { armorPoints(baseArmor, bossClass.defense, bossLevel) }
    val toughnessPointsValue = with(toughnessFormula) { armorToughness(baseToughness, bossClass.toughness, bossLevel) }

    // Применяем атрибуты
    entity.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = armorPointsValue
    entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.baseValue = toughnessPointsValue

    plugin.logger.debug {
      "Applied armor attributes to ${entity.type} lvl $bossLevel [${bossClass.displayName}]: " +
        "armor=${String.format("%.1f", armorPointsValue)}, toughness=${String.format("%.1f", toughnessPointsValue)}"
    }
  }

  private fun getRandomClass(): BossClass {
    val classes = plugin.configuration[ClassesConfigurationModel::class.java]

    val availableClasses = listOf(classes.common, classes.uncommon, classes.rare, classes.epic, classes.legendary, classes.mythic)

    val totalWeight = availableClasses.sumOf { it.chance }
    if (totalWeight <= 0.0) {
      return classes.common
    }

    val random = Random.nextDouble() * totalWeight
    var cumulative = 0.0

    availableClasses.forEach { classData ->
      cumulative += classData.chance
      if (random < cumulative) {
        return classData
      }
    }

    return classes.common
  }

  private fun calculateBossLevel(player: Player): Int {
    val playerProgress = progressionManager.getProgress(player)

    val playerLvl = with(generalSettings.progression.levelCurve) { playerlevel(playerProgress.totalExperience, playerProgress.bossKills) }
    val bossLvl = with(generalSettings.progression.variance) { bosslevel(playerLvl, Default) }

    return bossLvl
  }

  fun activeBosses() = activeBosses
  fun isBoss(entity: LivingEntity): Boolean = activeBosses.containsKey(entity.uniqueId)

  fun getBoss(entity: LivingEntity): BossEntity? = activeBosses[entity.uniqueId]

  fun removeBoss(entity: LivingEntity) {
    val boss = activeBosses.remove(entity.uniqueId)
    if (boss != null) {
      plugin.logger.debug { "Removing boss ${boss.bossType.name} lvl ${boss.level} (UUID: ${entity.uniqueId})" }

      entity.location.chunk.isForceLoaded = false
      effectsManager.removeGlowEffect(entity, boss)
      bossBarManager.removeBossBar(boss)
      abilitiesManager.clearCooldowns(entity.uniqueId)

      plugin.logger.debug { "Boss ${boss.bossType.name} successfully removed" }
    } else {
      plugin.logger.debug { "Attempted to remove non-boss entity ${entity.type} (UUID: ${entity.uniqueId})" }
    }
  }

  /**
   * Принудительно спавнит босса с указанными параметрами в указанном месте
   */
  fun spawnBoss(player: Player, entityType: EntityType, level: Int, bossClass: BossClass): Boolean {
    plugin.logger.debug { "Manual boss spawn requested: ${entityType} lvl ${level} [${bossClass.displayName}] by ${player.name}" }

    val bossConfig = getBossConfig(entityType)
    if (bossConfig == null) {
      plugin.logger.warn { "No boss configuration found for entity type ${entityType}" }
      return false
    }

    val world = player.world
    val location = player.location.add(0.0, 0.0, 2.0) // Спавним перед игроком

    val entity = world.spawnEntity(location, entityType) as? LivingEntity
    if (entity == null) {
      plugin.logger.warn { "Failed to spawn entity of type ${entityType}" }
      return false
    }

    // Принудительно трансформируем в босса с заданными параметрами
    transformIntoBossManual(entity, bossConfig, level, bossClass)

    plugin.logger.info { "✅ Manual boss spawn successful: ${bossConfig.name} lvl ${level} [${bossClass.displayName}] at ${location.world?.name}" }
    return true
  }

  /**
   * Ручная трансформация в босса с заданными параметрами (для команды spawn)
   */
  private fun transformIntoBossManual(
    entity: LivingEntity,
    bossConfig: BossesConfigurationModel.BossConfig,
    level: Int,
    clazz: BossClass
  ) {
    val healthCfg = generalSettings.formula.health
    val damageCfg = generalSettings.formula.damage

    val defaultMaxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
    val defaultDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!!.baseValue
    val health = with(healthCfg) { health(baseHealth = defaultMaxHealth, classHealthMul = clazz.health, level = level) }
    val damage = with(damageCfg) { damage(baseDamage = defaultDamage, classDamageMul = clazz.damage, level = level) }

    val bossEntity = BossEntity(entity, bossConfig, clazz, level)

    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = health
    entity.health = health
    entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = damage
    val rarityColor = clazz.color
    val bossName = "$rarityColor[Lv. $level] [${clazz.displayName}] ${bossConfig.name}"
    entity.customName = bossName
    entity.isCustomNameVisible = true

    if (entity is org.bukkit.entity.Creeper && bossConfig.isPowered == true) {
      entity.isPowered = true
    }

    entity.isPersistent = true
    entity.location.chunk.isForceLoaded = true

    equipBoss(entity, bossConfig, level, clazz)

    activeBosses[entity.uniqueId] = bossEntity
//    effectsManager.playSpawnEffect(entity, bossEntity)

    plugin.logger.debug { "Manual boss transformation completed: ${bossConfig.name} lvl ${level} [${clazz.displayName}]" }
  }

  /**
   * Получает класс босса по названию
   */
  fun getBossClassByName(className: String): BossClass? {
    val classes = plugin.configuration[ClassesConfigurationModel::class.java]
    return when (className.lowercase()) {
      "common" -> classes.common
      "uncommon" -> classes.uncommon
      "rare" -> classes.rare
      "epic" -> classes.epic
      "legendary" -> classes.legendary
      "mythic" -> classes.mythic
      else -> null
    }
  }

  /**
   * Получает список доступных типов существ
   */
  fun getAvailableEntityTypes(): List<String> {
    return listOf(
      "ZOMBIE", "SKELETON", "PIGLIN", "PILLAGER", "SPIDER",
      "ENDERMAN", "WITHER_SKELETON", "VINDICATOR", "STRAY",
      "CREEPER", "HUSK", "WITCH", "DROWNED", "EVOKER"
    )
  }

  /**
   * Получает список доступных классов боссов
   */
  fun getAvailableBossClasses(): List<String> {
    return listOf("common", "uncommon", "rare", "epic", "legendary", "mythic")
  }

  /**
   * Убивает всех активных боссов
   * @return количество убитых боссов
   */
  fun killAllBosses(): Int {
    val count = activeBosses.size
    if (count == 0) {
      plugin.logger.debug { "No active bosses to kill" }
      return 0
    }

    plugin.logger.info { "🔄 Killing all ${count} active bosses via command" }
    val bossesToKill = activeBosses.values.toList()

    bossesToKill.forEach { boss ->
      // Убиваем сущность босса
      boss.entity.health = 0.0
      plugin.logger.debug { "Killed boss: ${boss.bossType.name} lvl ${boss.level}" }
    }

    plugin.logger.info { "✅ Successfully killed ${count} bosses via killall command" }
    return count
  }

  /**
   * Очищает всех активных боссов и состояние менеджера при отключении плагина
   * Физически убивает всех боссов, чтобы они не оставались после перезагрузки
   */
  fun cleanup() {
    if (activeBosses.isEmpty()) {
      plugin.logger.debug { "BossManager: no active bosses to cleanup" }
      return
    }

    val bossCount = activeBosses.size
    plugin.logger.info { "🔄 Killing $bossCount active bosses during plugin shutdown..." }

    // Убиваем всех активных боссов
    val bossesToRemove = activeBosses.values.toList()
    bossesToRemove.forEach { boss ->
      try {
        // Убираем эффекты и боссбары перед убийством
        effectsManager.removeGlowEffect(boss.entity, boss)
        bossBarManager.removeBossBar(boss)

        // Отключаем форсированную загрузку чанков
        boss.entity.location.chunk.isForceLoaded = false

        // Физически убиваем босса
        if (!boss.entity.isDead) {
          boss.entity.health = 0.0
          plugin.logger.debug { "Killed boss: ${boss.entity.customName ?: boss.entity.type.name} (UUID: ${boss.entity.uniqueId})" }
        }
      } catch (e: Exception) {
        plugin.logger.warn { "Error killing boss ${boss.entity.uniqueId}: ${e.message}" }
      }
    }

    // Очищаем все карты
    activeBosses.clear()
    abilitiesManager.clearAllCooldowns()

    plugin.logger.info { "✅ BossManager: $bossCount bosses killed and state cleaned up during plugin shutdown" }
  }
}