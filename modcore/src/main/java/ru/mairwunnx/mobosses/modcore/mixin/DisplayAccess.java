package ru.mairwunnx.mobosses.modcore.mixin;

import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** What Bukkit's `Display` sets, for the name labels only their viewers' games know about. */
@Mixin(Display.class)
public interface DisplayAccess {
  @Invoker("setBillboardConstraints") void mobosses$billboard(Display.BillboardConstraints constraints);
  @Invoker("setTransformation") void mobosses$transformation(Transformation transformation);

  @Mixin(Display.TextDisplay.class)
  interface Text {
    @Invoker("setText") void mobosses$text(Component text);
  }
}
