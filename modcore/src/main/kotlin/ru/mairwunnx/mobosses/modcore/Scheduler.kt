package ru.mairwunnx.mobosses.modcore

/**
 * Work for later on the server thread, counted in the server's ticks, as Bukkit's scheduler does. [tick] runs at the
 * end of every server tick; [stop] drops everything when the server stops, so nothing reaches into the next world.
 */
class Scheduler {
  inner class Task internal constructor(private var wait: Long, private val period: Long, private val block: (Task) -> Unit) {
    var cancelled = false; private set

    fun cancel() {
      cancelled = true
    }

    /** True when it's done for good. */
    internal fun tick(): Boolean {
      if (cancelled) return true
      if (--wait > 0) return false
      block(this)
      if (period <= 0 || cancelled) return true
      wait = period
      return false
    }
  }

  private val tasks = ArrayList<Task>()
  private val added = ArrayList<Task>()

  /** Runs [block] once, [ticks] from now; 0 means the next tick. */
  fun later(ticks: Long, block: () -> Unit): Task = schedule(ticks.coerceAtLeast(1), 0) { block() }

  /** Runs [block] every [period] ticks, the first time [delay] from now. */
  fun every(period: Long, delay: Long = period, block: (Task) -> Unit): Task = schedule(delay.coerceAtLeast(1), period.coerceAtLeast(1), block)

  private fun schedule(wait: Long, period: Long, block: (Task) -> Unit) = Task(wait, period, block).also { added += it }

  fun tick() {
    tasks += added
    added.clear()
    tasks.removeAll { task -> runCatching { task.tick() }.getOrElse { error -> Mod.log.error("A Mo’Bosses task failed", error); true } }
  }

  fun stop() {
    tasks.forEach(Task::cancel)
    tasks.clear()
    added.clear()
  }
}
