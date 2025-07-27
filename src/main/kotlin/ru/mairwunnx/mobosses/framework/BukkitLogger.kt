package ru.mairwunnx.mobosses.framework

import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mobosses.PluginUnit
import java.util.logging.Level

class BukkitLogger(private val plugin: PluginUnit) {
  internal inline fun debug(msg: () -> String) {
    if (plugin.isConfigurationDefined) {
      if (plugin.configuration[GeneralConfigurationModel::class.java].debug) log(Level.INFO, "ℹ️ ${msg()}")
    } else {
      log(Level.INFO, "ℹ️ ${msg()}")
    }
  }
  internal inline fun info(msg: () -> String) = log(Level.INFO, msg())
  internal inline fun warn(msg: () -> String) = log(Level.WARNING, "⚠️ ${msg()}")
  internal inline fun error(msg: () -> String, t: Throwable? = null) = log(Level.SEVERE, "❌ ${msg()}", t)

  private fun log(level: Level, raw: String, t: Throwable? = null) = if (t == null) plugin.getLogger().log(level, raw) else plugin.getLogger().log(level, raw, t)
}