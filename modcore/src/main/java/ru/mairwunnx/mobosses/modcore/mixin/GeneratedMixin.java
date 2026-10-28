package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A mob a structure places as the world generates, like a bastion's brutes: it never goes through `addFreshEntity` on a level. */
@Mixin(WorldGenRegion.class)
abstract class GeneratedMixin {
  @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
  private void mobosses$generated(Entity entity, CallbackInfoReturnable<Boolean> callback) {
    Hooks.INSTANCE.generated(entity);
  }
}
