package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** Entities coming and going with their chunks: Paper's `EntityAddToWorldEvent` and `EntityRemoveFromWorldEvent`. */
@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
abstract class TrackingMixin {
  @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
  private void mobosses$start(Entity entity, CallbackInfo callback) {
    Hooks.INSTANCE.loaded(entity);
  }

  @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
  private void mobosses$end(Entity entity, CallbackInfo callback) {
    Hooks.INSTANCE.unloaded(entity);
  }
}
