package ru.mairwunnx.mobosses.fabric.gametest

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.level.storage.LevelResource
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.Perms

/**
 * A real client in a fresh singleplayer world: Mo’Bosses starts on the game's own server, keeps its database in the
 * world's save and its configs in `config/mobosses/`, reads the client's language, lets the world's owner reload
 * without cheats, and spawns a boss ahead of the player. `./gradlew :fabric:runClientGameTest` opens a
 * game window.
 */
class SingleplayerClientTest : FabricClientGameTest {
  override fun runTest(context: ClientGameTestContext) {
    context.runOnClient<RuntimeException> { it.options.languageCode = "ru_ru" }
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
    }
  }
}
