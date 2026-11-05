package ru.mairwunnx.mobosses.boss

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import ru.mairwunnx.mobosses.ability.Targets
import ru.mairwunnx.mobosses.boss.SpawnPlan.Spot

/** Where in a chunk the menu spawns a boss: a floor near the admin's height with room above, nothing that hurts. */
object SpawnSpots {
  /** Blocks of room a boss gets over its feet: enough for an enderman or a ravager. */
  private const val ROOM = 3
  private val HARMFUL = setOf(
    Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.POINTED_DRIPSTONE,
    Material.SWEET_BERRY_BUSH, Material.FIRE, Material.SOUL_FIRE, Material.POWDER_SNOW, Material.WITHER_ROSE,
  )

  fun inChunk(world: World, chunkX: Int, chunkZ: Int, y: Int): Location? {
    val spots = ArrayList<Spot>()
    for (dx in 0..15) for (dz in 0..15) {
      val x = chunkX * 16 + dx
      val z = chunkZ * 16 + dz
      val floor = Targets.ground(world, x + 0.5, y.toDouble(), z + 0.5) ?: continue
      if (floor.type in HARMFUL) continue
      val feet = floor.y + 1
      val roomy = (0 until ROOM).all { world.getBlockAt(x, feet + it, z).let { block -> Targets.isOpen(block) && block.type !in HARMFUL } }
      if (roomy) spots += Spot(x, feet, z)
    }
    val best = SpawnPlan.safest(spots, y, chunkX * 16 + 8.0, chunkZ * 16 + 8.0) ?: return null
    return Location(world, best.x + 0.5, best.y.toDouble(), best.z + 0.5)
  }
}
