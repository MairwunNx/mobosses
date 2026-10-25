package ru.mairwunnx.mobosses.modcore

import java.nio.file.Path

/**
 * What a loader hands modcore: its id and name, Mo’Bosses' version, and where configs live. Grows with the mods; a
 * difference between Fabric and NeoForge goes here, never into modcore's code as an `if`.
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
}
