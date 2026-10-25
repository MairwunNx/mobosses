package ru.mairwunnx.mobosses.boss

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.MoBossesPlugin
import java.util.UUID

/**
 * Minions in loaded chunks, by the boss that summoned them, so a boss keeps no more alive than
 * `summoning_max_alive` and each minion gets a name in its viewer's language. Minions are ordinary
 * mobs: they despawn as vanilla ones do and carry only their summoner on them.
 */
class Minions(private val plugin: MoBossesPlugin) {
  private val loaded = HashMap<UUID, LivingEntity>()

  val all: Collection<LivingEntity> get() = loaded.values

  fun adopt(entity: Entity) {
    if (entity !is LivingEntity || plugin.bosses.get(entity) != null) return
    if (entity.persistentDataContainer.has(plugin.keys.summoner, PersistentDataType.STRING)) loaded[entity.uniqueId] = entity
  }

  fun forget(entity: Entity) {
    if (loaded.remove(entity.uniqueId) != null) plugin.nameTags.clear(entity)
  }

  fun alive(summoner: UUID): Int = loaded.values.count { it.isValid && summonerOf(it) == summoner }

  fun summonerOf(entity: Entity): UUID? =
    entity.persistentDataContainer.get(plugin.keys.summoner, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

  /** The boss type that summoned it, like `zombie`, for its name. */
  fun typeOf(entity: Entity): String? = entity.persistentDataContainer.get(plugin.keys.minionOf, PersistentDataType.STRING)
}
