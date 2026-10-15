package ru.mairwunnx.mobosses.boss

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Creeper
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.Settings
import java.util.UUID

/**
 * Turns a mob into a boss: its data on the entity, then its stats and gear. A boss is never weaker
 * than the mob it was: every stat is at least the mob's own, whatever the classes and curves say,
 * and gear only gets better.
 */
class BossFactory(private val data: Keys) {
  fun apply(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID?, formula: Settings.Formulas) {
    entity.persistentDataContainer.apply {
      set(data.type, PersistentDataType.STRING, type.key)
      set(data.clazz, PersistentDataType.STRING, clazz.key)
      set(data.level, PersistentDataType.INTEGER, level)
      summoner?.let { set(data.summoner, PersistentDataType.STRING, it.toString()) }
    }

    // From the type's own values: a minion that rolls into a boss may have been changed already.
    entity.getAttribute(Attribute.MAX_HEALTH)?.let {
      it.baseValue = Stats.health(formula.health, it.defaultValue, clazz.health, level).coerceAtLeast(it.defaultValue)
      entity.health = it.value
    }
    entity.getAttribute(Attribute.ATTACK_DAMAGE)?.let { it.baseValue = Stats.damage(formula.damage, it.defaultValue, clazz.damage, level).coerceAtLeast(it.defaultValue) }
    if (entity is Creeper && type.spec.isPowered == true) entity.isPowered = true
    if (!Equipment.equip(entity, type, level)) {
      entity.getAttribute(Attribute.ARMOR)?.let { it.baseValue = Stats.armor(formula.armor, it.defaultValue, clazz.defense, level).coerceAtLeast(it.defaultValue) }
      entity.getAttribute(Attribute.ARMOR_TOUGHNESS)?.let { it.baseValue = Stats.armor(formula.toughness, it.defaultValue, clazz.toughness, level).coerceAtLeast(it.defaultValue) }
    }
  }
}
