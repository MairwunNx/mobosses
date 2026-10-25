package ru.mairwunnx.mobosses.modcore.protect

import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Scheduler

/**
 * Blocks an ability placed for a while, like cobwebs, as on Paper. Each goes back to what was there when its time is
 * up, when the server stops, or, after a crash, on the next start: the list is kept in the world's
 * `mobosses/temporary-blocks.txt` until they're all back. A block someone changed in between stays as it is.
 */
class TemporaryBlocks(private val mod: MoBosses, private val changes: BlockChanges) {
  private class Entry(val level: ServerLevel, val pos: BlockPos, val placed: BlockState, val original: BlockState, var task: Scheduler.Task? = null)

  private val entries = LinkedHashMap<String, Entry>()
  private val file get() = mod.data.resolve("temporary-blocks.txt")

  fun originalAt(level: ServerLevel, pos: BlockPos): BlockState? = entries[id(level, pos)]?.original

  fun contains(level: ServerLevel, pos: BlockPos) = id(level, pos) in entries

  /** A player broke it: nothing to put back. */
  fun forget(level: ServerLevel, pos: BlockPos) {
    entries.remove(id(level, pos))?.task?.cancel() ?: return
    save()
  }

  fun add(level: ServerLevel, pos: BlockPos, placed: BlockState, original: BlockState, ticks: Long) {
    entries.remove(id(level, pos))?.task?.cancel()
    val entry = Entry(level, pos.immutable(), placed, original)
    entry.task = mod.scheduler.later(ticks.coerceAtLeast(1)) { restore(entry) }
    entries[id(level, pos)] = entry
    save()
  }

  /** Puts every block back now, for shutdown. */
  fun restoreAll() {
    entries.values.toList().forEach { it.task?.cancel(); restore(it, save = false) }
    save()
  }

  /** Puts back what the last run left behind, if it stopped without doing so. */
  fun recover() {
    if (!file.isFile) return
    file.readLines().mapNotNull(::parse).forEach { restore(it, save = false) }
    save()
  }

  private fun restore(entry: Entry, save: Boolean = true) {
    entries.remove(id(entry.level, entry.pos))
    // By block: fire changes its age while it burns.
    if (entry.level.getBlockState(entry.pos).block == entry.placed.block) changes.put(entry.level, entry.pos, entry.original)
    if (save) save()
  }

  private fun save() {
    if (entries.isEmpty()) {
      file.delete()
      return
    }
    file.parentFile.mkdirs()
    file.writeText(entries.values.joinToString("\n") { e ->
      listOf(e.level.dimension().identifier(), e.pos.x, e.pos.y, e.pos.z, BlockStateParser.serialize(e.placed), BlockStateParser.serialize(e.original)).joinToString("\t")
    })
  }

  private fun parse(line: String): Entry? = runCatching {
    val parts = line.split('\t').also { require(it.size == 6) }
    val level = mod.server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(parts[0])))!!
    val blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK)
    fun state(text: String) = BlockStateParser.parseForBlock(blocks, text, false).blockState()
    Entry(level, BlockPos(parts[1].toInt(), parts[2].toInt(), parts[3].toInt()), state(parts[4]), state(parts[5]))
  }.getOrNull()

  private fun id(level: ServerLevel, pos: BlockPos) = "${level.dimension().identifier()} ${pos.x} ${pos.y} ${pos.z}"
}
