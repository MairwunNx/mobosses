package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** What an ability throws flies through everyone but the boss's foes: Bukkit's `ProjectileHitEvent`, cancelled. */
@Mixin(Projectile.class)
abstract class ProjectileHitMixin {
  @Inject(method = "canHitEntity", at = @At("RETURN"), cancellable = true)
  private void mobosses$canHit(Entity entity, CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValueZ() && !Hooks.INSTANCE.mayHit((Projectile) (Object) this, entity)) callback.setReturnValue(false);
  }
}
