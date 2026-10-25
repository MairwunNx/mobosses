package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A player's death, which skips the mob's ([DeathMixin]): Bukkit's `PlayerDeathEvent`, for the death penalty and the survival bonus. */
@Mixin(ServerPlayer.class)
abstract class PlayerDeathMixin {
  @Inject(method = "die", at = @At("HEAD"))
  private void mobosses$died(DamageSource source, CallbackInfo callback) {
    Hooks.INSTANCE.died((ServerPlayer) (Object) this, source);
  }
}
