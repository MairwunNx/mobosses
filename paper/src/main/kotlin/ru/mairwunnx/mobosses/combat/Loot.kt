package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.EnderDragon
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
import kotlin.random.Random

/**
 * Items from `loottable.yml` for each player who dealt enough of a boss's health: one, and more from
 * higher levels. A boss drops loot of its class and the classes below, at falling odds the further
 * down, never above. The
 * survival bonus tips the odds toward rare and better items, and a player whose drops from bosses
 * that could give one brought none for a while gets one for sure: after a count rolled anew each time,
 * so it shows no pattern. Entries with `types` belong to those boss types only. A type with a
 * `bonus_loot_chance` may drop one more item per kill, from its own entries, for one of the players
 * who got loot, picked by their shares: one per fight, however many fought, so groups don't multiply
 * it. Names and lore come in the language of the player who gets the item.
 */
class Loot(private val plugin: MoBossesPlugin) {
  fun drop(boss: Boss, player: Player) {
    val luck = SurvivalBonus.bonus(plugin.catalog.settings.survivalBonus, plugin.survival.minutes(player)) * plugin.catalog.settings.loot.survivalLuck
    val pool = pool(boss, luck)
    // Only a pool with something rare in it moves the pity count: a Common boss neither owes one nor pays it.
    val rarePool = pool.filter { (loot, _) -> isRare(loot) }
    repeat(LootRules.items(plugin.catalog.settings.loot.items, boss.level)) {
      val item = (if (rarePool.isNotEmpty() && pityDue(player)) Rolls.weighted(rarePool, Random) else null) ?: Rolls.weighted(pool, Random)
        ?: return plugin.log.debug { "No loot for ${player.name} from a level ${boss.level} ${boss.clazz.key} boss" }
      if (rarePool.isNotEmpty()) track(player, isRare(item))
      give(boss, player, item)
    }
  }

  /** The bonus roll after a kill: [shares] holds the players who got loot, with their shares. */
  fun bonus(boss: Boss, shares: Map<Player, Double>) {
    if (shares.isEmpty() || Random.nextDouble() >= boss.type.spec.bonusLootChance) return
    val item = Rolls.weighted(pool(boss, own = true), Random) ?: return
    val player = Rolls.weighted(shares.toList(), Random) ?: return
    plugin.log.debug { "Bonus loot for ${player.name} from a ${boss.type.key}" }
    give(boss, player, item)
  }

  private fun give(boss: Boss, player: Player, item: Catalog.Loot) {
    val stack = stack(item, boss.level, plugin.texts.languageOf(player))
    // The dragon dies over the exit portal, which would carry its loot off to the Overworld: it drops at the player.
    val at = if (boss.entity is EnderDragon) player.location else boss.entity.location
    at.world.dropItemNaturally(at, stack) {
      it.owner = player.uniqueId
      it.pickupDelay = 20
    }
    plugin.log.debug { "${player.name} got ${stack.amount} ${stack.type.key} from a level ${boss.level} ${boss.clazz.key} boss" }
    announce(item, stack, player)
  }

  /**
   * The items for the boss's level and class, with their weights, rare ones tipped up by [luck]: the
   * shared table and its type's own entries, or, with [own], only the latter.
   */
  fun pool(boss: Boss, luck: Double = 0.0, own: Boolean = false): List<Pair<Catalog.Loot, Double>> =
    LootRules.pool(plugin.catalog.loot, { it.spec }, { loot -> if (loot.types.isEmpty()) !own else boss.type.entity in loot.types }, plugin.catalog.classes, boss.clazz.key, boss.level, luck)

  private fun isRare(loot: Catalog.Loot) = LootRules.isRare(loot.spec)

  private fun pityDue(player: Player): Boolean {
    val pity = plugin.catalog.settings.loot.pity
    if (!pity.enabled) return false
    val data = player.persistentDataContainer
    val dry = data.get(plugin.keys.lootDry, PersistentDataType.INTEGER) ?: 0
    val at = data.get(plugin.keys.lootPityAt, PersistentDataType.INTEGER) ?: LootRules.pityAt(pity).also { data.set(plugin.keys.lootPityAt, PersistentDataType.INTEGER, it) }
    return dry >= at
  }

  /** A rare item starts the count over, with a new count to reach; anything else adds one. */
  private fun track(player: Player, rare: Boolean) {
    val data = player.persistentDataContainer
    if (rare) {
      data.set(plugin.keys.lootDry, PersistentDataType.INTEGER, 0)
      data.set(plugin.keys.lootPityAt, PersistentDataType.INTEGER, LootRules.pityAt(plugin.catalog.settings.loot.pity))
    } else {
      data.set(plugin.keys.lootDry, PersistentDataType.INTEGER, (data.get(plugin.keys.lootDry, PersistentDataType.INTEGER) ?: 0) + 1)
    }
  }

  private fun stack(loot: Catalog.Loot, level: Int, code: String): ItemStack {
    val spec = loot.spec
    val amount = LootRules.amount(spec, plugin.catalog.configs.loot.settings, level, loot.material.maxStackSize)
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
      if (meta is Damageable) LootRules.damage(plugin.catalog.settings.loot.randomDurability, spec, loot.material.maxDurability.toInt())?.let { meta.damage = it }
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
}
