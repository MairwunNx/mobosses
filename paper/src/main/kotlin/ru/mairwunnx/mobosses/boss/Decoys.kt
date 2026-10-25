package ru.mairwunnx.mobosses.boss

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID

/**
 * A boss's copies from its doubles ability: ordinary mobs of its type with one health, its gear, its
 * name, and its glow, so players must find the real one. They aren't saved, drop only what their mob
 * would and 1 experience, and leave after a while.
 */
class Decoys(private val plugin: MoBossesPlugin) {
  private val live = HashMap<UUID, LivingEntity>()

  val all: Collection<LivingEntity> get() = live.values

  fun add(decoy: LivingEntity) {
    live[decoy.uniqueId] = decoy
  }

  fun isDecoy(entity: Entity) = entity.persistentDataContainer.has(plugin.keys.decoy, PersistentDataType.STRING)

  /** The boss a copy copies, while it's loaded. */
  fun bossOf(entity: Entity): Boss? =
    entity.persistentDataContainer.get(plugin.keys.decoy, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.let(plugin.bosses::get)

  fun forget(entity: Entity) {
    if (live.remove(entity.uniqueId) == null) return
    plugin.nameTags.clear(entity)
    Bukkit.getScoreboardManager().mainScoreboard.teams.forEach { it.removeEntry(entity.uniqueId.toString()) }
  }

  fun remove(entity: LivingEntity) {
    forget(entity)
    entity.remove()
  }

  /** The copies of [boss], when it dies. */
  fun removeOf(boss: UUID) = live.values.filter { it.persistentDataContainer.get(plugin.keys.decoy, PersistentDataType.STRING) == boss.toString() }.forEach(::remove)

  /** Every copy, for shutdown. */
  fun removeAll() = live.values.toList().forEach(::remove)
}
