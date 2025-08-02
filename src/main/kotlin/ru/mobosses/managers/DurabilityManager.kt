package ru.mobosses.managers

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.LootTableConfigurationModel.LootItem
import ru.mobosses.PluginUnit
import kotlin.math.pow
import kotlin.random.Random

class DurabilityManager(private val plugin: PluginUnit) {
  private val general = plugin.configuration[GeneralConfigurationModel::class.java]
  private val bias = mapOf("common" to 0.0, "uncommon" to 0.3, "rare" to 0.6, "epic" to 1.0, "legendary" to 1.4, "mythic" to 2.0)

  fun adjustDurability(item: ItemStack, lootItem: LootItem): ItemStack {
    val cfg = general.loot.randomDurability
    val type = item.type

    if (!cfg.enabled || type.maxDurability <= 0 || item.itemMeta !is Damageable) {
      plugin.logger.debug { "Skipping durability for $type: disabled or not damageable" }
      return item
    }

    val meta = item.itemMeta as Damageable

    // Конфиг хранит долю урона (0.0..1.0)
    val minF = cfg.minDurability.coerceIn(0.0, 0.9)   // не даём выйти за 90% урона
    val maxF = cfg.maxDurability.coerceIn(minF, 0.9)  // остаток ≥ 10%

    val bias  = bias.getOrDefault(lootItem.itemClass, 0.0)
    val frac  = biasedInRange(minF, maxF, bias)       // доля урона с учётом класса
    val maxHP = type.maxDurability
    val dmg   = (maxHP * frac).toInt().coerceAtMost((maxHP * 0.9).toInt()) // ≤ 90%

    meta.damage = dmg
    item.itemMeta = meta

    plugin.logger.debug {
      val remainPct = ((1.0 - dmg.toDouble() / maxHP) * 100).toInt()
      "Durability for $type [class=${lootItem.itemClass ?: "unknown"}]: damage=${dmg}/${maxHP} (~${100 - remainPct}% wear), remain≈${remainPct}%"
    }

    return item
  }

  /**
   * Возвращает точку в (min..max) со сдвигом к max.
   * bias >= 0: 0 — без сдвига, выше — сильнее тянет к max.
   */
  private fun biasedInRange(min: Double, max: Double, bias: Double): Double {
    val u = Random.nextDouble() // 0..1
    val t = 1.0 - (1.0 - u).pow(1.0 + bias) // чем больше bias, тем ближе к 1
    return min + (max - min) * t
  }

  fun cleanup() {
    plugin.logger.debug { "DurabilityManager: no state to cleanup" }
  }
}