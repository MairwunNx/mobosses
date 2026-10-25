package ru.mairwunnx.mobosses.modcore.spawn

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.level.MoonPhase
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.spawn.Hunt

/**
 * Nights of the hunt, as on Paper: on a full-moon night in a world with a sky, bosses come more often, and rarer
 * classes more often still. Other phases leave everything as it is. Players hear about it as it rises.
 */
class FullMoon(private val mod: MoBosses) {
  /** The day each world last announced, so each full moon is announced once. */
  private val announced = HashMap<String, Long>()

  /** Whether the moon over [at] is full and up; the game reads the moon's phase by place, so a world's spawn stands in for the world. */
  fun isUp(level: ServerLevel, at: Vec3 = Vec3.atCenterOf(level.respawnData.globalPos.pos())): Boolean {
    if (!mod.catalog.settings.fullMoon.enabled || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return false
    return level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, at) == MoonPhase.FULL_MOON && level.defaultClockTime % 24000L in NIGHT
  }

  /** What the spawn chance is multiplied by at [at] now. */
  fun chance(level: ServerLevel, at: Vec3) = if (isUp(level, at)) mod.catalog.settings.fullMoon.chance.coerceAtLeast(1.0) else 1.0

  fun weight(level: ServerLevel, at: Vec3, clazz: BossClass): Double =
    if (!isUp(level, at)) clazz.chance else Hunt.moonWeight(mod.catalog.settings.fullMoon, clazz.chance, mod.catalog.classes.rank(clazz.key), mod.catalog.classes.all.size)

  /** Every minute: worlds where a full moon just rose tell their players, and WorldAPI's clients. */
  fun tick() {
    mod.server.allLevels.filter(::isUp).forEach { level ->
      val name = level.dimension().identifier().toString()
      val day = level.defaultClockTime / 24000L
      if (announced[name] == day) return@forEach
      announced[name] = day
      mod.worldApi.fullMoon(name)
      if (!mod.catalog.settings.fullMoon.announce) return@forEach
      mod.texts.sendAll(level.players(), Message.FULL_MOON) { emptyArray() }
      level.players().forEach { World.sound(it, SoundEvents.AMBIENT_CAVE, volume = 0.8f, pitch = 0.6f) }
    }
  }

  private companion object {
    /** When the moon is up: from dusk to dawn. */
    val NIGHT = 13000L..23000L
  }
}
