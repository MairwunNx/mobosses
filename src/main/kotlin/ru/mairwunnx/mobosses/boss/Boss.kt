package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.config.BossClass
import java.util.UUID

/**
 * A boss in a loaded chunk. Everything that matters for a fight, its type, class, level, who hit it,
 * its cooldowns, and whether it's enraged, lives on the entity, so a relog, a restart, or a chunk
 * unload loses none of it. Only the effects (the aura) are for this session.
 */
class Boss(
  val entity: LivingEntity,
  val type: Catalog.BossType,
  val clazz: BossClass,
  val level: Int,
  /** The boss that summoned this one, for minions that rolled into bosses. */
  val summoner: UUID?,
  private val keys: Keys,
) {
  val uuid: UUID = entity.uniqueId

  val color: TextColor get() = colorOf(clazz)

  /** Damage each player dealt, capped by the health the boss had left at each hit. */
  val damage: Map<UUID, Double> get() = read(keys.damage).mapNotNull { (k, v) -> runCatching { UUID.fromString(k) }.getOrNull()?.let { it to v } }.toMap()

  fun addDamage(player: UUID, amount: Double) {
    val next = damage.toMutableMap().apply { merge(player, amount, Double::plus) }
    write(keys.damage, next.mapKeys { it.key.toString() })
  }

  /** When an ability, or [SHARED] for any, may go off again, in epoch milliseconds. */
  fun readyAt(ability: String): Long = read(keys.cooldowns)[ability]?.toLong() ?: 0L

  /** Records an ability going off: when it and the shared cooldown end, each rolled once. */
  fun used(ability: String, readyAt: Long, sharedReadyAt: Long) =
    write(keys.cooldowns, read(keys.cooldowns) + mapOf(ability to readyAt.toDouble(), SHARED to sharedReadyAt.toDouble()))

  var enraged: Boolean
    get() = entity.persistentDataContainer.get(keys.enraged, PersistentDataType.BOOLEAN) == true
    set(value) = entity.persistentDataContainer.set(keys.enraged, PersistentDataType.BOOLEAN, value)

  var aura: BukkitTask? = null

  private fun read(key: NamespacedKey) = Codec.decode(entity.persistentDataContainer.get(key, PersistentDataType.STRING))

  private fun write(key: NamespacedKey, values: Map<String, Number>) =
    entity.persistentDataContainer.set(key, PersistentDataType.STRING, Codec.encode(values))

  companion object {
    /** The shared cooldown's key among the abilities'. */
    const val SHARED = "*"

    fun colorOf(clazz: BossClass): TextColor =
      clazz.color.trim().let { TextColor.fromHexString(it) ?: NamedTextColor.NAMES.value(it.lowercase()) } ?: NamedTextColor.WHITE
  }
}
