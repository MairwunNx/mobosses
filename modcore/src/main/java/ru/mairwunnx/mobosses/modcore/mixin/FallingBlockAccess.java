package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A meteor's block, as Bukkit's `FallingBlock.setBlockData`: the game sets it only when a real block starts to fall. */
@Mixin(FallingBlockEntity.class)
public interface FallingBlockAccess {
  @Accessor("blockState") void mobosses$block(BlockState state);
}
