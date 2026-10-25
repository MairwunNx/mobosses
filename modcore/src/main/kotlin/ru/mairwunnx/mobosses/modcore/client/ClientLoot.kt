package ru.mairwunnx.mobosses.modcore.client

import net.minecraft.client.Minecraft
import ru.mairwunnx.mobosses.modcore.net.Payloads

/**
 * The server's loot table in a player's game, for the recipe viewers: what the server sent last, and who wants to hear
 * when it changes. Empty on a server without Mo’Bosses as a mod, and between worlds.
 */
object ClientLoot {
  @Volatile var pages: List<Payloads.LootPage> = emptyList()
    private set

  private val listeners = ArrayList<(List<Payloads.LootPage>) -> Unit>()

  fun listen(listener: (List<Payloads.LootPage>) -> Unit) {
    listeners += listener
  }

  fun received(payload: Payloads.LootPages) {
    Minecraft.getInstance().execute {
      pages = payload.pages
      listeners.forEach { it(pages) }
    }
  }

  /** The player left the world: the next one sends its own. */
  fun left() {
    pages = emptyList()
  }
}
