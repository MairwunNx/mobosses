package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/**
 * A mob's death, before its drops: Bukkit's `EntityDeathEvent`. A boss's experience goes out by share, so the game's
 * own orb stays home; and a boss never goes through a portal.
 */
@Mixin(LivingEntity.class)
abstract class DeathMixin {
  @Shadow protected boolean dead;

  @Inject(method = "die", at = @At("HEAD"))
  private void mobosses$died(DamageSource source, CallbackInfo callback) {
    LivingEntity self = (LivingEntity) (Object) this;
    // The game ignores a second death the same way.
    if (!self.isRemoved() && !dead) Hooks.INSTANCE.died(self, source);
  }

  @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
  private void mobosses$experience(ServerLevel level, @Nullable Entity killer, CallbackInfo callback) {
    if (!Hooks.INSTANCE.dropsExperience((LivingEntity) (Object) this)) callback.cancel();
  }

  @Inject(method = "canUsePortal", at = @At("RETURN"), cancellable = true)
  private void mobosses$portal(boolean ignorePassenger, CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValueZ() && !Hooks.INSTANCE.mayTravel((LivingEntity) (Object) this)) callback.setReturnValue(false);
  }
}
