package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World

/**
 * The languages Mo’Bosses speaks, each by its own name, for the player to pick one, or Automatic to have Mo’Bosses
 * pick by country and game language, as on Paper. The current one glints; a pick goes back to the menu.
 */
class LanguagePage private constructor(mod: MoBosses, player: ServerPlayer, private val codes: List<String>) : Page(mod, player, 3) {
  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      AUTO -> picked(null)
      BACK -> Menu.open(mod, player)
      CLOSE -> close()
      in SLOTS -> codes.getOrNull(SLOTS.indexOf(slot))?.let(::picked)
    }
  }

  /** Back to the menu, already in the new language. */
  private fun picked(code: String?) {
    mod.playerLanguages.choose(player, code)
    World.sound(player, SoundEvents.UI_BUTTON_CLICK, volume = 0.5f, pitch = 1.2f)
    Menu.open(mod, player)
  }

  companion object {
    private const val AUTO = 10
    private const val BACK = 18
    private const val CLOSE = 26
    private val SLOTS = listOf(12, 13, 14, 15, 16, 20, 21, 22, 23, 24, 25)

    fun open(mod: MoBosses, player: ServerPlayer) {
      val languages = mod.languages
      val code = mod.texts.languageOf(player)
      val chosen = mod.playerLanguages.chosen(player)
      val page = LanguagePage(mod, player, mod.playerLanguages.codes().take(SLOTS.size))
      val current = languages.render(code, Message.LANGUAGE_CURRENT)
      val pick = languages.render(code, Message.LANGUAGE_PICK)
      fun state(on: Boolean) = listOfNotNull(Component.empty(), if (on) current else pick)

      with(page) {
        set(AUTO, item(Items.COMPASS, languages.lines(code, Message.LANGUAGE_AUTOMATIC) + state(chosen == null)).glint(chosen == null))
        codes.forEachIndexed { i, language ->
          val name = Component.text(LanguageChoices.nativeName(language), NamedTextColor.YELLOW)
          val lore = listOf(Component.text(language, NamedTextColor.DARK_GRAY)) + state(language == chosen)
          set(SLOTS[i], item(Items.PAPER, listOf(name) + lore).glint(language == chosen))
        }
        set(BACK, item(Items.ARROW, languages.lines(code, Message.MENU_BACK)))
        set(CLOSE, item(Items.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
        open(languages.render(code, Message.LANGUAGE_TITLE) ?: Component.text("Mo’Bosses"))
      }
    }
  }
}
