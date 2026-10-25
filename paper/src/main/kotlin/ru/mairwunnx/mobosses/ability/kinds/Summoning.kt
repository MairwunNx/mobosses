package ru.mairwunnx.mobosses.ability.kinds

import org.bukkit.Sound
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.ability.Ability
import ru.mairwunnx.mobosses.ability.Cast
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import kotlin.random.Random

/**
 * Minions from `minions.yml` at the boss's feet, topping up to `summoning_max_alive`; some may roll
 * into weaker bosses. Those mini bosses summon nothing themselves unless `summoning_by_mini_bosses`
 * is on, so a fight doesn't snowball.
 */
object Summoning : Ability {
  override val key = "summoning"
  override val message = Message.SUMMONING
  override fun enabled(settings: AbilitySettings) = settings.summoningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.summoning
  override fun cooldown(settings: AbilitySettings) = settings.summoningCooldown to settings.summoningCooldownBias

  private val BABIES = setOf(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.PIGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.HOGLIN)

  override fun usable(cast: Cast) =
    cast.plugin.catalog.minions.containsKey(cast.boss.entity.type) && (cast.boss.summoner == null || cast.settings.summoningByMiniBosses) && room(cast) > 0

  /** Minions that rolled into bosses count too. */
  private fun room(cast: Cast) =
    cast.settings.summoningMaxAlive - cast.plugin.minions.alive(cast.boss.uuid) - cast.plugin.bosses.all.count { it.summoner == cast.boss.uuid && it.entity.isValid }

  override fun cast(cast: Cast): Boolean {
    val minion = cast.plugin.catalog.minions[cast.boss.entity.type] ?: return false
    val count = minOf(Rolls.minions(cast.plugin.catalog.settings.formula.minions, cast.level, Random), room(cast))
    repeat(count) {
      val entity = cast.at.world.spawn(cast.at, minion.entity.entityClass!!, SpawnReason.CUSTOM) { spawned ->
        spawned.persistentDataContainer.set(cast.plugin.keys.summoner, PersistentDataType.STRING, cast.boss.uuid.toString())
        spawned.persistentDataContainer.set(cast.plugin.keys.minionOf, PersistentDataType.STRING, cast.boss.type.key)
      }
      if (entity is Ageable && entity.type in BABIES) entity.setBaby()
      // Without per-player labels, the game's own name shows, in the default language, as in 1.x.
      if (!cast.plugin.catalog.settings.nameTags.perPlayer) cast.plugin.texts.minionName(cast.plugin.languages.default, minion)?.let {
        entity.customName(it)
        entity.isCustomNameVisible = true
      }
    }
    cast.boss.type.summonSound?.let { cast.sound(it) }
    cast.sound(Sound.ENTITY_EVOKER_CAST_SPELL)
    return count > 0
  }
}
