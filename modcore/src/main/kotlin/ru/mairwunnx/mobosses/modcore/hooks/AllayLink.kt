package ru.mairwunnx.mobosses.modcore.hooks

import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Mo’Bosses in Allay, the AI helper, when it runs as a mod, as on Paper: tools that tell the model which bosses and
 * classes this server has, the asker's hunt, and the bosses around them. [AllayBosses] speaks Allay's expansion API;
 * this class holds no type of it, so it loads without Allay.
 */
class AllayLink(private val mod: MoBosses) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when Allay is installed; nothing happens without it. */
  fun connect() {
    if (!mod.loader.loaded(ALLAY)) return
    try {
      AllayBosses.register(mod)
      connected = true
      mod.log.info("Allay found: it can tell players about bosses, classes, and their hunt")
    } catch (error: LinkageError) {
      mod.log.warn("Allay is too old for Mo’Bosses' tools; it needs 6.17.0 or newer")
    } catch (error: Exception) {
      mod.log.warn("Allay didn't take Mo’Bosses' tools: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { AllayBosses.unregister() }
  }

  private companion object {
    const val ALLAY = "allay"
  }
}
