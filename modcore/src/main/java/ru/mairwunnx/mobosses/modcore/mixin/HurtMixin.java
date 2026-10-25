package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.Hooks;

/**
 * Hits on mobs: Bukkit's `EntityDamageEvent` once the health is gone, before the game decides whether the mob dies,
 * so the killing blow counts too. What it took is what the health shows, after armor and absorption. A boss's own
 * shots grow before anything else reads them.
 */
@Mixin(LivingEntity.class)
abstract class HurtMixin {
  @Unique private float mobosses$before;

  @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
  private float mobosses$damage(float damage, ServerLevel level, DamageSource source) {
    return Hooks.INSTANCE.damage((LivingEntity) (Object) this, source, damage);
  }

  @Inject(method = "actuallyHurt", at = @At("HEAD"))
  private void mobosses$before(ServerLevel level, DamageSource source, float damage, CallbackInfo callback) {
    mobosses$before = ((LivingEntity) (Object) this).getHealth();
  }

  @Inject(method = "actuallyHurt", at = @At("TAIL"))
  private void mobosses$after(ServerLevel level, DamageSource source, float damage, CallbackInfo callback) {
    LivingEntity self = (LivingEntity) (Object) this;
    Hooks.INSTANCE.hurt(self, source, Math.max(0f, mobosses$before - self.getHealth()));
  }
}
