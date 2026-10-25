package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Difficulty as GameDifficulty
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.BossSpec
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID

/**
 * The bosses in loaded chunks, as Paper's `Bosses`. A boss is an ordinary mob with Mo’Bosses' data on it: [make]
 * writes that data and scales the mob, [adopt] picks a boss up again when its chunk loads, and [forget] lets go of
 * everything but the entity when its chunk unloads or it dies. The server thread only.
 */
class Bosses(private val mod: MoBosses) {
  private val live = HashMap<UUID, Boss>()

  val all: Collection<Boss> get() = live.values

  fun get(entity: Entity): Boss? = live[entity.uuid]

  fun get(uuid: UUID): Boss? = live[uuid]

  fun count(level: ServerLevel) = live.values.count { it.entity.level() == level }

  fun near(level: ServerLevel, at: Vec3, radius: Double) = live.values.count { it.entity.level() == level && it.entity.position().distanceToSqr(at) <= radius * radius }

  /** The modifier a new boss takes from the game's difficulty; null on peaceful, where no mob turns into a boss on its own. */
  fun difficulty(level: ServerLevel): Double? {
    val difficulty = when (level.difficulty) {
      GameDifficulty.PEACEFUL -> Difficulty.PEACEFUL
      GameDifficulty.EASY -> Difficulty.EASY
      GameDifficulty.NORMAL -> Difficulty.NORMAL
      GameDifficulty.HARD -> Difficulty.HARD
    }
    return Difficulty.modifier(mod.catalog.settings.difficulty, difficulty, level.levelData.isHardcore)
  }

  /** Scales [entity] into a boss and marks it as one, so it stays a boss after a restart. */
  fun make(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID? = null): Boss {
    val lvl = level.coerceAtLeast(1)
    val settings = mod.catalog.settings
    // A boss an admin spawns on peaceful plays as on hard, for as long as the game lets it stay.
    val graded = (entity.level() as? ServerLevel)?.let(::difficulty) ?: Difficulty.UNGRADED
    BossFactory.apply(entity, type, clazz, lvl, summoner, settings.formula, graded, settings.difficulty.weights)
    val boss = register(entity, type, clazz, lvl, summoner)
    boss.seen = mod.now()
    mod.progression.count(type.key, clazz.key, killed = false)
    mod.log.debug { "${type.key} became a level $lvl ${clazz.key} boss at ${entity.level().dimension().identifier()} ${entity.blockX} ${entity.blockY} ${entity.blockZ}" }
    return boss
  }

  /** Takes up a boss whose chunk just loaded, or one from before a restart. Null for any other entity. */
  fun adopt(entity: Entity): Boss? {
    if (entity !is LivingEntity || entity.isDeadOrDying) return null
    live[entity.uuid]?.let { return it }
    val data = Data(entity)
    val typeKey = data.string(Keys.TYPE) ?: return null
    val catalog = mod.catalog
    val type = catalog.types.values.firstOrNull { it.key == typeKey }
      // The type left bosses.yml since this boss spawned: it stays a boss, with plain effects.
      ?: Catalog.BossType(entity.type, BossSpec(), null, null, null, emptyList(), emptyList())
    val clazz = data.string(Keys.CLASS)?.let(catalog.classes::byKey) ?: catalog.classes.all.first()
    val level = data.int(Keys.LEVEL) ?: 1
    val summoner = data.string(Keys.SUMMONER)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    return register(entity, type, clazz, level, summoner)
  }

  /** Lets go of a boss: its bar, glow, aura, and labels. The entity and its data stay. */
  fun forget(boss: Boss) {
    if (live.remove(boss.uuid) == null) return
    mod.bars.remove(boss)
    mod.antiCheese.forget(boss)
    mod.visuals.off(boss)
    mod.nameTags.clear(boss.entity)
  }

  /** Removes a boss from the world without loot or experience, as `/mobosses killall` does. */
  fun remove(boss: Boss) {
    forget(boss)
    boss.entity.discard()
  }

  /** Lets go of every boss, for shutdown; the entities stay in the world. */
  fun forgetAll() = live.values.toList().forEach(::forget)

  /**
   * The game's own name for the boss, in the default language: death messages use it, and it keeps the boss from
   * despawning. With per-player labels on, it stays hidden under them.
   */
  fun vanillaName(boss: Boss) {
    val code = mod.languages.default
    mod.languages.render(code, Message.BOSS_TAG, *mod.texts.boss(code, boss))?.let { boss.entity.customName = mod.texts.vanilla(it) }
    boss.entity.isCustomNameVisible = !mod.catalog.settings.nameTags.perPlayer
  }

  private fun register(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID?): Boss {
    val boss = Boss(entity, type, clazz, level, summoner, mod::now)
    live[entity.uuid] = boss
    mod.minions.forget(entity)
    return boss
  }
}
