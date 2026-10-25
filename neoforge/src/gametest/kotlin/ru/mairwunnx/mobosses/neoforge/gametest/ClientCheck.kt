package ru.mairwunnx.mobosses.neoforge.gametest

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import net.minecraft.world.level.storage.LevelResource
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModLoader
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.gui.LoadingErrorScreen
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.modcore.client.SettingsScreen
import ru.mairwunnx.mobosses.modcore.client.SettingsSource
import ru.mairwunnx.mobosses.modcore.net.Payloads
import java.io.File

/**
 * A real client in a fresh singleplayer world without cheats, as Fabric's client game test opens one: Mo’Bosses starts
 * on the game's own server, keeps its database in the world's save and its configs in `config/mobosses/`, reads the
 * client's language, lets the world's owner reload without cheats, spawns a boss ahead of the player, and takes a
 * change from the settings screen into the world's server's config.yml. The result
 * goes to `mobosses-check.txt` in the run folder, then the game closes; `./gradlew :neoforge:runClientGameTest` fails
 * unless it says `ok`.
 */
object ClientCheck {
  fun start() {
    if (System.getProperty("mobosses.clientGameTest") != "true" || FMLEnvironment.getDist() != Dist.CLIENT) return
    Client.start()
  }

  private object Client {
    private var created = false
    private var joinedTicks = 0
    private var spawnedAt = -1
    private var saved: Boolean? = null
    private var done = false
    private var closeIn = -1
    private var ticks = 0
    private val early = mutableListOf<String>()
    private val log = LoggerFactory.getLogger("Mo’Bosses check")

    fun start() {
      NeoForge.EVENT_BUS.addListener { _: ClientTickEvent.Post -> tick() }
    }

    private fun tick() {
      val game = Minecraft.getInstance()
      if (done) {
        if (closeIn > 0 && --closeIn == 0) game.stop()
        return
      }
      // Three minutes is far more than a world takes; a check that never ran fails.
      if (++ticks > 20 * 180) return finish(game, listOf("the world didn't open within three minutes"))
      // NeoForge lists loading warnings first; another mod's are its own business, Mo’Bosses' fail the check.
      if (!created && game.gui.screen() is LoadingErrorScreen) {
        ModLoader.getLoadingIssues().filter { it.affectedMod()?.modId?.startsWith("mobosses") == true }.forEach { early += "NeoForge warns about Mo’Bosses: $it" }
        game.gui.setScreen(TitleScreen())
        return
      }
      // A fresh game folder asks about accessibility first, as a player's first start does.
      if (!created && game.gui.screen() is AccessibilityOnboardingScreen) {
        game.options.onboardingAccessibilityFinished()
        game.gui.setScreen(TitleScreen())
        return
      }
      if (!created && game.gui.screen() is TitleScreen) {
        created = true
        game.options.languageCode = "ru_ru"
        val name = "mobosses-check-${System.currentTimeMillis()}"
        val settings = LevelSettings(name, GameType.SURVIVAL, LevelSettings.DifficultySettings.DEFAULT, false, WorldDataConfiguration.DEFAULT)
        game.createWorldOpenFlows().createFreshLevel(name, settings, WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, TitleScreen())
        return
      }
      val server = game.singleplayerServer ?: return
      if (game.player == null || ++joinedTicks < 40) return
      if (spawnedAt < 0) {
        spawnedAt = joinedTicks
        // Two blocks ahead of the player, as their own /mobosses spawn would put it; the console runs it, since the world has no cheats.
        server.execute { server.commands.performPrefixedCommand(server.createCommandSourceStack(), "execute as @p at @s run mobosses spawn zombie 20 rare") }
        return
      }
      if (joinedTicks < spawnedAt + 20) return
      // The settings screen in the world: a change reaches the world's server's config.yml.
      if (saved == null) {
        saved = false
        game.gui.setScreen(SettingsScreen(null))
        SettingsSource.Remote.save("config.yml", listOf(Payloads.Change(listOf("anti_cheese", "step_out", "stuck_seconds"), "7"))) { saved = !it.failed }
        return
      }
      if (saved == false && joinedTicks < spawnedAt + 100) return
      done = true
      val problems = runCatching {
        server.submit<List<String>> {
          val mod = Mod.current
          val owner = server.playerList.players.single()
          listOfNotNull(
            "Mo’Bosses didn't start in a singleplayer world".takeIf { mod == null },
            "the database isn't inside the world's save".takeUnless { mod?.data?.toPath()?.normalize()?.startsWith(server.getWorldPath(LevelResource.ROOT).normalize()) == true },
            "the configs aren't in config/mobosses/".takeUnless { mod?.folder == FMLPaths.CONFIGDIR.get().resolve("mobosses").toFile() && mod.folder.resolve("config.yml").exists() },
            "the owner reads ${mod?.texts?.languageOf(owner)}, not the client's Russian".takeUnless { mod?.texts?.languageOf(owner) == "ru" },
            "the world's owner can't reload without cheats".takeUnless { Perms.check(owner.createCommandSourceStack(), Permissions.RELOAD) },
            "the player's spawn made ${mod?.bosses?.all?.size} bosses".takeUnless { mod?.bosses?.all?.size == 1 },
            "the settings screen's change didn't reach config.yml".takeUnless { saved == true && mod?.folder?.resolve("config.yml")?.readText()?.contains("    stuck_seconds: 7") == true },
          )
        }.get()
      }.getOrElse { listOf("the check failed: $it") }
      finish(game, early + problems)
    }

    private fun finish(game: Minecraft, problems: List<String>) {
      done = true
      val result = if (problems.isEmpty()) "ok" else problems.joinToString("\n")
      log.info("Singleplayer check: {}", result)
      File(game.gameDirectory, "mobosses-check.txt").writeText(result + "\n")
      closeIn = 20
    }
  }
}
