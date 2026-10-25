package ru.mairwunnx.mobosses.modcore

import net.minecraft.commands.CommandSourceStack
import java.nio.file.Path

/**
 * What a loader hands modcore: its id and name, Mo’Bosses' version, where configs live, its mod list, and its
 * permissions. A difference between Fabric and NeoForge goes here, never into modcore's code as an `if`.
 */
interface Loader {
  /** `fabric` or `neoforge`. */
  val id: String

  /** The loader's name as people know it, like `Fabric`. */
  val name: String

  /** Mo’Bosses' own version, from the mod's metadata. */
  val version: String

  /** The game's `config/` folder; Mo’Bosses' files go to `config/mobosses/`. */
  val config: Path

  /**
   * A file from Mo’Bosses' own jar, like `config.yml`: never through the class loader, which on Fabric finds the first
   * mod's file of that name, often another mod's own `config.yml`. Null when there's no such file.
   */
  fun resource(path: String): ByteArray?

  /** Whether a mod with this id runs. */
  fun loaded(mod: String): Boolean

  /**
   * Whether [source] has [node], through the loader's permissions API when a permissions mod answers it (LuckPerms
   * does), else [fallback].
   */
  fun permission(source: CommandSourceStack, node: String, fallback: Boolean): Boolean
}
