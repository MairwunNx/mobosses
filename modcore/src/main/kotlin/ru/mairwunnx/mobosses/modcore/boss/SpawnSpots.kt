package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.boss.SpawnPlan.Spot
import ru.mairwunnx.mobosses.modcore.ability.Targets

/** Where in a chunk the menu spawns a boss, as Paper's `SpawnSpots`: a floor near the admin's height with room above, nothing that hurts. */
object SpawnSpots {
  /** Blocks of room a boss gets over its feet: enough for an enderman or a ravager. */
  private const val ROOM = 3
  private val HARMFUL = setOf(
    Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.POINTED_DRIPSTONE,
    Blocks.SWEET_BERRY_BUSH, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.POWDER_SNOW, Blocks.WITHER_ROSE,
  )

  fun inChunk(level: ServerLevel, chunkX: Int, chunkZ: Int, y: Int): Vec3? {
    val spots = ArrayList<Spot>()
    for (dx in 0..15) for (dz in 0..15) {
      val x = chunkX * 16 + dx
      val z = chunkZ * 16 + dz
      val floor = Targets.ground(level, x + 0.5, y.toDouble(), z + 0.5) ?: continue
      if (level.getBlockState(floor).block in HARMFUL) continue
      val feet = floor.above()
      val roomy = (0 until ROOM).all { feet.above(it).let { pos -> Targets.isOpen(level, pos) && level.getBlockState(pos).block !in HARMFUL } }
      if (roomy) spots += Spot(x, feet.y, z)
    }
    val best = SpawnPlan.safest(spots, y, chunkX * 16 + 8.0, chunkZ * 16 + 8.0) ?: return null
    return Vec3(best.x + 0.5, best.y.toDouble(), best.z + 0.5)
  }
}
