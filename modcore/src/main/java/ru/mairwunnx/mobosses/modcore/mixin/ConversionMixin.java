package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/**
 * A mob turning into another, Bukkit's `EntityTransformEvent`: a boss stays the mob it is, so a drowned zombie, a
 * frozen skeleton, or a zombified piglin never takes its place. A slime's split on death goes on.
 */
@Mixin(Mob.class)
abstract class ConversionMixin {
  @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At("HEAD"), cancellable = true)
  private void mobosses$convert(EntityType<?> type, ConversionParams params, EntitySpawnReason reason, ConversionParams.AfterConversion<?> after, CallbackInfoReturnable<Mob> callback) {
    if (params.type() == ConversionType.SINGLE && !Hooks.INSTANCE.mayConvert((Mob) (Object) this)) callback.setReturnValue(null);
  }
}
