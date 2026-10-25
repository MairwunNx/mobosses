package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.data.Data
import java.util.UUID

/**
 * Minions in loaded chunks, by the boss that summoned them, as on Paper: so a boss keeps no more alive than
 * `summoning_max_alive` and each minion gets a name in its viewer's language. Minions are ordinary mobs: they despawn
 * as vanilla ones do and carry only their summoner on them.
 */
class Minions(private val mod: MoBosses) {
  private val loaded = HashMap<UUID, LivingEntity>()

  val all: Collection<LivingEntity> get() = loaded.values

  fun adopt(entity: Entity) {
    if (entity !is LivingEntity || mod.bosses.get(entity) != null) return
    if (Data(entity).has(Keys.SUMMONER)) loaded[entity.uuid] = entity
  }

  fun forget(entity: Entity) {
    if (loaded.remove(entity.uuid) != null) mod.nameTags.clear(entity)
  }

  fun alive(summoner: UUID): Int = loaded.values.count { it.isAlive && summonerOf(it) == summoner }

  fun summonerOf(entity: Entity): UUID? = Data(entity).string(Keys.SUMMONER)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

  /** The boss type that summoned it, like `zombie`, for its name. */
  fun typeOf(entity: Entity): String? = Data(entity).string(Keys.MINION_OF)
}
