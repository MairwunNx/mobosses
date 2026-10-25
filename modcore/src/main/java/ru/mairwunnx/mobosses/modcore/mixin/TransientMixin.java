package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/**
 * What abilities leave in the world, arrows, meteors, obelisks, and a boss's copies, is never saved, as Bukkit's
 * `setPersistent(false)`: a restart never brings one back.
 */
@Mixin(Entity.class)
abstract class TransientMixin {
  @Inject(method = "shouldBeSaved", at = @At("RETURN"), cancellable = true)
  private void mobosses$saved(CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValueZ() && Hooks.INSTANCE.fleeting((Entity) (Object) this)) callback.setReturnValue(false);
  }
}
