package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** Bukkit's `EntityInsideBlockEvent` for cobwebs and fire: bosses and their minions pass through what abilities leave. */
@Mixin({WebBlock.class, BaseFireBlock.class})
abstract class InsideBlockMixin {
  @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
  private void mobosses$inside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise, CallbackInfo callback) {
    if (Hooks.INSTANCE.passes(level, pos, entity)) callback.cancel();
  }
}
