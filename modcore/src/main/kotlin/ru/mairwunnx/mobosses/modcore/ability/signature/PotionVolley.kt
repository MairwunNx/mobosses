package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast
import java.util.Optional

/** The witch throws splash potions at the players it fights near it: poison and slowness, weakness and harming as it grows. */
object PotionVolley : Signature("potion_volley", Message.POTION_VOLLEY) {
  override fun cast(cast: Cast): Boolean {
    val targets = cast.foes(16.0).take(4).ifEmpty { return false }
    val seconds = ticks(cast, 4.0, 0.06, 10.0)
    targets.forEach { target ->
      val effects = buildList {
        add(MobEffectInstance(MobEffects.POISON, seconds, if (cast.level >= 40) 1 else 0))
        add(MobEffectInstance(MobEffects.SLOWNESS, seconds, 0))
        if (cast.level >= 25) add(MobEffectInstance(MobEffects.WEAKNESS, seconds, 0))
        if (cast.level >= 50) add(MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 0))
      }
      val potion = ItemStack(Items.SPLASH_POTION).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty())) }
      launch(cast, ThrownSplashPotion(cast.world, cast.entity, potion), aim(cast, target, 0.05).scale(0.75).add(0.0, 0.2, 0.0))
    }
    cast.sound(SoundEvents.WITCH_THROW)
    return true
  }
}
