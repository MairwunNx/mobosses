package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** An obelisk takes only players' hits and never explodes: the game's crystal would blow up at the first one. */
@Mixin(EndCrystal.class)
abstract class CrystalMixin {
  @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
  private void mobosses$hurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> callback) {
    if (Hooks.INSTANCE.obeliskHurt((EndCrystal) (Object) this, source)) callback.setReturnValue(false);
  }
}
