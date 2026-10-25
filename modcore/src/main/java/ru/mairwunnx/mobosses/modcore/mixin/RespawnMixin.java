package ru.mairwunnx.mobosses.modcore.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.data.Data;

/**
 * A player who respawns is a new entity: Mo’Bosses' data (rewards' and spawns' cooldowns, the survival bonus, pity)
 * goes along, as Bukkit keeps a player's data through death. What death takes, the survival bonus, it takes itself.
 */
@Mixin(ServerPlayer.class)
abstract class RespawnMixin {
  @Inject(method = "restoreFrom", at = @At("TAIL"))
  private void mobosses$restore(ServerPlayer old, boolean restoreAll, CallbackInfo callback) {
    new Data(old).copyTo((ServerPlayer) (Object) this);
  }
}
