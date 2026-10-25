package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A player's own way of taking a hit, which skips the mob's ([HurtMixin]): who a boss hurt, for “untouched”. */
@Mixin(Player.class)
abstract class PlayerHurtMixin {
  @Unique private float mobosses$before;

  @Inject(method = "actuallyHurt", at = @At("HEAD"))
  private void mobosses$before(ServerLevel level, DamageSource source, float damage, CallbackInfo callback) {
    mobosses$before = ((Player) (Object) this).getHealth();
  }

  @Inject(method = "actuallyHurt", at = @At("TAIL"))
  private void mobosses$after(ServerLevel level, DamageSource source, float damage, CallbackInfo callback) {
    Player self = (Player) (Object) this;
    Hooks.INSTANCE.hurt(self, source, Math.max(0f, mobosses$before - self.getHealth()));
  }
}
