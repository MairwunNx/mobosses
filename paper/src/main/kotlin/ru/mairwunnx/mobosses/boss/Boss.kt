package ru.mairwunnx.mobosses.boss

import ru.mairwunnx.mobosses.combat.Threat
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.combat.Shares
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

  /** Damage each player dealt, their pets' included, capped by the health the boss had left at each hit. */
  val damage: Map<UUID, Double> get() = players(keys.damage)

  /** The part of [damage] that each player's pets dealt. */
  val petDamage: Map<UUID, Double> get() = players(keys.petDamage)

  /** [hit] false for damage credited to them that they didn't deal by hand, which keeps them in the fight no longer. */
  fun addDamage(player: UUID, amount: Double, byPet: Boolean = false, hit: Boolean = true) {
    add(keys.damage, player, amount)
    if (byPet) add(keys.petDamage, player, amount)
    if (hit) write(keys.hitAt, read(keys.hitAt) + (player.toString() to System.currentTimeMillis().toDouble()))
  }

  /** Damage with no player behind it, split among the players who hit it lately ([Shares.credit]). */
  fun credit(amount: Double) {
    Shares.credit(amount, damage, petDamage, hitWithin(CREDIT_MILLIS)).forEach { (id, credit) ->
      if (credit.own > 0.0) addDamage(id, credit.own, hit = false)
      if (credit.pets > 0.0) addDamage(id, credit.pets, byPet = true, hit = false)
    }
  }

  /** The players who hit it, themselves or through a pet, within the last [millis]. */
  fun hitWithin(millis: Long): Set<UUID> {
    val since = System.currentTimeMillis() - millis
    return players(keys.hitAt).filterValues { it >= since }.keys
  }

  /** Who set it alight, and whether through a pet: the fire's damage counts as theirs; null when no player did. */
  var igniter: Pair<UUID, Boolean>?
    get() = entity.persistentDataContainer.get(keys.igniter, PersistentDataType.STRING)?.split('|')
      ?.let { parts -> runCatching { UUID.fromString(parts[0]) }.getOrNull()?.let { it to (parts.getOrNull(1) == "pet") } }
    set(value) = if (value == null) entity.persistentDataContainer.remove(keys.igniter)
      else entity.persistentDataContainer.set(keys.igniter, PersistentDataType.STRING, value.first.toString() + if (value.second) "|pet" else "")

  /** When a player was last near, in epoch milliseconds; null for bosses from before 2.3. */
  var seen: Long?
    get() = entity.persistentDataContainer.get(keys.seen, PersistentDataType.LONG)
    set(value) = if (value == null) entity.persistentDataContainer.remove(keys.seen) else entity.persistentDataContainer.set(keys.seen, PersistentDataType.LONG, value)

  /** A boss from `/mobosses spawn`, which an admin placed and which doesn't despawn. */
  var pinned: Boolean
    get() = entity.persistentDataContainer.get(keys.pinned, PersistentDataType.BOOLEAN) == true
    set(value) = entity.persistentDataContainer.set(keys.pinned, PersistentDataType.BOOLEAN, value)

  private fun players(key: NamespacedKey) = read(key).mapNotNull { (k, v) -> runCatching { UUID.fromString(k) }.getOrNull()?.let { it to v } }.toMap()

  private fun add(key: NamespacedKey, player: UUID, amount: Double) =
    write(key, players(key).toMutableMap().apply { merge(player, amount, Double::plus) }.mapKeys { it.key.toString() })

  /** When an ability, or [SHARED] for any, may go off again, in epoch milliseconds. */
  fun readyAt(ability: String): Long = read(keys.cooldowns)[ability]?.toLong() ?: 0L

  /** Records an ability going off: when it and the shared cooldown end, each rolled once. */
  fun used(ability: String, readyAt: Long, sharedReadyAt: Long) =
    write(keys.cooldowns, read(keys.cooldowns) + mapOf(ability to readyAt.toDouble(), SHARED to sharedReadyAt.toDouble()))

  var enraged: Boolean
    get() = entity.persistentDataContainer.get(keys.enraged, PersistentDataType.BOOLEAN) == true
    set(value) = entity.persistentDataContainer.set(keys.enraged, PersistentDataType.BOOLEAN, value)

  /** The max health it started with, before groups grew it; null until a group first does. */
  var baseHealth: Double?
    get() = entity.persistentDataContainer.get(keys.baseHealth, PersistentDataType.DOUBLE)
    set(value) = if (value == null) entity.persistentDataContainer.remove(keys.baseHealth) else entity.persistentDataContainer.set(keys.baseHealth, PersistentDataType.DOUBLE, value)

  /** The health groups added to it, which the shares split along with the rest. */
  var addedHealth: Double
    get() = entity.persistentDataContainer.get(keys.addedHealth, PersistentDataType.DOUBLE) ?: 0.0
    set(value) = entity.persistentDataContainer.set(keys.addedHealth, PersistentDataType.DOUBLE, value)

  /** How many players its health counts. */
  var fighters: Int
    get() = entity.persistentDataContainer.get(keys.fighters, PersistentDataType.INTEGER) ?: 1
    set(value) = entity.persistentDataContainer.set(keys.fighters, PersistentDataType.INTEGER, value)

  var fromSpawner: Boolean
    get() = entity.persistentDataContainer.get(keys.fromSpawner, PersistentDataType.BOOLEAN) == true
    set(value) = entity.persistentDataContainer.set(keys.fromSpawner, PersistentDataType.BOOLEAN, value)

  /** Past its second phase: it casts faster for the rest of the fight. */
  var phaseTwo: Boolean
    get() = entity.persistentDataContainer.get(keys.phaseTwo, PersistentDataType.BOOLEAN) == true
    set(value) = entity.persistentDataContainer.set(keys.phaseTwo, PersistentDataType.BOOLEAN, value)

  /** The difficulty's modifier it was made with, which a later change of difficulty leaves alone. */
  val difficulty: Double get() = entity.persistentDataContainer.get(keys.difficulty, PersistentDataType.DOUBLE) ?: Difficulty.UNGRADED

  /** What a part of it is multiplied by, from its [difficulty] and that part's weight in `difficulty.weights`. */
  fun graded(weight: Double): Double = Difficulty.part(difficulty, weight)

  var aura: BukkitTask? = null

  /** Whom it chases: each player's damage, fading. Only in memory; a restart starts it over. */
  val threat = Threat()

  private fun read(key: NamespacedKey) = Codec.decode(entity.persistentDataContainer.get(key, PersistentDataType.STRING))

  private fun write(key: NamespacedKey, values: Map<String, Number>) =
    entity.persistentDataContainer.set(key, PersistentDataType.STRING, Codec.encode(values))

  companion object {
    /** The shared cooldown's key among the abilities'. */
    const val SHARED = "*"

    /** How long after their last hit a player still gets the damage nobody dealt by hand. */
    const val CREDIT_MILLIS = 30_000L

    fun colorOf(clazz: BossClass): TextColor =
      clazz.color.trim().let { TextColor.fromHexString(it) ?: NamedTextColor.NAMES.value(it.lowercase()) } ?: NamedTextColor.WHITE
  }
}
