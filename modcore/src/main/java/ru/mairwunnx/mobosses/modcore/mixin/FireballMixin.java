package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.mairwunnx.mobosses.modcore.boss.Keys;
import ru.mairwunnx.mobosses.modcore.data.Data;

/** An ability's small fireball burns players, not blocks: Bukkit's `setIsIncendiary(false)`. */
@Mixin(SmallFireball.class)
abstract class FireballMixin {
  @Redirect(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
  private boolean mobosses$fire(Level level, BlockPos pos, BlockState state) {
    if (new Data((SmallFireball) (Object) this).has(Keys.ARROW)) return false;
    return level.setBlockAndUpdate(pos, state);
  }
}
