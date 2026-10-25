package ru.mairwunnx.mobosses.modcore.client.viewers

import me.shedaniel.math.Point
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.gui.Renderer
import me.shedaniel.rei.api.client.gui.widgets.Widget
import me.shedaniel.rei.api.client.gui.widgets.Widgets
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry
import me.shedaniel.rei.api.client.registry.display.DisplayCategory
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry
import me.shedaniel.rei.api.common.category.CategoryIdentifier
import me.shedaniel.rei.api.common.display.DisplaySerializer
import me.shedaniel.rei.api.common.display.basic.BasicDisplay
import me.shedaniel.rei.api.common.util.EntryIngredients
import me.shedaniel.rei.api.common.util.EntryStacks
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.client.ClientLoot
import ru.mairwunnx.mobosses.modcore.client.ClientTexts
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Bosses' loot in REI: a page per entry, the item and the lines the server wrote for it, as [JeiLoot] shows them in
 * JEI. The pages come with the world, after REI has read its plugins, so they join its displays when they arrive.
 * Only loaded when REI runs: Fabric names it as REI's entry point, NeoForge finds a subclass by REI's annotation.
 */
open class ReiLoot : REIClientPlugin {
  class Page(val page: Payloads.LootPage) : BasicDisplay(emptyList(), listOf(EntryIngredients.of(page.stack))) {
    override fun getCategoryIdentifier(): CategoryIdentifier<*> = ID
    // A page lives in the player's game alone: nothing sends it anywhere.
    override fun getSerializer(): DisplaySerializer<out Page>? = null
  }

  private object Category : DisplayCategory<Page> {
    override fun getCategoryIdentifier(): CategoryIdentifier<Page> = ID
    override fun getTitle(): Component = ClientTexts.text(Message.LOOT_PAGE_TITLE)
    override fun getIcon(): Renderer = EntryStacks.of(Items.ZOMBIE_HEAD)
    override fun getDisplayHeight() = 54
    override fun getDisplayWidth(display: Page) = 228

    override fun setupDisplay(display: Page, bounds: Rectangle): List<Widget> = buildList {
      add(Widgets.createRecipeBase(bounds))
      add(Widgets.createSlot(Point(bounds.x + 5, bounds.y + 5)).entries(display.outputEntries.first()).markOutput())
      display.page.lines.forEachIndexed { i, line ->
        add(Widgets.createLabel(Point(bounds.x + 26, bounds.y + 5 + i * 11), ClientLoot.plain(line)).leftAligned().noShadow().color(ClientLoot.INK, 0xFFBBBBBB.toInt()))
      }
    }
  }

  override fun registerCategories(registry: CategoryRegistry) {
    registry.add(Category)
  }

  override fun registerDisplays(registry: DisplayRegistry) {
    ClientLoot.pages.forEach { registry.add(Page(it)) }
    if (!listening) {
      listening = true
      ClientLoot.listen { pages -> DisplayRegistry.getInstance().let { displays -> pages.forEach { displays.add(Page(it)) } } }
    }
  }

  companion object {
    val ID: CategoryIdentifier<Page> = CategoryIdentifier.of("mobosses", "loot")
    private var listening = false
  }
}
