package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.spawn.SpawnReasonHolder;

/** Remembers why a mob spawned: the game tells `finalizeSpawn`, and the mob enters the world just after. */
@Mixin(Mob.class)
abstract class SpawnReasonMixin implements SpawnReasonHolder {
  @Unique private @Nullable EntitySpawnReason mobosses$reason;

  @Inject(method = "finalizeSpawn", at = @At("HEAD"))
  private void mobosses$finalize(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> callback) {
    if (mobosses$reason == null) mobosses$reason = reason;
  }

  @Override
  public @Nullable EntitySpawnReason mobosses$reason() {
    return mobosses$reason;
  }
}
