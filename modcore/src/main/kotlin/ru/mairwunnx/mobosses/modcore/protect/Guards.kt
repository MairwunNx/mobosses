package ru.mairwunnx.mobosses.modcore.protect

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/** Every adapter at once: an action goes ahead only when all of them allow it. */
class Guards(val adapters: List<Guard>) : Guard {
  override val name = adapters.joinToString { it.name }.ifEmpty { "none" }

  override fun canSpawnBoss(level: ServerLevel, pos: BlockPos) = adapters.all { it.canSpawnBoss(level, pos) }
  override fun canChangeBlock(level: ServerLevel, pos: BlockPos) = adapters.all { it.canChangeBlock(level, pos) }
  override fun canHurt(player: ServerPlayer) = adapters.all { it.canHurt(player) }
}
