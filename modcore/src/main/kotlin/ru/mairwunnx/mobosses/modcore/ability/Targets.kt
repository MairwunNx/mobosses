package ru.mairwunnx.mobosses.modcore.ability

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.boss.Codec
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.modcore.protect.Guard
import java.util.UUID
import kotlin.math.floor

/** Who abilities may hit and where the ground is, as Paper's `Targets`. */
object Targets {
  /** How long a pet or golem stays fair game after its last hit on a boss. */
  const val AGGRESSOR_MILLIS = 15_000L

  /** Players in survival or adventure, alive, whom the protection mods let mobs hurt where they stand. */
  fun canHit(player: ServerPlayer, guard: Guard) =
    player.isAlive && !player.isDeadOrDying && !player.hasDisconnected() && (player.gameMode() == GameType.SURVIVAL || player.gameMode() == GameType.ADVENTURE) && guard.canHurt(player)

  fun players(level: ServerLevel, at: Vec3, radius: Double, guard: Guard): List<ServerPlayer> =
    level.players().filter { it.position().distanceToSqr(at) <= radius * radius && canHit(it, guard) }

  /** Pets and golems near [at] that hit [boss] within [AGGRESSOR_MILLIS]. */
  fun aggressors(level: ServerLevel, at: Vec3, radius: Double, boss: UUID, now: Long): List<LivingEntity> =
    level.getEntitiesOfClass(LivingEntity::class.java, AABB.ofSize(at, radius * 2, radius * 2, radius * 2)) {
      it !is Player && it.position().distanceToSqr(at) <= radius * radius && isAggressor(it, boss, now)
    }

  fun isAggressor(entity: Entity, boss: UUID, now: Long) =
    entity.isAlive && (Codec.decode(Data(entity).string(Keys.AGGRESSOR))[boss.toString()] ?: 0.0) > now

  /** Marks [entity] as having hit [boss] just now, dropping marks that ran out. */
  fun markAggressor(entity: Entity, boss: UUID, now: Long) {
    val data = Data(entity)
    val marks = Codec.decode(data.string(Keys.AGGRESSOR)).filterValues { it > now } + (boss.toString() to (now + AGGRESSOR_MILLIS).toDouble())
    data[Keys.AGGRESSOR] = Codec.encode(marks)
  }

  /** Throws [entity] along [velocity], as Bukkit's `setVelocity`: a player's game hears of it at once. */
  fun push(entity: Entity, velocity: Vec3) {
    entity.deltaMovement = velocity
    entity.needsSync = true
    (entity as? ServerPlayer)?.connection?.send(ClientboundSetEntityMotionPacket(entity))
  }

  /**
   * The floor near [y]: the block under the highest open spot with solid ground, within [window] blocks up or down.
   * 1.x took the highest block of the column, which put lightning and cobwebs on the surface, or on the Nether's
   * roof, while the fight went on in a cave.
   */
  fun ground(level: ServerLevel, x: Double, y: Double, z: Double, window: Int = 8): BlockPos? {
    val bx = floor(x).toInt()
    val bz = floor(z).toInt()
    val start = floor(y).toInt()
    val min = (start - window).coerceAtLeast(level.minY + 1)
    val max = (start + window).coerceAtMost(level.maxY - 1)
    // Down from the fight first, then up, as 1.x's fire ring searched.
    for (by in (start.coerceIn(min, max) downTo min) + (start.coerceIn(min, max) + 1..max)) {
      val below = BlockPos(bx, by - 1, bz)
      if (solid(level, below) && isOpen(level, below.above())) return below
    }
    return null
  }

  /** Water with open air over it near [y] in the column at [x], [z]: where a fire ring dances on a lake. */
  fun water(level: ServerLevel, x: Double, y: Double, z: Double, window: Int = 8): BlockPos? {
    val bx = floor(x).toInt()
    val bz = floor(z).toInt()
    val start = floor(y).toInt()
    for (by in (start + window).coerceAtMost(level.maxY - 1) downTo (start - window).coerceAtLeast(level.minY)) {
      val pos = BlockPos(bx, by, bz)
      if (level.getFluidState(pos).`is`(FluidTags.WATER) && level.getBlockState(pos.above()).isAir) return pos
    }
    return null
  }

  /** How many blocks of open air are over [at], up to [max]: under a roof, arrows and meteors start below it. */
  fun headroom(level: ServerLevel, at: Vec3, max: Double): Double {
    val x = floor(at.x).toInt()
    val z = floor(at.z).toInt()
    val start = floor(at.y).toInt() + 2
    val top = (at.y + max).toInt()
    for (y in start..top) if (solid(level, BlockPos(x, y, z))) return (y - at.y - 1).coerceAtLeast(0.0)
    return max
  }

  /** What Bukkit calls a solid block: one that blocks movement. */
  fun solid(level: ServerLevel, pos: BlockPos) = level.getBlockState(pos).isSolid

  /** Air or a plant an ability may replace; never water, lava, or anything waterlogged. */
  fun isOpen(level: ServerLevel, pos: BlockPos): Boolean {
    val state = level.getBlockState(pos)
    if (state.isAir) return true
    if (!state.fluidState.isEmpty) return false
    if (state.`is`(Blocks.SEAGRASS) || state.`is`(Blocks.TALL_SEAGRASS) || state.`is`(Blocks.KELP) || state.`is`(Blocks.KELP_PLANT)) return false
    return state.`is`(BlockTags.REPLACEABLE) || state.`is`(BlockTags.SMALL_FLOWERS)
  }
}
