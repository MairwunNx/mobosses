package ru.mairwunnx.mobosses.boss

import org.bukkit.entity.Hoglin
import org.bukkit.entity.PiglinAbstract
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Zombie
import org.bukkit.entity.ZombieVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTransformEvent
import org.bukkit.event.entity.EntityTransformEvent.TransformReason
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * A boss stays the mob it is: a zombie that drowns, a skeleton in powder snow, a piglin in the overworld, a cured
 * zombie villager would come out a plain mob, and the boss would be gone mid-fight. Its conversion stops and starts
 * over, as if it never began. A slime's split on death is the one change that goes on.
 */
class Conversions(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onTransform(event: EntityTransformEvent) {
    if (event.transformReason == TransformReason.SPLIT || plugin.bosses.get(event.entity) == null) return
    event.isCancelled = true
    when (val entity = event.entity) {
      is ZombieVillager -> entity.setConversionTime(-1)
      is Zombie -> entity.stopDrowning()
      is Skeleton -> entity.setConversionTime(-1)
      is PiglinAbstract -> entity.setConversionTime(-1)
      is Hoglin -> entity.setConversionTime(-1)
    }
  }
}
