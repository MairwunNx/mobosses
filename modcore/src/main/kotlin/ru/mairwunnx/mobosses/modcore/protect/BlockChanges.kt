package ru.mairwunnx.mobosses.modcore.protect

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * The only way abilities touch blocks, as on Paper: each change is checked against the [Guard]s and then made.
 * Temporary ones go through [TemporaryBlocks]. The mods have no block log yet, so nothing is logged.
 */
class BlockChanges(private val mod: MoBosses) {
  val temporary = TemporaryBlocks(mod, this)

  /** Sets a block if the guards allow it; false when they don't. */
  fun set(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean {
    if (!mod.guards.canChangeBlock(level, pos)) return false
    put(level, pos, state)
    return true
  }

  /** Sets a block for [ticks], then puts back what was there. */
  fun setFor(level: ServerLevel, pos: BlockPos, state: BlockState, ticks: Long): Boolean {
    val original = temporary.originalAt(level, pos) ?: level.getBlockState(pos)
    if (!set(level, pos, state)) return false
    temporary.add(level, pos, state, original, ticks)
    return true
  }

  /** Makes a change without asking the guards: for putting things back. */
  internal fun put(level: ServerLevel, pos: BlockPos, state: BlockState) {
    level.setBlock(pos, state, Block.UPDATE_ALL)
  }
}
