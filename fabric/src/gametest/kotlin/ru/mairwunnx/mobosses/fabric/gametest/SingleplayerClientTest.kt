package ru.mairwunnx.mobosses.fabric.gametest

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.level.storage.LevelResource
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.modcore.client.ClientLoot
import ru.mairwunnx.mobosses.modcore.client.SettingsScreen
import ru.mairwunnx.mobosses.modcore.client.viewers.JeiLoot
import ru.mairwunnx.mobosses.modcore.client.SettingsSource
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * A real client in a fresh singleplayer world: Mo’Bosses starts on the game's own server, keeps its database in the
 * world's save and its configs in `config/mobosses/`, reads the client's language, lets the world's owner reload
 * without cheats, spawns a boss ahead of the player, and opens the settings screen in the menu and in the world, where
 * a change reaches the world's server's files, and shows the loot pages in JEI. `./gradlew :fabric:runClientGameTest` opens a
 * game window.
 */
class SingleplayerClientTest : FabricClientGameTest {
  override fun runTest(context: ClientGameTestContext) {
    context.runOnClient<RuntimeException> { it.options.languageCode = "ru_ru" }
    // From the main menu, the settings screen edits the game's own files.
    context.setScreen { SettingsScreen(null) }
    context.waitTicks(10)
    context.takeScreenshot("mobosses-settings-menu")
    context.setScreen { null }
    context.worldBuilder().create().use { world ->
      world.connection.waitForChunksRender()
      val problems = world.server.computeOnServer<List<String>, RuntimeException> { server ->
        val mod = Mod.current
        val owner = server.playerList.players.single()
        listOfNotNull(
          "Mo’Bosses didn't start in a singleplayer world".takeIf { mod == null },
          "the database isn't inside the world's save".takeUnless { mod?.data?.toPath()?.normalize()?.startsWith(server.getWorldPath(LevelResource.ROOT).normalize()) == true },
          "the configs aren't in config/mobosses/".takeUnless { mod?.folder == FabricLoader.getInstance().configDir.resolve("mobosses").toFile() && mod.folder.resolve("config.yml").exists() },
          "the owner reads ${mod?.texts?.languageOf(owner)}, not the client's Russian".takeUnless { mod?.texts?.languageOf(owner) == "ru" },
          "the world's owner can't reload without cheats".takeUnless { Perms.check(owner.createCommandSourceStack(), Permissions.RELOAD) },
        )
      }
      check(problems.isEmpty()) { problems.joinToString("; ") }
      // Two blocks ahead of the player, as their own /mobosses spawn would put it.
      world.server.runCommand("execute as @p at @s run mobosses spawn zombie 20 rare")
      context.waitTicks(20)
      val made = world.server.computeOnServer<Int, RuntimeException> { Mod.current?.bosses?.all?.size ?: 0 }
      check(made == 1) { "the player's spawn made $made bosses" }

      // In a world, the screen edits the world's server's files: the owner's in singleplayer.
      context.setScreen { SettingsScreen(null) }
      context.waitTicks(20)
      context.takeScreenshot("mobosses-settings-world")
      var answer: SettingsSource.State? = null
      context.runOnClient<RuntimeException> { SettingsSource.Remote.save("config.yml", listOf(Payloads.Change(listOf("anti_cheese", "step_out", "stuck_seconds"), "7"))) { answer = it } }
      context.waitFor { answer != null }
      check(answer?.failed == false) { "the server refused the change: ${answer?.message}" }
      val saved = world.server.computeOnServer<String, RuntimeException> { Mod.current!!.folder.resolve("config.yml").readText() }
      check("    stuck_seconds: 7" in saved) { "the server's config.yml didn't change" }
      context.setScreen { null }

      // The loot table reached the player's JEI: a page for each entry.
      val entries = world.server.computeOnServer<Int, RuntimeException> { Mod.current!!.catalog.loot.size }
      context.waitFor { ClientLoot.pages.size == entries }
      context.runOnClient<RuntimeException> {
        val runtime = JeiLoot.runtime ?: error("JEI isn't on")
        runtime.recipesGui.showTypes(listOf(JeiLoot.TYPE))
      }
      context.waitTicks(20)
      context.takeScreenshot("mobosses-loot-jei")
      context.setScreen { null }
    }
  }
}
