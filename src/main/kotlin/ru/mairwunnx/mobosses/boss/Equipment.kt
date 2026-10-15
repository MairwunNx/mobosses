package ru.mairwunnx.mobosses.boss

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Gear for a boss's level: a random pick among the weapons and armor sets it qualifies for, within
 * [SPREAD] levels of the best one, so a level 40 boss doesn't come with level 2 gear.
 */
object Equipment {
  private const val SPREAD = 7

  /** True when the boss got an armor set, which then takes the place of its armor attributes. */
  fun equip(entity: LivingEntity, type: Catalog.BossType, level: Int, rng: Random = Random): Boolean {
    val equipment = entity.equipment ?: return false
    pick(type.weapons, level, rng) { it.minLevel }?.let { equipment.setItemInMainHand(item(it)) }
    val set = pick(type.armors, level, rng) { it.minLevel } ?: return false
    equipment.setHelmet(set.helmet?.let(::item))
    equipment.setChestplate(set.chestplate?.let(::item))
    equipment.setLeggings(set.leggings?.let(::item))
    equipment.setBoots(set.boots?.let(::item))
    return true
  }

  fun <T> pick(options: List<T>, level: Int, rng: Random, minLevel: (T) -> Int): T? {
    val fit = options.filter { minLevel(it) <= level }.ifEmpty { return null }
    val best = fit.maxOf(minLevel)
    return fit.filter { minLevel(it) >= best - SPREAD }.random(rng)
  }

  private fun item(gear: Catalog.Gear) = ItemStack(gear.material).apply { gear.enchantments.forEach { (e, l) -> addUnsafeEnchantment(e, l.coerceAtLeast(1)) } }
}
