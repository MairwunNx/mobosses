package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.persistence.PersistentDataType
import org.bukkit.entity.ThrownPotion
import org.bukkit.entity.Trident
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import ru.mairwunnx.mobosses.ability.Arrows
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/** The blaze fires a burst of small fireballs at the player it fights; they burn players, not blocks. */
object FireballBarrage : Signature("fireball_barrage", Message.FIREBALL_BARRAGE) {
  override fun cast(cast: Cast): Boolean {
    val target = cast.foe(24.0) ?: return false
    val count = (3 + cast.level / 25).coerceAtMost(7)
    repeat(count) { i ->
      cast.later(i * 4L) {
        if (!cast.canHit(target) || target.world != cast.at.world) return@later
        cast.boss.entity.launchProjectile(SmallFireball::class.java, aim(cast, target, 0.08)) {
          it.setIsIncendiary(false)
          it.isPersistent = false
          it.persistentDataContainer.set(cast.plugin.keys.arrow, PersistentDataType.BOOLEAN, true)
        }
        cast.sound(Sound.ENTITY_BLAZE_SHOOT)
      }
    }
    return true
  }
}
