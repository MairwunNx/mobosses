package ru.mairwunnx.mobosses.boss

import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Formulas
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * The bosses in loaded chunks. A boss is an ordinary mob with Mo’Bosses' data on it: [make] writes
 * that data and scales the mob, [adopt] picks a boss up again when its chunk loads, and [forget]
 * lets go of everything but the entity when its chunk unloads or it dies. The server thread only.
 */
class Bosses(private val plugin: MoBossesPlugin) {
  private val live = HashMap<UUID, Boss>()

  val all: Collection<Boss> get() = live.values

  fun get(entity: Entity): Boss? = live[entity.uniqueId]

  fun get(uuid: UUID): Boss? = live[uuid]

  fun count(world: World) = live.values.count { it.entity.world == world }

  /** Scales [entity] into a boss and marks it as one, so it stays a boss after a restart. */
  fun make(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID? = null): Boss {
    val catalog = plugin.catalog
    val formula = catalog.settings.formula
    val lvl = level.coerceAtLeast(1)

    entity.persistentDataContainer.apply {
      set(plugin.keys.type, PersistentDataType.STRING, type.key)
      set(plugin.keys.clazz, PersistentDataType.STRING, clazz.key)
      set(plugin.keys.level, PersistentDataType.INTEGER, lvl)
      summoner?.let { set(plugin.keys.summoner, PersistentDataType.STRING, it.toString()) }
    }

    // The type's own values, not the current ones: a minion that turns into a boss was already changed by its spawn.
    entity.getAttribute(Attribute.MAX_HEALTH)?.let { attribute ->
      attribute.baseValue = Formulas.health(formula.health, attribute.defaultValue, clazz.health, lvl)
      entity.health = attribute.value
    }
    entity.getAttribute(Attribute.ATTACK_DAMAGE)?.let { it.baseValue = Formulas.damage(formula.damage, it.defaultValue, clazz.damage, lvl) }
    if (entity is Creeper && type.spec.isPowered == true) entity.isPowered = true
    if (!equip(entity, type, lvl)) {
      entity.getAttribute(Attribute.ARMOR)?.let { it.baseValue = Formulas.armor(formula.armor, it.defaultValue, clazz.defense, lvl) }
      entity.getAttribute(Attribute.ARMOR_TOUGHNESS)?.let { it.baseValue = Formulas.armor(formula.toughness, it.defaultValue, clazz.toughness, lvl) }
    }

    val boss = Boss(entity, type, clazz, lvl, summoner)
    live[entity.uniqueId] = boss
    nameTag(boss)
    plugin.log.debug { "${type.key} became a level $lvl ${clazz.key} boss at ${entity.world.name} ${entity.location.blockX} ${entity.location.blockY} ${entity.location.blockZ}" }
    return boss
  }

  /** Takes up a boss whose chunk just loaded, or one that was here before a restart. Null for any other entity. */
  fun adopt(entity: Entity): Boss? {
    if (entity !is LivingEntity || entity.isDead) return null
    live[entity.uniqueId]?.let { return it }
    val data = entity.persistentDataContainer
    val typeKey = data.get(plugin.keys.type, PersistentDataType.STRING) ?: return null
    val catalog = plugin.catalog
    val type = catalog.types.values.firstOrNull { it.key == typeKey }
      // The type left bosses.yml since this boss spawned: it stays a boss, with plain effects.
      ?: Catalog.BossType(entity.type, BossSpec(), null, null, null, emptyList(), emptyList())
    val clazz = data.get(plugin.keys.clazz, PersistentDataType.STRING)?.let(catalog.classes::byKey) ?: catalog.classes.all.first()
    val level = data.get(plugin.keys.level, PersistentDataType.INTEGER) ?: 1
    val summoner = data.get(plugin.keys.summoner, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    return Boss(entity, type, clazz, level, summoner).also { live[entity.uniqueId] = it }
  }

  /** Lets go of a boss: its bar, glow, aura, and cooldowns. The entity stays as it is. */
  fun forget(boss: Boss) {
    if (live.remove(boss.uuid) == null) return
    plugin.bars.remove(boss)
    plugin.visuals.off(boss)
  }

  /** Removes a boss from the world without loot or experience, as `/mobosses killall` does. */
  fun remove(boss: Boss) {
    forget(boss)
    boss.entity.remove()
  }

  /** Lets go of every boss, for a reload or shutdown; the entities stay in the world. */
  fun forgetAll() = live.values.toList().forEach(::forget)

  /** The name over the boss's head. Every player sees the same one, so it's in the default language. */
  fun nameTag(boss: Boss) {
    val code = plugin.languages.default
    plugin.languages.render(code, Message.BOSS_TAG, *plugin.texts.boss(code, boss))?.let {
      boss.entity.customName(it)
      boss.entity.isCustomNameVisible = true
    }
  }

  /**
   * Gear for the boss's level: a random pick among the weapons and armor sets it qualifies for,
   * within 7 levels of the best one. True when it got armor, which then replaces its armor attributes.
   */
  private fun equip(entity: LivingEntity, type: Catalog.BossType, level: Int): Boolean {
    val equipment = entity.equipment ?: return false
    type.weapons.filter { it.minLevel <= level }.takeIf { it.isNotEmpty() }?.let { weapons ->
      val best = weapons.maxOf { it.minLevel }
      weapons.filter { it.minLevel >= best - 7 }.randomOrNull()?.let { equipment.setItemInMainHand(item(it)) }
    }
    val armors = type.armors.filter { it.minLevel <= level }.takeIf { it.isNotEmpty() } ?: return false
    val best = armors.maxOf { it.minLevel }
    val set = armors.filter { it.minLevel >= best - 7 }.randomOrNull() ?: return false
    equipment.setHelmet(set.helmet?.let(::item))
    equipment.setChestplate(set.chestplate?.let(::item))
    equipment.setLeggings(set.leggings?.let(::item))
    equipment.setBoots(set.boots?.let(::item))
    return true
  }

  private fun item(gear: Catalog.Gear) = ItemStack(gear.material).apply { gear.enchantments.forEach { (e, l) -> addUnsafeEnchantment(e, l.coerceAtLeast(1)) } }
}
