package ru.mairwunnx.mobosses.modcore.ability.signature

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Cast

/** The drowned hurls tridents at the player it fights, none of which anyone can pick up. */
object TridentStorm : Signature("trident_storm", Message.TRIDENT_STORM) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(28.0) ?: return false
    val count = (2 + cast.level / 30).coerceAtMost(5)
    val damage = scaled(cast, 6.0, 0.06, 12.0)
    repeat(count) { i ->
      cast.later(i * 5L) {
        if (!cast.canHit(target) || target.level() != cast.world) return@later
        val trident = ThrownTrident(cast.world, cast.entity, ItemStack(Items.TRIDENT))
        trident.pickup = AbstractArrow.Pickup.DISALLOWED
        trident.setBaseDamage(damage)
        launch(cast, trident, aim(cast, target, 0.06).scale(2.0), 100)
        cast.sound(SoundEvents.TRIDENT_THROW.value())
      }
    }
    return true
  }
}
