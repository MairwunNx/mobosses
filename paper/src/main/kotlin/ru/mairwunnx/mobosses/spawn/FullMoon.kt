package ru.mairwunnx.mobosses.spawn

import io.papermc.paper.world.MoonPhase
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message

/**
 * Nights of the hunt: on a full-moon night in a world with a sky, bosses come more often, and rarer
 * classes more often still. Other phases leave everything as it is. Players hear about it as it rises.
 */
class FullMoon(private val plugin: MoBossesPlugin) {
  /** The day each world last announced, so each full moon is announced once. */
  private val announced = HashMap<String, Long>()

  fun isUp(world: World): Boolean {
    val cfg = plugin.catalog.settings.fullMoon
    return cfg.enabled && world.environment == World.Environment.NORMAL && world.moonPhase == MoonPhase.FULL_MOON && world.time in NIGHT
  }

  /** What the spawn chance is multiplied by in [world] now. */
  fun chance(world: World) = if (isUp(world)) plugin.catalog.settings.fullMoon.chance.coerceAtLeast(1.0) else 1.0

  /** A class's weight under the moon: Common's stays, the rarest's grows by `rarer_classes`, the ones between by their share. */
  fun weight(world: World, clazz: BossClass): Double {
    if (!isUp(world)) return clazz.chance
    return Hunt.moonWeight(plugin.catalog.settings.fullMoon, clazz.chance, plugin.catalog.classes.rank(clazz.key), plugin.catalog.classes.all.size)
  }

  /** Every minute: worlds where a full moon just rose tell their players, and WorldAPI's clients. */
  fun tick() {
    Bukkit.getWorlds().filter(::isUp).forEach { world ->
      val day = world.fullTime / 24000
      if (announced[world.name] == day) return@forEach
      announced[world.name] = day
      plugin.worldApi.fullMoon(world.name)
      if (!plugin.catalog.settings.fullMoon.announce) return@forEach
      plugin.texts.sendAll(world.players, Message.FULL_MOON) { emptyArray() }
      world.players.forEach { it.playSound(it.location, Sound.AMBIENT_CAVE, 0.8f, 0.6f) }
    }
  }

  private companion object {
    /** When the moon is up: from dusk to dawn. */
    val NIGHT = 13000L..23000L
  }
}
