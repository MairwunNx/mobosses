package ru.mairwunnx.mobosses.modcore.spawn;

import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/** Why a mob spawned, kept from its `finalizeSpawn` until it enters the world, where Bukkit's spawn event would fire. */
public interface SpawnReasonHolder {
  @Nullable EntitySpawnReason mobosses$reason();
}
