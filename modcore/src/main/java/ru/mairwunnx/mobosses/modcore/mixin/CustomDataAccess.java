package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Custom data's tag without the copy the game makes on every read: Mo’Bosses reads its data on every tick. */
@Mixin(CustomData.class)
public interface CustomDataAccess {
  @Accessor("tag") CompoundTag mobosses$tag();
}
