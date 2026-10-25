package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TridentItem
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.random.Random

/**
 * Gear for a boss's level, as Paper's `Equipment`: a random pick among the weapons and armor sets it qualifies for,
 * within [SPREAD] levels of the best one. Gear only ever gets better than what the mob spawned with: a piece goes on
 * only where the slot is empty or the new piece is stronger, and an archer keeps its bow. Strength comes from the
 * item's own attributes, so other mods' gear ranks the same way.
 */
object Equipment {
  private const val SPREAD = 7

  /** True when the boss wears any armor from its set, which then takes the place of its armor attributes. */
  fun equip(entity: LivingEntity, type: Catalog.BossType, level: Int, rng: Random = Random): Boolean {
    pick(type.weapons, level, rng) { it.minLevel }?.let { gear ->
      val next = item(entity, gear)
      if (betterWeapon(entity, next, entity.getItemBySlot(EquipmentSlot.MAINHAND))) entity.setItemSlot(EquipmentSlot.MAINHAND, next)
    }
    val set = pick(type.armors, level, rng) { it.minLevel } ?: return false
    var worn = false
    listOf(EquipmentSlot.HEAD to set.helmet, EquipmentSlot.CHEST to set.chestplate, EquipmentSlot.LEGS to set.leggings, EquipmentSlot.FEET to set.boots)
      .forEach { (slot, gear) ->
        // A slot the set leaves out keeps what the mob has, a pumpkin against the sun included.
        val next = gear?.let { item(entity, it) } ?: return@forEach
        if (armor(entity, next, slot) >= armor(entity, entity.getItemBySlot(slot), slot)) {
          entity.setItemSlot(slot, next)
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
  private fun betterWeapon(entity: LivingEntity, next: ItemStack, current: ItemStack): Boolean {
    if (current.isEmpty) return true
    if (kind(next) != kind(current)) return false
    return weapon(entity, next) >= weapon(entity, current)
  }

  private enum class Kind { BOW, CROSSBOW, TRIDENT, MELEE }

  private fun kind(stack: ItemStack) = when (stack.item) {
    is BowItem -> Kind.BOW
    is CrossbowItem -> Kind.CROSSBOW
    is TridentItem -> Kind.TRIDENT
    else -> Kind.MELEE
  }

  private fun weapon(entity: LivingEntity, stack: ItemStack): Double =
    modifiers(stack, EquipmentSlotGroup.MAINHAND, Attributes.ATTACK_DAMAGE) +
      (enchant(entity, stack, Enchantments.SHARPNESS) + enchant(entity, stack, Enchantments.POWER) + enchant(entity, stack, Enchantments.IMPALING)) * 0.5

  private fun armor(entity: LivingEntity, stack: ItemStack, slot: EquipmentSlot): Double {
    if (stack.isEmpty) return -1.0
    val group = EquipmentSlotGroup.bySlot(slot)
    return modifiers(stack, group, Attributes.ARMOR) + modifiers(stack, group, Attributes.ARMOR_TOUGHNESS) + enchant(entity, stack, Enchantments.PROTECTION) * 0.5
  }

  /** The item type's own attributes for [group], as Bukkit's `getDefaultAttributeModifiers`. */
  private fun modifiers(stack: ItemStack, group: EquipmentSlotGroup, attribute: Holder<Attribute>): Double =
    (stack.item.components().get(DataComponents.ATTRIBUTE_MODIFIERS) ?: ItemAttributeModifiers.EMPTY).modifiers()
      .filter { it.attribute() == attribute && (it.slot() == group || it.slot() == EquipmentSlotGroup.ANY) }
      .sumOf { it.modifier().amount() }

  private fun enchant(entity: LivingEntity, stack: ItemStack, key: ResourceKey<Enchantment>): Double {
    val holder = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null) ?: return 0.0
    return EnchantmentHelper.getItemEnchantmentLevel(holder, stack).toDouble()
  }

  private fun item(entity: LivingEntity, gear: Catalog.Gear) = ItemStack(gear.item).apply { gear.enchantments.forEach { (e, l) -> enchant(e, l.coerceAtLeast(1)) } }
}
