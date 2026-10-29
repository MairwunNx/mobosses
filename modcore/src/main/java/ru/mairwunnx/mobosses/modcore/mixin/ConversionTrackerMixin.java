package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A boss zombie under water or a boss skeleton in powder snow never starts turning, so it doesn't shake for nothing. */
@Mixin(ConversionTracker.class)
abstract class ConversionTrackerMixin {
  @Shadow @Final private Mob mob;

  @Inject(method = "startConversion", at = @At("HEAD"), cancellable = true)
  private void mobosses$start(int time, CallbackInfo callback) {
    if (!Hooks.INSTANCE.mayConvert(mob)) callback.cancel();
  }
}
