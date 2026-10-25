package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** A mob new to the world: Bukkit's `CreatureSpawnEvent`, after the game gave it its gear. */
@Mixin(ServerLevel.class)
abstract class LevelEntityMixin {
  @Inject(method = "addFreshEntity", at = @At("RETURN"))
  private void mobosses$added(Entity entity, CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValueZ()) Hooks.INSTANCE.spawned(entity);
  }
}
