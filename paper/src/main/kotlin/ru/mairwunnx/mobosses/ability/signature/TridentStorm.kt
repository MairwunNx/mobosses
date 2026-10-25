package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.entity.ThrownPotion
import org.bukkit.entity.Trident
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Arrows
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/** The drowned hurls tridents at the player it fights, none of which anyone can pick up. */
object TridentStorm : Signature("trident_storm", Message.TRIDENT_STORM) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(28.0) ?: return false
    val count = (2 + cast.level / 30).coerceAtMost(5)
    val damage = scaled(cast, 6.0, 0.06, 12.0)
    repeat(count) { i ->
      cast.later(i * 5L) {
        if (!cast.canHit(target) || target.world != cast.at.world) return@later
        cast.boss.entity.launchProjectile(Trident::class.java, aim(cast, target, 0.06).multiply(2.0)) {
          it.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
          it.damage = damage
          it.isPersistent = false
          it.persistentDataContainer.set(cast.plugin.keys.arrow, PersistentDataType.BOOLEAN, true)
          cast.later(100) { if (it.isValid) it.remove() }
        }
        cast.sound(Sound.ITEM_TRIDENT_THROW)
      }
    }
    return true
  }
}
