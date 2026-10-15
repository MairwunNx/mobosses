package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.lang.Message
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.spawn.SurvivalBonus
import kotlin.math.ceil
import kotlin.random.Random

/**
 * One item from `loottable.yml` for each player who dealt enough of a boss's health. A boss drops
 * loot of its class and the classes below, at falling odds the further down, never above. The
 * survival bonus tips the odds toward rare and better items, and a player whose drops from bosses
 * that could give one brought none for a while gets one for sure: after a count rolled anew each time,
 * so it shows no pattern. Names and lore come in the language of the player who gets the item.
 */
class Loot(private val plugin: MoBossesPlugin) {
  fun drop(boss: Boss, player: Player) {
    val luck = SurvivalBonus.bonus(plugin.catalog.settings.survivalBonus, plugin.survival.minutes(player)) * plugin.catalog.settings.loot.survivalLuck
    val pool = pool(boss, luck)
    // Only a pool with something rare in it moves the pity count: a Common boss neither owes one nor pays it.
    val rarePool = pool.filter { (loot, _) -> isRare(loot) }
    val item = (if (rarePool.isNotEmpty() && pityDue(player)) Rolls.weighted(rarePool, Random) else null) ?: Rolls.weighted(pool, Random)
      ?: return plugin.log.debug { "No loot for ${player.name} from a level ${boss.level} ${boss.clazz.key} boss" }
    if (rarePool.isNotEmpty()) track(player, isRare(item))
    val stack = stack(item, boss.level, plugin.texts.languageOf(player))
    boss.entity.world.dropItemNaturally(boss.entity.location, stack) {
      it.owner = player.uniqueId
      it.pickupDelay = 20
    }
    plugin.log.debug { "${player.name} got ${stack.amount} ${stack.type.key} from a level ${boss.level} ${boss.clazz.key} boss" }
    announce(item, stack, player)
  }

  /** The items for the boss's level and class, with their weights, rare ones tipped up by [luck]. */
  fun pool(boss: Boss, luck: Double = 0.0): List<Pair<Catalog.Loot, Double>> {
    val classes = plugin.catalog.classes
    val bossRank = classes.rank(boss.clazz.key)
    return plugin.catalog.loot.mapNotNull { loot ->
      val itemRank = classes.rank(loot.spec.mobclass)
      if (itemRank < 0 || itemRank > bossRank || boss.level !in loot.spec.minLevel..loot.spec.maxLevel) null
      else loot to loot.spec.weight * crossClass(bossRank - itemRank) * (1.0 + luck.coerceAtLeast(0.0) * (LUCK_TIER[loot.spec.itemClass?.lowercase()] ?: 0.0))
    }
  }

  private fun isRare(loot: Catalog.Loot) = loot.spec.itemClass?.lowercase() in LUCK_TIER

  private fun pityDue(player: Player): Boolean {
    val pity = plugin.catalog.settings.loot.pity
    if (!pity.enabled) return false
    val data = player.persistentDataContainer
    val dry = data.get(plugin.keys.lootDry, PersistentDataType.INTEGER) ?: 0
    val at = data.get(plugin.keys.lootPityAt, PersistentDataType.INTEGER) ?: rollPity().also { data.set(plugin.keys.lootPityAt, PersistentDataType.INTEGER, it) }
    return dry >= at
  }

  /** A rare item starts the count over, with a new count to reach; anything else adds one. */
  private fun track(player: Player, rare: Boolean) {
    val data = player.persistentDataContainer
    if (rare) {
      data.set(plugin.keys.lootDry, PersistentDataType.INTEGER, 0)
      data.set(plugin.keys.lootPityAt, PersistentDataType.INTEGER, rollPity())
    } else {
      data.set(plugin.keys.lootDry, PersistentDataType.INTEGER, (data.get(plugin.keys.lootDry, PersistentDataType.INTEGER) ?: 0) + 1)
    }
  }

  private fun rollPity(): Int {
    val pity = plugin.catalog.settings.loot.pity
    val low = pity.minDrops.coerceAtLeast(1)
    return Random.nextInt(low, pity.maxDrops.coerceAtLeast(low) + 1)
  }

