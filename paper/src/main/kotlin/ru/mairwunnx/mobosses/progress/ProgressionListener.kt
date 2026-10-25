package ru.mairwunnx.mobosses.progress

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

/** Paper's logins and quits for [Progression]: progress loads during login, off the server thread. */
class ProgressionListener(private val progression: Progression) : Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED) progression.login(event.uniqueId, event.name)
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onQuit(event: PlayerQuitEvent) = progression.quit(event.player.uniqueId)
}
