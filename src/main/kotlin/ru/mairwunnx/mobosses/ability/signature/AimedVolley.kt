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

/** Archers loose a fan of arrows at the nearest player; the arrows hit players only. */
object AimedVolley : Signature("aimed_volley", Message.AIMED_VOLLEY) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.nearest(32.0) ?: return false
    val damage = scaled(cast, 3.0, 0.06, 9.0)
    val count = (5 + cast.level / 20).coerceAtMost(10)
    repeat(count) {
      Arrows.shoot(cast, cast.boss.entity.eyeLocation, aim(cast, target, 0.12).multiply(2.2), damage, true, 6, null)
    }
    cast.sound(Sound.ITEM_CROSSBOW_SHOOT, 1f, 1.2f)
    return true
  }
}
