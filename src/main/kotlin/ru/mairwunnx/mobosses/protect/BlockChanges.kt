package ru.mairwunnx.mobosses.protect

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * The only way abilities touch blocks: each change is checked against the [Guard]s, logged before it
 * happens, in the same tick, and then made. Temporary ones go through [TemporaryBlocks].
 */
class BlockChanges(private val plugin: MoBossesPlugin) {
  val temporary = TemporaryBlocks(plugin, this)

  /** Sets a block if the guards allow it; false when they don't. */
  fun set(block: Block, data: BlockData): Boolean {
    if (!plugin.guards.canChangeBlock(block.location)) return false
    put(block, data)
    return true
  }

  /** Sets a block for [ticks], then puts back what was there. */
  fun setFor(block: Block, data: BlockData, ticks: Long): Boolean {
    val original = temporary.originalAt(block) ?: block.blockData
    if (!set(block, data)) return false
    temporary.add(block, data, original, ticks)
    return true
  }

  /** Logs and makes a change, without asking the guards: for putting things back. */
  internal fun put(block: Block, data: BlockData) {
    val log = plugin.blockLog
    val old = block.blockData
    if (old.material != Material.AIR) log.removed(block.location, old)
    if (data.material != Material.AIR) log.placed(block.location, data)
    block.setBlockData(data, true)
  }
}
