package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A temporary block a player breaks is gone without drops, so shears don't farm a boss's cobwebs. */
@Mixin(ServerPlayerGameMode.class)
abstract class BreakMixin {
  @Shadow protected ServerLevel level;

  @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
  private void mobosses$destroy(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
    if (!Hooks.INSTANCE.broke(level, pos)) return;
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    callback.setReturnValue(true);
  }
}
