package ru.mairwunnx.mobosses.boss

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * The bosses in loaded chunks. A boss is an ordinary mob with Mo’Bosses' data on it: [make] writes
 * that data and scales the mob, [adopt] picks a boss up again when its chunk loads, and [forget]
 * lets go of everything but the entity when its chunk unloads or it dies. The server thread only.
 */
class Bosses(private val plugin: MoBossesPlugin) {
  private val live = HashMap<UUID, Boss>()
  private val factory = BossFactory(plugin.keys)

  val all: Collection<Boss> get() = live.values

  fun get(entity: Entity): Boss? = live[entity.uniqueId]

  fun get(uuid: UUID): Boss? = live[uuid]

  fun count(world: World) = live.values.count { it.entity.world == world }

  fun near(at: Location, radius: Double) = live.values.count { it.entity.world == at.world && it.entity.location.distanceSquared(at) <= radius * radius }

  /**
   * The modifier a new boss in [world] takes from the world's difficulty; null on peaceful, where no mob
   * turns into a boss on its own.
   */
  fun difficulty(world: World): Double? {
    val difficulty = when (world.difficulty) {
      org.bukkit.Difficulty.PEACEFUL -> Difficulty.PEACEFUL
      org.bukkit.Difficulty.EASY -> Difficulty.EASY
      org.bukkit.Difficulty.NORMAL -> Difficulty.NORMAL
      org.bukkit.Difficulty.HARD -> Difficulty.HARD
    }
    return Difficulty.modifier(plugin.catalog.settings.difficulty, difficulty, world.isHardcore)
  }

  /** Scales [entity] into a boss and marks it as one, so it stays a boss after a restart. */
  fun make(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID? = null): Boss {
    val lvl = level.coerceAtLeast(1)
    val settings = plugin.catalog.settings
    // A boss an admin spawns on peaceful plays as on hard, for as long as the game lets it stay.
    factory.apply(entity, type, clazz, lvl, summoner, settings.formula, difficulty(entity.world) ?: Difficulty.UNGRADED, settings.difficulty.weights)
    val boss = register(entity, type, clazz, lvl, summoner)
    boss.seen = System.currentTimeMillis()
    plugin.progression.count(type.key, clazz.key, killed = false)
    plugin.log.debug { "${type.key} became a level $lvl ${clazz.key} boss at ${entity.world.name} ${entity.location.blockX} ${entity.location.blockY} ${entity.location.blockZ}" }
    return boss
  }

  /** Takes up a boss whose chunk just loaded, or one from before a restart. Null for any other entity. */
  fun adopt(entity: Entity): Boss? {
    if (entity !is LivingEntity || entity.isDead) return null
    live[entity.uniqueId]?.let { return it }
    val data = entity.persistentDataContainer
    val typeKey = data.get(plugin.keys.type, PersistentDataType.STRING) ?: return null
    val catalog = plugin.catalog
    val type = catalog.types.values.firstOrNull { it.key == typeKey }
      // The type left bosses.yml since this boss spawned: it stays a boss, with plain effects.
      ?: Catalog.BossType(entity.type, BossSpec(), null, null, null, emptyList(), emptyList())
    val clazz = data.get(plugin.keys.clazz, PersistentDataType.STRING)?.let(catalog.classes::byKey) ?: catalog.classes.all.first()
    val level = data.get(plugin.keys.level, PersistentDataType.INTEGER) ?: 1
    val summoner = data.get(plugin.keys.summoner, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    return register(entity, type, clazz, level, summoner)
  }

  /** Lets go of a boss: its bar, glow, aura, and labels. The entity and its data stay. */
  fun forget(boss: Boss) {
    if (live.remove(boss.uuid) == null) return
    plugin.bars.remove(boss)
    plugin.visuals.off(boss)
    plugin.nameTags.clear(boss.entity)
  }

  /** Removes a boss from the world without loot or experience, as `/mobosses killall` does. */
  fun remove(boss: Boss) {
    forget(boss)
    boss.entity.remove()
  }

  /** Lets go of every boss, for shutdown; the entities stay in the world. */
  fun forgetAll() = live.values.toList().forEach(::forget)

  /**
   * The game's own name for the boss, in the default language: death messages use it, and it keeps
   * the boss from despawning. With per-player labels on, it stays hidden under them.
   */
  fun vanillaName(boss: Boss) {
    val code = plugin.languages.default
    plugin.languages.render(code, Message.BOSS_TAG, *plugin.texts.boss(code, boss))?.let { boss.entity.customName(it) }
    boss.entity.isCustomNameVisible = !plugin.catalog.settings.nameTags.perPlayer
  }

  private fun register(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID?): Boss {
    val boss = Boss(entity, type, clazz, level, summoner, plugin.keys)
    live[entity.uniqueId] = boss
    plugin.minions.forget(entity)
    return boss
  }
}