  private fun stack(loot: Catalog.Loot, level: Int, code: String): ItemStack {
    val spec = loot.spec
    val factor = 1.0 + plugin.catalog.configs.loot.settings.perLevelDropFactor * (level - spec.minLevel).coerceAtLeast(0)
    val amount = ceil(spec.amount.random() * factor).toInt().coerceIn(1, loot.material.maxStackSize.coerceAtLeast(1))
    val stack = ItemStack(loot.material, amount)

    stack.editMeta { meta ->
      loot.enchantments.forEach { (enchantment, range) ->
        val enchantmentLevel = range.random().coerceAtLeast(1)
        // A book stores its enchantments, so an anvil can use them; 1.x put them on the book itself.
        if (meta is EnchantmentStorageMeta) meta.addStoredEnchant(enchantment, enchantmentLevel, true)
        else meta.addEnchant(enchantment, enchantmentLevel, true)
      }
      name(spec, code)?.let { meta.displayName(it) }
      lore(spec, code)?.let { meta.lore(it) }
      if (spec.randomDurability && meta is Damageable) durability(meta, loot.material, spec.itemClass)
    }
    return stack
  }

  /** The item's own name in a language, or null to keep the game's. */
  fun name(spec: LootTable.Item, code: String): Component? {
    val raw = spec.title ?: spec.id?.let { plugin.languages.text(code, "items.$it.name") } ?: return null
    return plain(plugin.languages.deserialize(raw))
  }

  private fun lore(spec: LootTable.Item, code: String): List<Component>? {
    val lines = spec.lore.takeIf { it.isNotEmpty() } ?: spec.id?.let { plugin.languages.text(code, "items.$it.lore") }?.split('\n') ?: return null
    return lines.filter { it.isNotBlank() }.map { plain(plugin.languages.deserialize(it)) }.ifEmpty { null }
  }

  /** Item text isn't italic unless it asks to be. */
  private fun plain(text: Component) = text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

  /**
   * The share of durability left, between `min_durability` and `max_durability`, pulled toward the
   * top for better classes. 1.x read these as wear, so mythic loot came out the most worn.
   */
  private fun durability(meta: Damageable, material: Material, itemClass: String?) {
    val cfg = plugin.catalog.settings.loot.randomDurability
    if (!cfg.enabled || material.maxDurability <= 0) return
    val min = cfg.minDurability.coerceIn(0.1, 1.0)
    val max = cfg.maxDurability.coerceIn(min, 1.0)
    val left = Rolls.biasedInRange(min, max, WEAR_BIAS[itemClass?.lowercase()] ?: 0.0, Random)
    meta.damage = (material.maxDurability * (1.0 - left)).toInt().coerceIn(0, material.maxDurability - 1)
  }

  private fun announce(loot: Catalog.Loot, stack: ItemStack, player: Player) {
    if (!plugin.catalog.settings.broadcast.rareLoot) return
    val message = when (loot.spec.itemClass?.lowercase()) {
      "rare" -> Message.LOOT_RARE
      "epic" -> Message.LOOT_EPIC
      "legendary" -> Message.LOOT_LEGENDARY
      "mythic" -> Message.LOOT_MYTHIC
      else -> return
    }
    plugin.texts.broadcast(message) { code ->
      // Each reader sees the item's name in their own language, and its tooltip on hover.
      val name = (name(loot.spec, code) ?: Component.translatable(stack.type.translationKey())).hoverEvent(stack.asHoverEvent())
      arrayOf(Placeholder.unparsed("player", player.name), Placeholder.component("itemstack", name))
    }
  }

  private companion object {
    /** How much the survival bonus tips each rare item class up: rarer ones more. */
    val LUCK_TIER = mapOf("rare" to 1.0, "epic" to 1.5, "legendary" to 2.0, "mythic" to 2.5)

    /** How far toward `max_durability` each class pulls. */
    val WEAR_BIAS = mapOf("common" to 0.0, "uncommon" to 0.3, "rare" to 0.6, "epic" to 1.0, "legendary" to 1.4, "mythic" to 2.0)

    /** The odds a boss drops loot of a lower class, by how many classes lower. */
    fun crossClass(steps: Int) = when (steps) {
      0 -> 1.0
      1 -> 0.6
      2 -> 0.3
      3 -> 0.15
      4 -> 0.08
      else -> 0.03
    }
  }
}
