package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.component.ItemLore
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * A page of the `/mobosses` menu, as on Paper: a chest the client already knows how to draw, whose items are only
 * pictures. Its [View] takes no click, drag, or shift-click, so nothing moves in or out, and clicks on its own slots
 * become [click] calls.
 */
abstract class Page(protected val mod: MoBosses, val player: ServerPlayer, private val rows: Int) {
  private val container = SimpleContainer(rows * 9)

  abstract fun click(slot: Int, right: Boolean, shift: Boolean)

  fun open(title: Component) {
    player.openMenu(SimpleMenuProvider({ id, inventory, _ -> View(id, inventory, this, rows) }, mod.texts.vanilla(title)))
  }

  fun close() = player.closeContainer()

  fun set(slot: Int, item: ItemStack?) = container.setItem(slot, item ?: ItemStack.EMPTY)

  /** Runs a `/mobosses` command as the player, as Bukkit's `performCommand`. */
  fun command(line: String) = mod.server.commands.performPrefixedCommand(player.createCommandSourceStack(), line)

  /** An item named by the first of [lines], with the rest for its lore. */
  fun item(item: Item, lines: List<Component>): ItemStack = name(ItemStack(item), lines.firstOrNull(), lines.drop(1))

  fun name(item: ItemStack, name: Component?, lore: List<Component>): ItemStack = item.apply {
    fun plain(text: Component) = mod.texts.vanilla(text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
    name?.let { set(DataComponents.CUSTOM_NAME, plain(it)) }
    if (lore.isNotEmpty()) set(DataComponents.LORE, ItemLore(lore.map(::plain)))
  }

  fun ItemStack.glint(on: Boolean) = apply { if (on) set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) }

  /** The chest the client draws; every way to move an item through it ends here. */
  class View(id: Int, inventory: Inventory, val page: Page, rows: Int) : ChestMenu(TYPES.getValue(rows), id, inventory, page.container, rows) {
    override fun clicked(slotIndex: Int, buttonNum: Int, containerInput: ContainerInput, player: Player) {
      // Shift-clicks from the player's inventory, number keys, double clicks: all of it stops here.
      if (player.uuid != page.player.uuid || slotIndex !in 0 until page.container.containerSize) return
      if (containerInput == ContainerInput.PICKUP || containerInput == ContainerInput.QUICK_MOVE) page.click(slotIndex, buttonNum == 1, containerInput == ContainerInput.QUICK_MOVE)
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun stillValid(player: Player) = true
  }

  companion object {
    private val TYPES = mapOf(3 to MenuType.GENERIC_9x3, 6 to MenuType.GENERIC_9x6)

    /** A mob type's spawn egg, or a zombie's for a mob without one. */
    fun egg(type: EntityType<*>): Item = SpawnEggItem.byId(type).map { it.value() }.orElse(Items.ZOMBIE_SPAWN_EGG)
  }
}
