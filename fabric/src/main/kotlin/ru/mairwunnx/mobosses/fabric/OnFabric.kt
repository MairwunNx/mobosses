package ru.mairwunnx.mobosses.fabric

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import ru.mairwunnx.mobosses.modcore.Loader
import java.nio.file.Path

/** Fabric as modcore's loader: Fabric Loader's mod list and folders, and fabric-permissions-api. */
object OnFabric : Loader {
  override val id = "fabric"
  override val name = "Fabric"
  override val version: String
    get() = FabricLoader.getInstance().getModContainer("mobosses").map { it.metadata.version.friendlyString }.orElse("dev")
  override val config: Path
    get() = FabricLoader.getInstance().configDir

  override fun loaded(mod: String) = FabricLoader.getInstance().isModLoaded(mod)

  // fabric-permissions-api comes with LuckPerms; its classes are touched only when it's loaded.
  private val permissions by lazy { loaded("fabric-permissions-api-v0") }

  override fun permission(source: CommandSourceStack, node: String, fallback: Boolean) =
    if (permissions) me.lucko.fabric.api.permissions.v0.Permissions.check(source, node, fallback) else fallback
}
