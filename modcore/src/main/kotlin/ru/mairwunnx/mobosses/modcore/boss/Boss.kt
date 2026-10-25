package ru.mairwunnx.mobosses.modcore.boss

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.entity.LivingEntity
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.boss.Codec
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.modcore.Scheduler
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID

/**
 * A boss in a loaded chunk. Everything that matters for a fight, its type, class, level, who hit it, its cooldowns,
 * and whether it's enraged, lives on the entity ([Data]), so a relog, a restart, or a chunk unload loses none of it.
 * Only the effects (the aura) are for this session. Times are the server's clock ([ru.mairwunnx.mobosses.modcore.MoBosses.now]).
 */
class Boss(
  val entity: LivingEntity,
  val type: Catalog.BossType,
  val clazz: BossClass,
  val level: Int,
  /** The boss that summoned this one, for minions that rolled into bosses. */
  val summoner: UUID?,
  private val clock: () -> Long,
) {
  val uuid: UUID = entity.uuid

  private val data = Data(entity)

  val color: TextColor get() = colorOf(clazz)

  /** Damage each player dealt, their pets' included, capped by the health the boss had left at each hit. */
  val damage: Map<UUID, Double> get() = players(Keys.DAMAGE)

  /** The part of [damage] that each player's pets dealt. */
  val petDamage: Map<UUID, Double> get() = players(Keys.PET_DAMAGE)

  fun addDamage(player: UUID, amount: Double, byPet: Boolean = false) {
    add(Keys.DAMAGE, player, amount)
    if (byPet) add(Keys.PET_DAMAGE, player, amount)
    write(Keys.HIT_AT, read(Keys.HIT_AT) + (player.toString() to clock().toDouble()))
  }

  /** The players who hit it, themselves or through a pet, within the last [millis]. */
  fun hitWithin(millis: Long): Set<UUID> {
    val since = clock() - millis
    return players(Keys.HIT_AT).filterValues { it >= since }.keys
  }

  /** Who set it alight, and whether through a pet: the fire's damage counts as theirs; null when no player did. */
  var igniter: Pair<UUID, Boolean>?
    get() = data.string(Keys.IGNITER)?.split('|')
      ?.let { parts -> runCatching { UUID.fromString(parts[0]) }.getOrNull()?.let { it to (parts.getOrNull(1) == "pet") } }
    set(value) = if (value == null) data.remove(Keys.IGNITER) else data.set(Keys.IGNITER, value.first.toString() + if (value.second) "|pet" else "")

  /** When a player was last near. */
  var seen: Long?
    get() = data.long(Keys.SEEN)
    set(value) = if (value == null) data.remove(Keys.SEEN) else data.set(Keys.SEEN, value)

  /** A boss from `/mobosses spawn`, which an admin placed and which doesn't despawn. */
  var pinned: Boolean
    get() = data.boolean(Keys.PINNED) == true
    set(value) = data.set(Keys.PINNED, value)

  /** When an ability, or [SHARED] for any, may go off again. */
  fun readyAt(ability: String): Long = read(Keys.COOLDOWNS)[ability]?.toLong() ?: 0L

  /** Records an ability going off: when it and the shared cooldown end, each rolled once. */
  fun used(ability: String, readyAt: Long, sharedReadyAt: Long) =
    write(Keys.COOLDOWNS, read(Keys.COOLDOWNS) + mapOf(ability to readyAt.toDouble(), SHARED to sharedReadyAt.toDouble()))

  var enraged: Boolean
    get() = data.boolean(Keys.ENRAGED) == true
    set(value) = data.set(Keys.ENRAGED, value)

  /** The max health it started with, before groups grew it; null until a group first does. */
  var baseHealth: Double?
    get() = data.double(Keys.BASE_HEALTH)
    set(value) = if (value == null) data.remove(Keys.BASE_HEALTH) else data.set(Keys.BASE_HEALTH, value)

  /** The health groups added to it, which the shares split along with the rest. */
  var addedHealth: Double
    get() = data.double(Keys.ADDED_HEALTH) ?: 0.0
    set(value) = data.set(Keys.ADDED_HEALTH, value)

  /** How many players its health counts. */
  var fighters: Int
    get() = data.int(Keys.FIGHTERS) ?: 1
    set(value) = data.set(Keys.FIGHTERS, value)

  var fromSpawner: Boolean
    get() = data.boolean(Keys.FROM_SPAWNER) == true
    set(value) = data.set(Keys.FROM_SPAWNER, value)

  /** Past its second phase: it casts faster for the rest of the fight. */
  var phaseTwo: Boolean
    get() = data.boolean(Keys.PHASE_TWO) == true
    set(value) = data.set(Keys.PHASE_TWO, value)

  /** The difficulty's modifier it was made with, which a later change of difficulty leaves alone. */
  val difficulty: Double get() = data.double(Keys.DIFFICULTY) ?: Difficulty.UNGRADED

  /** What a part of it is multiplied by, from its [difficulty] and that part's weight in `difficulty.weights`. */
  fun graded(weight: Double): Double = Difficulty.part(difficulty, weight)

  var aura: Scheduler.Task? = null

  private fun players(key: String) = read(key).mapNotNull { (k, v) -> runCatching { UUID.fromString(k) }.getOrNull()?.let { it to v } }.toMap()

  private fun add(key: String, player: UUID, amount: Double) =
    write(key, players(key).toMutableMap().apply { merge(player, amount, Double::plus) }.mapKeys { it.key.toString() })

  private fun read(key: String) = Codec.decode(data.string(key))

  private fun write(key: String, values: Map<String, Number>) = data.set(key, Codec.encode(values))

  companion object {
    /** The shared cooldown's key among the abilities'. */
    const val SHARED = "*"

    fun colorOf(clazz: BossClass): TextColor =
      clazz.color.trim().let { TextColor.fromHexString(it) ?: NamedTextColor.NAMES.value(it.lowercase()) } ?: NamedTextColor.WHITE
  }
}
