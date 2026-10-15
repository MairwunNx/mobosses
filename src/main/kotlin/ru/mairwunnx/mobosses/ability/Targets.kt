package ru.mairwunnx.mobosses.ability

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.protect.Guard
import kotlin.math.floor

/** Who abilities may hit, and where on the ground they land. */
object Targets {
  /** Players in survival or adventure, alive, whom the protection plugins let mobs hurt where they stand. */
  fun canHit(player: Player, guard: Guard) =
    player.isValid && !player.isDead && (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE) && guard.canHurt(player)

  fun players(at: Location, radius: Double, guard: Guard): List<Player> =
    at.world.getNearbyPlayers(at, radius, radius, radius) { canHit(it, guard) }.toList()

  fun nearest(at: Location, radius: Double, guard: Guard): Player? =
    players(at, radius, guard).minByOrNull { it.location.distanceSquared(at) }

  /**
   * The floor near [y]: the highest block with a solid block under it and room above, within
   * [window] blocks up or down. 1.x took the highest block of the column, which put lightning and
   * cobwebs on the surface, or on the Nether's roof, while the fight went on in a cave.
   */
  fun ground(world: World, x: Double, y: Double, z: Double, window: Int = 8): Block? {
    val bx = floor(x).toInt()
    val bz = floor(z).toInt()
    val start = floor(y).toInt()
    val min = (start - window).coerceAtLeast(world.minHeight + 1)
    val max = (start + window).coerceAtMost(world.maxHeight - 2)
    // Down from the fight first, then up, as 1.x's fire ring searched.
    for (by in (start.coerceIn(min, max) downTo min) + (start.coerceIn(min, max) + 1..max)) {
      val below = world.getBlockAt(bx, by - 1, bz)
      if (below.type.isSolid && isOpen(world.getBlockAt(bx, by, bz))) return below
    }
    return null
  }

  /** Air or a plant an ability may replace; never water, lava, or anything waterlogged. */
  fun isOpen(block: Block): Boolean {
    val type = block.type
    if (type.isAir) return true
    if (block.isLiquid || (block.blockData as? Waterlogged)?.isWaterlogged == true) return false
    if (type == Material.SEAGRASS || type == Material.TALL_SEAGRASS || type == Material.KELP || type == Material.KELP_PLANT) return false
    return Tag.REPLACEABLE.isTagged(type) || Tag.SMALL_FLOWERS.isTagged(type)
  }
}
