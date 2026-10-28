package ru.mairwunnx.mobosses.modcore.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.mairwunnx.mobosses.modcore.Hooks;

/** What a mob's loot table drops, Looting's included: a dying boss's comes multiplied. Its gear drops elsewhere, as it is. */
@Mixin(LivingEntity.class)
abstract class LootDropMixin {
  @ModifyVariable(
    method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
    at = @At("HEAD"),
    argsOnly = true
  )
  private Consumer<ItemStack> mobosses$drops(Consumer<ItemStack> drop) {
    return Hooks.INSTANCE.drops((LivingEntity) (Object) this, drop);
  }
}
