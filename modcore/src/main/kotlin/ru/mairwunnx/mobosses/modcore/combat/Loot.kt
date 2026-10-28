package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.EnchantmentHelper
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.combat.LootRules
import ru.mairwunnx.mobosses.config.LootTable
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Catalog
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.spawn.Hunt
import kotlin.random.Random

/**
 * Items from `loottable.yml` for each player who dealt enough of a boss's health, by [LootRules], as on Paper: one, and
 * more from higher levels. Each item drops for its player alone, and its name and lore come in their language. A type with a `bonus_loot_chance`
 * may drop one more item per kill, for one of the players who got loot, picked by their shares.
 */
class Loot(private val mod: MoBosses) {
  fun drop(boss: Boss, player: ServerPlayer) {
    val luck = Hunt.bonus(mod.catalog.settings.survivalBonus, mod.survival.minutes(player)) * mod.catalog.settings.loot.survivalLuck
    val pool = pool(boss, luck)
    // Only a pool with something rare in it moves the pity count: a Common boss neither owes one nor pays it.
    val rarePool = pool.filter { (loot, _) -> LootRules.isRare(loot.spec) }
    repeat(LootRules.items(mod.catalog.settings.loot.items, boss.level)) {
      val item = (if (rarePool.isNotEmpty() && pityDue(player)) Rolls.weighted(rarePool, Random) else null) ?: Rolls.weighted(pool, Random)
        ?: return mod.log.debug { "No loot for ${player.plainTextName} from a level ${boss.level} ${boss.clazz.key} boss" }
      if (rarePool.isNotEmpty()) track(player, LootRules.isRare(item.spec))
      give(boss, player, item)
    }
  }

  /** The bonus roll after a kill: [shares] holds the players who got loot, with their shares. */
  fun bonus(boss: Boss, shares: Map<ServerPlayer, Double>) {
    if (shares.isEmpty() || Random.nextDouble() >= boss.type.spec.bonusLootChance) return
    val item = Rolls.weighted(pool(boss, own = true), Random) ?: return
    val player = Rolls.weighted(shares.toList(), Random) ?: return
    mod.log.debug { "Bonus loot for ${player.plainTextName} from a ${boss.type.key}" }
    give(boss, player, item)
  }

  private fun give(boss: Boss, player: ServerPlayer, item: Catalog.Loot) {
    val level = boss.entity.level() as? ServerLevel ?: return
    val stack = stack(item, boss.level, mod.texts.languageOf(player))
    // The dragon dies over the exit portal, which would carry its loot off to the Overworld: it drops at the player.
    val at = if (boss.entity is EnderDragon) player.position() else boss.entity.position()
    val drop = ItemEntity(level, at.x, at.y + 0.5, at.z, stack.copy())
    drop.setTarget(player.uuid)
    drop.setPickUpDelay(20)
    level.addFreshEntity(drop)
    mod.log.debug { "${player.plainTextName} got ${stack.count} ${stack.item} from a level ${boss.level} ${boss.clazz.key} boss" }
    announce(item, stack, player)
  }

  /**
   * The items for the boss's level and class, with their weights, rare ones tipped up by [luck]: the shared table and
   * its type's own entries, or, with [own], only the latter.
   */
  fun pool(boss: Boss, luck: Double = 0.0, own: Boolean = false): List<Pair<Catalog.Loot, Double>> =
    LootRules.pool(mod.catalog.loot, { it.spec }, { loot -> if (loot.types.isEmpty()) !own else boss.type.entity in loot.types }, mod.catalog.classes, boss.clazz.key, boss.level, luck)

  private fun pityDue(player: ServerPlayer): Boolean {
    val pity = mod.catalog.settings.loot.pity
    if (!pity.enabled) return false
    val data = Data(player)
    val dry = data.int(Keys.LOOT_DRY) ?: 0
    val at = data.int(Keys.LOOT_PITY_AT) ?: LootRules.pityAt(pity).also { data[Keys.LOOT_PITY_AT] = it }
    return dry >= at
  }

  /** A rare item starts the count over, with a new count to reach; anything else adds one. */
  private fun track(player: ServerPlayer, rare: Boolean) {
    val data = Data(player)
    if (rare) {
      data[Keys.LOOT_DRY] = 0
      data[Keys.LOOT_PITY_AT] = LootRules.pityAt(mod.catalog.settings.loot.pity)
    } else {
      data[Keys.LOOT_DRY] = (data.int(Keys.LOOT_DRY) ?: 0) + 1
    }
  }

  fun stack(loot: Catalog.Loot, level: Int, code: String): ItemStack {
    val spec = loot.spec
    val stack = ItemStack(loot.item)
    stack.count = LootRules.amount(spec, mod.catalog.configs.loot.settings, level, stack.maxStackSize)
    // A book stores its enchantments, so an anvil can use them: the game picks which for the item.
    if (loot.enchantments.isNotEmpty()) EnchantmentHelper.updateEnchantments(stack) { enchantments ->
      loot.enchantments.forEach { (enchantment, range) -> enchantments.set(enchantment, range.random().coerceAtLeast(1)) }
    }
    name(spec, code)?.let { stack.set(DataComponents.CUSTOM_NAME, mod.texts.vanilla(it)) }
    lore(spec, code)?.let { lines -> stack.set(DataComponents.LORE, ItemLore(lines.map(mod.texts::vanilla))) }
    LootRules.damage(mod.catalog.settings.loot.randomDurability, spec, stack.maxDamage)?.takeIf { stack.isDamageableItem }?.let { stack.damageValue = it }
    return stack
  }

  /** The item's own name in a language, or null to keep the game's. */
  fun name(spec: LootTable.Item, code: String): Component? {
    val raw = spec.title ?: spec.id?.let { mod.languages.text(code, "items.$it.name") } ?: return null
    return plain(mod.languages.deserialize(raw))
  }

  private fun lore(spec: LootTable.Item, code: String): List<Component>? {
    val lines = spec.lore.takeIf { it.isNotEmpty() } ?: spec.id?.let { mod.languages.text(code, "items.$it.lore") }?.split('\n') ?: return null
    return lines.filter { it.isNotBlank() }.map { plain(mod.languages.deserialize(it)) }.ifEmpty { null }
  }

  /** Item text isn't italic unless it asks to be. */
  private fun plain(text: Component) = text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

  private fun announce(loot: Catalog.Loot, stack: ItemStack, player: ServerPlayer) {
    if (!mod.catalog.settings.broadcast.rareLoot) return
    val message = when (loot.spec.itemClass?.lowercase()) {
      "rare" -> Message.LOOT_RARE
      "epic" -> Message.LOOT_EPIC
      "legendary" -> Message.LOOT_LEGENDARY
      "mythic" -> Message.LOOT_MYTHIC
      else -> return
    }
    mod.texts.broadcast(message) { code ->
      // Each reader sees the item's name in their own language.
      val name = name(loot.spec, code) ?: Component.translatable(stack.item.descriptionId)
      arrayOf(Placeholder.unparsed("player", player.plainTextName), Placeholder.component("itemstack", name))
    }
  }
}
