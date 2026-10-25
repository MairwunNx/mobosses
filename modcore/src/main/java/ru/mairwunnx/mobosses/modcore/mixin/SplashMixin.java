package ru.mairwunnx.mobosses.modcore.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** An ability's potion splashes only the boss's foes: Bukkit's `PotionSplashEvent` with everyone else at no intensity. */
@Mixin(ThrownSplashPotion.class)
abstract class SplashMixin {
  @ModifyExpressionValue(
    method = "onHitAsPotion",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
  )
  private List<LivingEntity> mobosses$splashed(List<LivingEntity> entities) {
    return Hooks.INSTANCE.splashed((ThrownSplashPotion) (Object) this, entities);
  }
}
