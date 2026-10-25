package ru.mairwunnx.mobosses.neoforge

import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import ru.mairwunnx.mobosses.modcore.Loader
import java.nio.file.Path

/** What NeoForge does its own way, for modcore. */
object OnNeoForge : Loader {
  override val id = "neoforge"
  override val name = "NeoForge"
  // FML wants a number, so a dev build carries 0.0.0-dev; Mo’Bosses calls it dev on every platform.
  override val version: String
    get() = ModList.get().getModContainerById("mobosses").map { it.modInfo.version.toString() }.orElse("dev").removePrefix("0.0.0-")
  override val config: Path
    get() = FMLPaths.CONFIGDIR.get()
}
