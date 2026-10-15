package ru.mairwunnx.mobosses.ability.signature

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.persistence.PersistentDataType
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
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

/** The breeze fires wind charges at players near it: knockback, no broken blocks. */
object WindBurst : Signature("wind_burst", Message.WIND_BURST) {
  override fun cast(cast: Cast): Boolean {
    val targets = cast.players(cast.at, 20.0).take(3).ifEmpty { return false }
    targets.forEach { target ->
      cast.boss.entity.launchProjectile(org.bukkit.entity.BreezeWindCharge::class.java, aim(cast, target, 0.03).multiply(1.4)) {
        it.isPersistent = false
        it.persistentDataContainer.set(cast.plugin.keys.arrow, PersistentDataType.BOOLEAN, true)
      }
    }
    cast.at.world.spawnParticle(Particle.GUST, cast.at.clone().add(0.0, 1.0, 0.0), 3)
    cast.sound(Sound.ENTITY_BREEZE_SHOOT)
    return true
  }
}
