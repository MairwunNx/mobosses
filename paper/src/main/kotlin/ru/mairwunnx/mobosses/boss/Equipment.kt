package ru.mairwunnx.mobosses.boss

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Gear for a boss's level: a random pick among the weapons and armor sets it qualifies for, within
 * [SPREAD] levels of the best one. Gear only ever gets better than what the mob spawned with: a piece
 * goes on only where the slot is empty or the new piece is stronger, and an archer keeps its bow.
 */
object Equipment {
  private const val SPREAD = 7

  /** True when the boss wears any armor from its set, which then takes the place of its armor attributes. */
  fun equip(entity: LivingEntity, type: Catalog.BossType, level: Int, rng: Random = Random): Boolean {
    val equipment = entity.equipment ?: return false
    pick(type.weapons, level, rng) { it.minLevel }?.let { gear ->
      val next = item(gear)
      if (betterWeapon(next, equipment.itemInMainHand)) equipment.setItemInMainHand(next)
    }
    val set = pick(type.armors, level, rng) { it.minLevel } ?: return false
    var worn = false
    listOf(EquipmentSlot.HEAD to set.helmet, EquipmentSlot.CHEST to set.chestplate, EquipmentSlot.LEGS to set.leggings, EquipmentSlot.FEET to set.boots)
      .forEach { (slot, gear) ->
        // A slot the set leaves out keeps what the mob has, a pumpkin against the sun included.
        val next = gear?.let(::item) ?: return@forEach
        if (armor(next, slot) >= armor(equipment.getItem(slot), slot)) {
          equipment.setItem(slot, next)
          worn = true
        }
      }
    return worn
  }

  fun <T> pick(options: List<T>, level: Int, rng: Random, minLevel: (T) -> Int): T? {
    val fit = options.filter { minLevel(it) <= level }.ifEmpty { return null }
    val best = fit.maxOf(minLevel)
    return fit.filter { minLevel(it) >= best - SPREAD }.random(rng)
  }

  /** Whether [next] beats [current]: an empty hand takes anything; otherwise only a weapon of the same kind that hits at least as hard. */
  private fun betterWeapon(next: ItemStack, current: ItemStack): Boolean {
    if (current.type.isAir) return true
    if (kind(next.type) != kind(current.type)) return false
    return weapon(next) >= weapon(current)
  }

  private enum class Kind { BOW, CROSSBOW, TRIDENT, MELEE }

  private fun kind(material: Material) = when (material) {
    Material.BOW -> Kind.BOW
    Material.CROSSBOW -> Kind.CROSSBOW
    Material.TRIDENT -> Kind.TRIDENT
    else -> Kind.MELEE
  }

  private fun weapon(item: ItemStack): Double =
    modifiers(item, EquipmentSlotGroup.MAINHAND, Attribute.ATTACK_DAMAGE) + enchant(item, Enchantment.SHARPNESS) * 0.5 + enchant(item, Enchantment.POWER) * 0.5 + enchant(item, Enchantment.IMPALING) * 0.5

  private fun armor(item: ItemStack, slot: EquipmentSlot): Double {
    if (item.type.isAir) return -1.0
    val group = slot.group
    return modifiers(item, group, Attribute.ARMOR) + modifiers(item, group, Attribute.ARMOR_TOUGHNESS) + enchant(item, Enchantment.PROTECTION) * 0.5
  }

  private fun modifiers(item: ItemStack, group: EquipmentSlotGroup, attribute: Attribute): Double =
    item.type.asItemType()?.getDefaultAttributeModifiers()?.get(attribute)?.filter { it.slotGroup == group || it.slotGroup == EquipmentSlotGroup.ANY }?.sumOf { it.amount } ?: 0.0

  private fun enchant(item: ItemStack, enchantment: Enchantment) = item.getEnchantmentLevel(enchantment).toDouble()

  private fun item(gear: Catalog.Gear) = ItemStack(gear.material).apply { gear.enchantments.forEach { (e, l) -> addUnsafeEnchantment(e, l.coerceAtLeast(1)) } }
}
