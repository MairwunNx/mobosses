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

/** The witch throws splash potions at the players it fights near it: poison and slowness, weakness and harming as it grows. */
object PotionVolley : Signature("potion_volley", Message.POTION_VOLLEY) {
  override fun cast(cast: Cast): Boolean {
    val targets = cast.foes(16.0).take(4).ifEmpty { return false }
    val seconds = ticks(cast, 4.0, 0.06, 10.0)
    targets.forEach { target ->
      val effects = buildList {
        add(PotionEffect(PotionEffectType.POISON, seconds, if (cast.level >= 40) 1 else 0))
        add(PotionEffect(PotionEffectType.SLOWNESS, seconds, 0))
        if (cast.level >= 25) add(PotionEffect(PotionEffectType.WEAKNESS, seconds, 0))
        if (cast.level >= 50) add(PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0))
      }
      val potion = ItemStack(Material.SPLASH_POTION).apply { editMeta(PotionMeta::class.java) { meta -> effects.forEach { meta.addCustomEffect(it, true) } } }
      cast.boss.entity.launchProjectile(ThrownPotion::class.java, aim(cast, target, 0.05).multiply(0.75).add(Vector(0.0, 0.2, 0.0))) {
        it.item = potion
        it.isPersistent = false
        it.persistentDataContainer.set(cast.plugin.keys.arrow, PersistentDataType.BOOLEAN, true)
      }
    }
    cast.sound(Sound.ENTITY_WITCH_THROW)
    return true
  }
}
