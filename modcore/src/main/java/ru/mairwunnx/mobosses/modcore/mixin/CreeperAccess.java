package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A creeper boss's blast grows with its level, and a type may come charged: Bukkit's `setExplosionRadius` and `setPowered`. */
@Mixin(Creeper.class)
public interface CreeperAccess {
  @Accessor("explosionRadius") int mobosses$radius();
  @Accessor("explosionRadius") void mobosses$radius(int radius);

  @Accessor("DATA_IS_POWERED")
  static EntityDataAccessor<Boolean> mobosses$powered() {
    throw new AssertionError();
  }
}
