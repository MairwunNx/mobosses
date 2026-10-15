package ru.mobosses.managers

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.mairwunnx.mobosses.framework.platformItemTextFrom
import ru.mairwunnx.mobosses.managers.DurabilityManager
import ru.mairwunnx.mobosses.models.LootTableConfigurationModel
import ru.mairwunnx.mobosses.models.MessagesConfigurationModel
import ru.mobosses.PluginUnit
import ru.mobosses.entities.BossEntity

/**
 * Вспомогательный класс для работы с модифицированным весом лута
 */
private class WeightedLootItem(
  val lootItem: LootTableConfigurationModel.LootItem,
  val weight: Double
)

class LootManager(
  private val plugin: PluginUnit,
  private val durabilityManager: DurabilityManager,
) {
  val lootTable = plugin.configuration[LootTableConfigurationModel::class.java]
  val messages = plugin.configuration[MessagesConfigurationModel::class.java]

  fun dropLoot(boss: BossEntity, killer: Player, damageFactor: Double) {
    plugin.logger.debug { "Starting loot generation for boss ${boss.bossType.name} lvl ${boss.level} killed by ${killer.name}" }

    if (damageFactor <= 0.27777777) {
        plugin.logger.debug { "Player ${killer.name} did not get loot due to low damage contribution." }
        return
    }

    plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
      val itemsToDrop = generateLoot(boss)
      plugin.server.scheduler.runTask(plugin, Runnable {
        if (itemsToDrop.isNotEmpty()) {
          plugin.logger.debug { "Generated loot for ${killer.name}: ${itemsToDrop.joinToString(", ") { "${it.amount}x ${it.type.name}" }}" }
          dropLootAtBoss(boss, killer, itemsToDrop)
          plugin.logger.debug { "Loot dropped for ${killer.name}: ${itemsToDrop.size} items" }
        } else {
          plugin.logger.debug { "No loot generated for ${boss.bossType.name} lvl ${boss.level}" }
        }
      })
    })
  }

  private fun dropLootOnGround(at: Location, killer: Player, items: List<ItemStack>, pickupDelayTicks: Int = 20) {
    val world = at.world ?: return
    items.forEach { stack ->
      val drop = world.dropItemNaturally(at, stack) // сам чуть рандомно раскинет
      drop.owner = killer.uniqueId
      drop.pickupDelay = pickupDelayTicks                          // небольшая задержка (по желанию)
    }
  }

  private fun dropLootAtBoss(boss: BossEntity, killer: Player, items: List<ItemStack>, pickupDelayTicks: Int = 20) =
    dropLootOnGround(boss.entity.location, killer, items, pickupDelayTicks)

  private fun generateLoot(boss: BossEntity): List<ItemStack> {
    val level = boss.level.coerceAtLeast(1)
    val bossClass = boss.clazz.displayName.lowercase(java.util.Locale.ROOT)
    val loot = mutableListOf<ItemStack>()

    plugin.logger.debug { "Generating loot for boss class ${bossClass} lvl ${level}" }

    val table = lootTable

    // Кандидаты + модификатор шанса под «кросс-класс»
    val possible = table.levelLoot.asSequence()
      .filter { it.minLevel <= level && level <= it.maxLevel && canDropLoot(bossClass, it.mobclass) }
      .map { li -> WeightedLootItem(li, (li.weight * getClassDropModifier(bossClass, li.mobclass)).coerceAtLeast(0.0)) }
      .filter { it.weight > 0.0 }
      .toList()

    if (possible.isEmpty()) {
      plugin.logger.debug { "No possible loot items found for ${bossClass} lvl ${level}" }
      return emptyList()
    }

    plugin.logger.debug { "Found ${possible.size} possible loot items for ${bossClass} lvl ${level}" }

    val totalWeight = possible.sumOf { it.weight }
    val r = kotlin.random.Random.nextDouble() * totalWeight
    var acc = 0.0
    var picked: WeightedLootItem? = null
    for (w in possible) {
      acc += w.weight
      if (r <= acc) {
        picked = w; break
      }
    }
    val selected = picked ?: possible.last()

    plugin.logger.debug { "Selected loot item: ${selected.lootItem.material} (weight: ${selected.weight}, class: ${selected.lootItem.mobclass})" }

    // Генерация одного ItemStack (внимание на maxStack)
    val stack = createItemStack(selected.lootItem, boss.level)

    // Определяем класс предмета из конфигурации (по умолчанию common)
    val itemClass = selected.lootItem.itemClass ?: "common"

    if (selected.lootItem.randomDurability == true) {
      durabilityManager.adjustDurability(stack, selected.lootItem)
      plugin.logger.debug { "Applied random durability to ${stack.type}" }
    }

    // Применяем кастомное название и лор если они указаны
    applyCustomMetadata(stack, selected.lootItem)

    loot += stack
    plugin.logger.debug { "Created loot item: ${stack.amount}x ${stack.type} (class: ${itemClass})" }

    // Сообщение о редком луте на основе класса предмета (только для rare+)
    if (messages.boss.loot.enabled && itemClass != "common") {
      boss.entity.killer?.let { killer ->
        plugin.logger.debug { "Broadcasting rare loot message for ${itemClass} item to all players: ${stack.type}" }

        val stringArgs = mapOf("player" to killer.name)
        val componentArgs = mapOf("itemstack" to platformItemTextFrom(stack))

        when (itemClass.lowercase()) {
          "rare" -> plugin.messages.broadcastPlayers(
            messages.boss.loot.rareDrop,
            stringArgs = stringArgs,
            componentArgs = componentArgs
          )
          "epic" -> plugin.messages.broadcastPlayers(
            messages.boss.loot.epicDrop,
            stringArgs = stringArgs,
            componentArgs = componentArgs
          )
          "legendary" -> plugin.messages.broadcastPlayers(
            messages.boss.loot.legendaryDrop,
            stringArgs = stringArgs,
            componentArgs = componentArgs
          )
          "mythic" -> plugin.messages.broadcastPlayers(
            messages.boss.loot.mythicDrop,
            stringArgs = stringArgs,
            componentArgs = componentArgs
          )
        }
      }
    }

    return loot
  }

  /** Босс может дропать лут своего класса и ниже. */
  private fun canDropLoot(bossClass: String, lootClass: String): Boolean {
    val bossIndex = CLASS_HIERARCHY.indexOfFirst { it.equals(bossClass, true) }
    val lootIndex = CLASS_HIERARCHY.indexOfFirst { it.equals(lootClass, true) }
    if (bossIndex == -1 || lootIndex == -1) return false
    return bossIndex >= lootIndex
  }

  /** Модификатор шанса при кросс-классе. */
  private fun getClassDropModifier(bossClass: String, lootClass: String): Double {
    val bossIndex = CLASS_HIERARCHY.indexOfFirst { it.equals(bossClass, true) }
    val lootIndex = CLASS_HIERARCHY.indexOfFirst { it.equals(lootClass, true) }
    if (bossIndex == -1 || lootIndex == -1) return 1.0
    return when (bossIndex - lootIndex) {
      0 -> 1.0
      1 -> 0.6
      2 -> 0.3
      3 -> 0.15
      4 -> 0.08
      else -> 0.03
    }
  }

  /**
   * Возвращает **один** ItemStack.
   */
  private fun createItemStack(lootItem: LootTableConfigurationModel.LootItem, bossLevel: Int): ItemStack {
    val mat = lootItem.material
    val baseAmount = lootItem.amount.sample()

    val settings    = lootTable.lootSettings

    // Δ уровней относительно minLevel предмета (ниже минимума предмет просто не попадёт в выборку)
    val levelDelta  = (bossLevel - lootItem.minLevel).coerceAtLeast(0)
    val levelFactor = 1.0 + settings.perLevelDropFactor * levelDelta


    val rawAmount     = baseAmount * levelFactor
    val scaledAmount  = kotlin.math.ceil(rawAmount).toInt()

    val maxStack = mat.maxStackSize.coerceAtLeast(1)
    if (scaledAmount > maxStack) {
      plugin.logger.warn { "Loot amount clamped: requested=$scaledAmount, maxStack=$maxStack for $mat (base=$baseAmount, Δ=$levelDelta, levelFactor=$levelFactor)" }
    }
    val finalAmount = scaledAmount.coerceIn(1, maxStack)

    plugin.logger.debug { "Creating ItemStack: ${finalAmount}x ${mat} (base=${baseAmount}, level_factor=${String.format("%.2f", levelFactor)})" }

    val stack = ItemStack(mat, finalAmount)

    lootItem.enchantments?.forEach { (ench, range) ->
      val enchLevel = range.sample().coerceAtLeast(1)
      stack.addUnsafeEnchantment(ench, enchLevel)
      plugin.logger.debug { "Applied enchantment ${ench} level ${enchLevel} to ${mat}" }
    }
    return stack
  }

  /**
   * Применяет кастомное название и лор к предмету из конфигурации
   */
  private fun applyCustomMetadata(stack: ItemStack, lootItem: LootTableConfigurationModel.LootItem) {
    val meta = stack.itemMeta ?: return

    var hasChanges = false

    // 1) Опционально поддерживаем &-коды из конфигов (если ты уже кладёшь § — просто оставь как есть)
    fun colorize(s: String) = org.bukkit.ChatColor.translateAlternateColorCodes('&', s)

    // 2) Кастомное название
    lootItem.title?.let { rawTitle ->
      // Снимаем "курсив по умолчанию" через §r в начале (для legacy-API).
      val title = buildString {
        if (!rawTitle.startsWith("§r")) append("§r")
        append(colorize(rawTitle))
      }
      meta.setDisplayName(title)
      hasChanges = true
      plugin.logger.debug { "Applied custom title to ${stack.type}: ${rawTitle}" }
    }

    // 3) Лор
    lootItem.lore?.let { customLoreRaw ->
      val customLore = customLoreRaw.map(::colorize)
      val existingLore: List<String> = meta.lore ?: emptyList()

      // Если хотим добавить свой лор сверху и сохранить старый — разделим пустой строкой.
      val combined = if (existingLore.isNotEmpty())
        buildList { addAll(customLore); add(""); addAll(existingLore) }
      else
        customLore

      meta.lore = combined
      hasChanges = true
      plugin.logger.debug { "Applied custom lore to ${stack.type}: ${customLoreRaw.size} lines" }
    }

    // Если ничего не меняли — это присваивание безопасно, но можно пропустить.
    if (hasChanges) {
      stack.itemMeta = meta
    }
  }

  /**
   * LootManager не содержит состояния для очистки
   */
  fun cleanup() {
    plugin.logger.debug { "LootManager: no state to cleanup" }
  }

  companion object {
    private val CLASS_HIERARCHY = listOf("common", "uncommon", "rare", "epic", "legendary", "mythic")
  }
}