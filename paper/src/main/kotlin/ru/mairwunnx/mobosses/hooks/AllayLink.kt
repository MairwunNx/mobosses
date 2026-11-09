package ru.mairwunnx.mobosses.hooks

import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * Mo’Bosses in Allay, the AI helper, when it runs: tools that tell the model which bosses and classes
 * this server has, the asker's hunt, and the bosses around them, and a few lines on how Mo’Bosses
 * works. [AllayBosses] speaks Allay's expansion API 2. This class holds no type of it, so it loads
 * without Allay.
 */
class AllayLink(private val plugin: MoBossesPlugin) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when Allay is installed; nothing happens without it. */
  fun connect() {
    if (plugin.server.pluginManager.getPlugin(ALLAY) == null) return
    try {
      AllayBosses.plugin = plugin
      AllayBosses.register()
      connected = true
      plugin.log.info("Allay found: it can tell players about bosses, classes, and their hunt")
    } catch (error: ExceptionInInitializerError) {
      plugin.log.warn("Allay didn't take Mo’Bosses' tools: ${error.cause?.message ?: error}")
    } catch (error: LinkageError) {
      // Allay before 6.33.0 has no expansion API 2.
      plugin.log.warn("Allay is too old for Mo’Bosses' tools: it needs 6.33 or newer")
    } catch (error: Exception) {
      plugin.log.warn("Allay didn't take Mo’Bosses' tools: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { AllayBosses.unregister() }
  }

  private companion object {
    const val ALLAY = "Allay"
  }
}
