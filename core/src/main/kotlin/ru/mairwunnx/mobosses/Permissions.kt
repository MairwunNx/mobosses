package ru.mairwunnx.mobosses

/**
 * Mo’Bosses' permissions and who has them by default, as `paper-plugin.yml` lists them for Paper. The mods ask a
 * permissions mod first and fall back to these: everyone gets [USE] and [RESET], operators everything, which
 * [ADMIN] holds.
 */
object Permissions {
  const val USE = "mobosses.use"
  const val INFO_OTHERS = "mobosses.info.others"
  const val RESET = "mobosses.reset"
  const val RESET_OTHERS = "mobosses.reset.others"
  const val BOSSES = "mobosses.bosses"
  const val RELOAD = "mobosses.reload"
  const val SPAWN = "mobosses.spawn"
  const val KILLALL = "mobosses.killall"
  /** Changing the server's files from a player's settings screen: only the owner's operators, level 4, by default. */
  const val CONFIG = "mobosses.config"
  const val ADMIN = "mobosses.admin"

  /** Everyone's, unless a permissions mod says otherwise. */
  val EVERYONE = setOf(USE, RESET)

  /** What [ADMIN] holds. */
  val CHILDREN = setOf(USE, INFO_OTHERS, RESET, RESET_OTHERS, BOSSES, RELOAD, SPAWN, KILLALL)

  val ALL = CHILDREN + CONFIG + ADMIN

  /** Whether someone has [node] when nothing but the game decides: [operator] for level 2, [owner] for level 4. */
  fun byDefault(node: String, operator: Boolean, owner: Boolean): Boolean = when (node) {
    in EVERYONE -> true
    CONFIG -> owner
    else -> operator
  }
}
