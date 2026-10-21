package ru.mairwunnx.mobosses.ability

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.NamespacedKey
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.boss.Codec
import ru.mairwunnx.mobosses.protect.Guard
import java.util.UUID
import kotlin.math.floor

/**
 * Who abilities may hit, and where on the ground they land: players, and the pets and golems that
 * hit the boss lately, which it hits back.
 */
object Targets {
  /** How long a pet or golem stays fair game after its last hit on a boss. */
  const val AGGRESSOR_MILLIS = 15_000L
  /** Players in survival or adventure, alive, whom the protection plugins let mobs hurt where they stand. */
  fun canHit(player: Player, guard: Guard) =
    player.isValid && !player.isDead && (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE) && guard.canHurt(player)

  fun players(at: Location, radius: Double, guard: Guard): List<Player> =
    at.world.getNearbyPlayers(at, radius, radius, radius) { canHit(it, guard) }.toList()

  /** Pets and golems near [at] that hit [boss] within [AGGRESSOR_MILLIS]. */
  fun aggressors(at: Location, radius: Double, boss: UUID, key: NamespacedKey): List<LivingEntity> =
    at.world.getNearbyLivingEntities(at, radius, radius, radius) { it !is Player && isAggressor(it, boss, key) }.toList()

  fun isAggressor(entity: Entity, boss: UUID, key: NamespacedKey, now: Long = System.currentTimeMillis()) =
    entity.isValid && !entity.isDead && (Codec.decode(entity.persistentDataContainer.get(key, PersistentDataType.STRING))[boss.toString()] ?: 0.0) > now

  /** Marks [entity] as having hit [boss] just now, dropping marks that ran out. */
  fun markAggressor(entity: Entity, boss: UUID, key: NamespacedKey, now: Long = System.currentTimeMillis()) {
    val marks = Codec.decode(entity.persistentDataContainer.get(key, PersistentDataType.STRING)).filterValues { it > now } + (boss.toString() to (now + AGGRESSOR_MILLIS).toDouble())
    entity.persistentDataContainer.set(key, PersistentDataType.STRING, Codec.encode(marks))
  }

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

  /** How many blocks of open air are over [at], up to [max]: under a roof, arrows and meteors start below it. */
  fun headroom(at: Location, max: Double): Double {
    val start = at.blockY + 2
    val top = (at.y + max).toInt()
    for (y in start..top) if (at.world.getBlockAt(at.blockX, y, at.blockZ).type.isSolid) return (y - at.y - 1).coerceAtLeast(0.0)
    return max
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
