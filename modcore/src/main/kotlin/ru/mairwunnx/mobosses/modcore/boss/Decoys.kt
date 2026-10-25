package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID

/**
 * A boss's copies from its doubles ability, as on Paper: ordinary mobs of its type with one health, its gear, its
 * name, and its glow, so players must find the real one. They aren't saved, drop only what their mob would and 1
 * experience, and leave after a while.
 */
class Decoys(private val mod: MoBosses) {
  private val live = HashMap<UUID, LivingEntity>()

  val all: Collection<LivingEntity> get() = live.values

  fun add(decoy: LivingEntity) {
    live[decoy.uuid] = decoy
  }

  fun isDecoy(entity: Entity) = Data(entity).has(Keys.DECOY)

  /** The boss a copy copies, while it's loaded. */
  fun bossOf(entity: Entity): Boss? = Data(entity).string(Keys.DECOY)?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let(mod.bosses::get)

  fun forget(entity: Entity) {
    if (live.remove(entity.uuid) == null) return
    mod.nameTags.clear(entity)
    mod.server.scoreboard.removePlayerFromTeam(entity.uuid.toString())
  }

  fun remove(entity: LivingEntity) {
    forget(entity)
    entity.discard()
  }

  /** The copies of [boss], when it dies. */
  fun removeOf(boss: UUID) = live.values.filter { Data(it).string(Keys.DECOY) == boss.toString() }.forEach(::remove)

  /** Every copy, for shutdown. */
  fun removeAll() = live.values.toList().forEach(::remove)
}
