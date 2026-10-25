package ru.mairwunnx.mobosses.fabric.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import ru.mairwunnx.mobosses.modcore.client.SettingsScreen

/** Mod Menu's settings button for Mo’Bosses: the game's own files from the main menu, the server's in a world. */
class ModMenu : ModMenuApi {
  override fun getModConfigScreenFactory() = ConfigScreenFactory { parent -> SettingsScreen(parent) }
}
