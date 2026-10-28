package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** The dying dragon drops its experience over its last seconds; a boss dragon's went out by share, so it drops none. */
@Mixin(EnderDragon.class)
abstract class DragonMixin {
  @Redirect(
    method = "tickDeath",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
    require = 2
  )
  private void mobosses$experience(ServerLevel level, Vec3 at, int amount) {
    if (Hooks.INSTANCE.dragonExperience((EnderDragon) (Object) this)) ExperienceOrb.award(level, at, amount);
  }
}
