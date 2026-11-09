package ru.mairwunnx.mobosses.modcore.hooks

import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * Mo’Bosses in Allay, the AI helper, when it runs as a mod, as on Paper: tools that tell the model which bosses,
 * classes, abilities, and loot this server has, the asker's hunt, the best hunters, and the bosses around them, one
 * that spawns bosses for operators, and the named drops for its item search. [AllayBosses] speaks Allay's expansion API 2.
 * This class holds no type of it, so it loads without Allay.
 */
class AllayLink(private val mod: MoBosses) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when Allay is installed; nothing happens without it. */
  fun connect() {
    if (!mod.loader.loaded(ALLAY)) return
    try {
      AllayBosses.mod = mod
      AllayBosses.register()
      connected = true
      mod.log.info("Allay found: it can tell players about bosses, their abilities and loot, and their hunt")
    } catch (error: ExceptionInInitializerError) {
      mod.log.warn("Allay didn't take Mo’Bosses' tools: ${error.cause?.message ?: error}")
    } catch (error: LinkageError) {
      // Allay before 6.33.0 has no expansion API 2.
      mod.log.warn("Allay is too old for Mo’Bosses' tools: it needs 6.33 or newer")
    } catch (error: Exception) {
      mod.log.warn("Allay didn't take Mo’Bosses' tools: ${error.message}")
    }
  }

  /** Hands Allay the expansion again after a reload, so its items follow the loot table and languages just read. */
  fun refresh() {
    if (!connected) return
    runCatching { AllayBosses.register() }.onFailure { mod.log.warn("Allay didn't take Mo’Bosses' tools again: ${it.message}") }
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
