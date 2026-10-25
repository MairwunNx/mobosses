package ru.mairwunnx.mobosses.modcore.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.mobosses.modcore.achievement.Achievements;

/**
 * Mo’Bosses' advancements join the game's each time it builds its list, at the start and after `/reload`: Bukkit's
 * `loadAdvancements`. A data pack's advancement under the same id wins.
 */
@Mixin(ServerAdvancementManager.class)
abstract class AdvancementsMixin {
  @Shadow @Final @Mutable private Map<Identifier, AdvancementHolder> advancements;
  @Shadow @Final @Mutable private AdvancementTree tree;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void mobosses$add(HolderLookup.Provider registries, CallbackInfo callback) {
    Map<Identifier, AdvancementHolder> all = new HashMap<>();
    for (AdvancementHolder holder : Achievements.Companion.build(registries)) all.put(holder.id(), holder);
    if (all.isEmpty()) return;
    all.putAll(advancements);
    advancements = Map.copyOf(all);
    AdvancementTree next = new AdvancementTree();
    next.addAll(advancements.values());
    next.repositionNodes();
    tree = next;
  }
}
