package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A boss's fangs bite only its foes: Bukkit's damage event by `EvokerFangs`, cancelled for anyone else. */
@Mixin(EvokerFangs.class)
abstract class FangsMixin {
  @Inject(method = "dealDamageTo", at = @At("HEAD"), cancellable = true)
  private void mobosses$bite(LivingEntity entity, CallbackInfo callback) {
    if (!Hooks.INSTANCE.mayBite(((EvokerFangs) (Object) this).getOwner(), entity)) callback.cancel();
  }
}
