package ru.mairwunnx.mobosses.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

class BukkitDispatcher(private val plugin: JavaPlugin) : CoroutineDispatcher() {
  override fun isDispatchNeeded(context: CoroutineContext) = !Bukkit.isPrimaryThread()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val job = context[Job]
    if (job != null && !job.isActive) return

    if (Bukkit.isPrimaryThread()) {
      block.run()
    } else {
      Bukkit.getScheduler().runTask(plugin, block)
    }
  }
}
