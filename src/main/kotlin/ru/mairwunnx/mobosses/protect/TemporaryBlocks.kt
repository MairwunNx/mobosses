package ru.mairwunnx.mobosses.protect

import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Blocks an ability placed for a while, like cobwebs. Each goes back to what was there when its time
 * is up, when the plugin stops, or, after a crash, on the next start: the list is kept in
 * `temporary-blocks.txt` until they're all back. A block someone changed in between stays as it is.
 */
class TemporaryBlocks(private val plugin: MoBossesPlugin, private val changes: BlockChanges) {
  private class Entry(val block: Block, val placed: BlockData, val original: BlockData, var task: BukkitTask? = null)

  private val entries = LinkedHashMap<String, Entry>()
  private val file get() = plugin.dataFolder.resolve("temporary-blocks.txt")

  fun originalAt(block: Block): BlockData? = entries[id(block)]?.original

  fun add(block: Block, placed: BlockData, original: BlockData, ticks: Long) {
    entries.remove(id(block))?.task?.cancel()
    val entry = Entry(block, placed, original)
    entry.task = Bukkit.getScheduler().runTaskLater(plugin, Runnable { restore(entry) }, ticks.coerceAtLeast(1))
    entries[id(block)] = entry
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
    entries.remove(id(entry.block))
    if (entry.block.blockData == entry.placed) changes.put(entry.block, entry.original)
    if (save) save()
  }

  private fun save() {
    if (entries.isEmpty()) {
      file.delete()
      return
    }
    file.parentFile.mkdirs()
    file.writeText(entries.values.joinToString("\n") { e ->
      listOf(e.block.world.name, e.block.x, e.block.y, e.block.z, e.placed.asString, e.original.asString).joinToString("\t")
    })
  }

  private fun parse(line: String): Entry? = runCatching {
    val (world, x, y, z, placed, original) = line.split('\t').also { require(it.size == 6) }
    val block = Bukkit.getWorld(world)!!.getBlockAt(x.toInt(), y.toInt(), z.toInt())
    Entry(block, Bukkit.createBlockData(placed), Bukkit.createBlockData(original))
  }.getOrNull()

  private fun id(block: Block) = "${block.world.name} ${block.x} ${block.y} ${block.z}"

  private operator fun <T> List<T>.component6() = this[5]
}
