package ru.mairwunnx.mobosses.modcore.protect

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/** What a protection mod lets bosses do, as on Paper. One adapter per mod, each loaded only when that mod runs. With none, everything is allowed. */
interface Guard {
  val name: String

  /** Whether a boss may spawn here naturally. */
  fun canSpawnBoss(level: ServerLevel, pos: BlockPos): Boolean = true

  /** Whether an ability may break, place, or ignite the block here. */
  fun canChangeBlock(level: ServerLevel, pos: BlockPos): Boolean = true

  /** Whether an ability may hurt this player here. */
  fun canHurt(player: ServerPlayer): Boolean = true
}
