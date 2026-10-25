package ru.mairwunnx.mobosses.fabric

import net.fabricmc.loader.api.FabricLoader
import ru.mairwunnx.mobosses.modcore.Loader
import java.nio.file.Path

/** What Fabric does its own way, for modcore. */
object OnFabric : Loader {
  override val id = "fabric"
  override val name = "Fabric"
  override val version: String
    get() = FabricLoader.getInstance().getModContainer("mobosses").map { it.metadata.version.friendlyString }.orElse("dev")
  override val config: Path
    get() = FabricLoader.getInstance().configDir
}
