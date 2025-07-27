package ru.mairwunnx.mobosses.framework

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Thread.currentThread
import kotlin.coroutines.CoroutineContext

class BukkitAsyncDispatcher(private val plugin: JavaPlugin) : CoroutineDispatcher() {
  override fun isDispatchNeeded(context: CoroutineContext) = currentThread().name.startsWith("CraftScheduler").not()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val job = context[Job]
    if (job != null && !job.isActive) return

    if (isDispatchNeeded(context)) {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, block)
    } else {
      block.run()
    }
  }
}
