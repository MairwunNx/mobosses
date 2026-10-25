package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A boss never gets into a boat or a minecart: Bukkit's `EntityMountEvent`. */
@Mixin(Entity.class)
abstract class RideMixin {
  @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("HEAD"), cancellable = true)
  private void mobosses$ride(Entity vehicle, boolean force, boolean sendEventAndTriggers, CallbackInfoReturnable<Boolean> callback) {
    if (!Hooks.INSTANCE.mayRide((Entity) (Object) this, vehicle)) callback.setReturnValue(false);
  }
}
