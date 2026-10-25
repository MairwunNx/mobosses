package ru.mairwunnx.mobosses.modcore.client.viewers

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.client.ClientLoot
import ru.mairwunnx.mobosses.modcore.client.ClientTexts
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * Bosses' loot in JEI, and in EMI, which reads JEI's plugins: a page per entry, the item and the lines the server
 * wrote for it. The pages come with the world, after JEI has built its lists, so they join them at runtime. Only
 * loaded when JEI runs: Fabric names it as JEI's entry point, NeoForge finds it by the annotation.
 */
@JeiPlugin
class JeiLoot : IModPlugin {
  private var shown: List<Payloads.LootPage> = emptyList()

  override fun getPluginUid(): Identifier = Identifier.fromNamespaceAndPath("mobosses", "loot")

  override fun registerCategories(registration: IRecipeCategoryRegistration) {
    registration.addRecipeCategories(Category(registration.jeiHelpers.guiHelper))
  }

  override fun onRuntimeAvailable(runtime: IJeiRuntime) {
    Companion.runtime = runtime
    shown = emptyList()
    show(ClientLoot.pages)
    if (!listening) {
      listening = true
      ClientLoot.listen { show(it) }
    }
  }

  override fun onRuntimeUnavailable() {
    Companion.runtime = null
  }

  private fun show(pages: List<Payloads.LootPage>) {
    val manager = runtime?.recipeManager ?: return
    if (shown.isNotEmpty()) manager.hideRecipes(TYPE, shown)
    shown = pages
    if (pages.isNotEmpty()) manager.addRecipes(TYPE, pages)
  }

  private class Category(helper: IGuiHelper) : IRecipeCategory<Payloads.LootPage> {
    private val icon = helper.createDrawableItemLike(Items.ZOMBIE_HEAD)

    override fun getRecipeType(): IRecipeType<Payloads.LootPage> = TYPE
    override fun getTitle(): Component = ClientTexts.text(Message.LOOT_PAGE_TITLE)
    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT
    override fun getIcon(): IDrawable = icon

    override fun setRecipe(builder: IRecipeLayoutBuilder, page: Payloads.LootPage, focuses: IFocusGroup) {
      builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1).add(page.stack)
    }

    override fun draw(page: Payloads.LootPage, slots: IRecipeSlotsView, graphics: GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
      val font = Minecraft.getInstance().font
      page.lines.forEachIndexed { i, line -> graphics.text(font, line, 22, 1 + i * 11, -1) }
    }
  }

  companion object {
    private const val WIDTH = 220
    private const val HEIGHT = 46
    val TYPE: IRecipeType<Payloads.LootPage> = IRecipeType.create("mobosses", "loot", Payloads.LootPage::class.java)
    private var listening = false

    /** JEI's runtime while a world is open, for anything that shows a page. */
    @Volatile var runtime: IJeiRuntime? = null
  }
}
