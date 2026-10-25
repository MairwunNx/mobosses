package ru.mairwunnx.mobosses.modcore.ability.kinds

import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.Mob
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Minions from `minions.yml` at the boss's feet, topping up to `summoning_max_alive`; some may roll into weaker
 * bosses. Those mini bosses summon nothing themselves unless `summoning_by_mini_bosses` is on, so a fight doesn't
 * snowball.
 */
object Summoning : Ability {
  override val key = "summoning"
  override val message = Message.SUMMONING
  override fun enabled(settings: AbilitySettings) = settings.summoningEnabled
  override fun threshold(thresholds: AbilityThresholds) = thresholds.summoning
  override fun cooldown(settings: AbilitySettings) = settings.summoningCooldown to settings.summoningCooldownBias

  private val BABIES = setOf(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.PIGLIN, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HOGLIN)

  override fun usable(cast: Cast) =
    cast.mod.catalog.minions.containsKey(cast.entity.type) && (cast.boss.summoner == null || cast.settings.summoningByMiniBosses) && room(cast) > 0

  /** Minions that rolled into bosses count too. */
  private fun room(cast: Cast) =
    cast.settings.summoningMaxAlive - cast.mod.minions.alive(cast.boss.uuid) - cast.mod.bosses.all.count { it.summoner == cast.boss.uuid && it.entity.isAlive }

  override fun cast(cast: Cast): Boolean {
    val mod = cast.mod
    val minion = mod.catalog.minions[cast.entity.type] ?: return false
    val settings = mod.catalog.settings
    val rolled = (Rolls.minions(settings.formula.minions, cast.level, Random) * cast.boss.graded(settings.difficulty.weights.minions)).roundToInt().coerceAtLeast(1)
    val count = minOf(rolled, room(cast))
    var spawned = 0
    repeat(count) {
      // No finalizeSpawn, so it enters the world as Paper's CUSTOM and rolls by its summoner.
      val entity = minion.entity.create(cast.world, EntitySpawnReason.MOB_SUMMONED) ?: return@repeat
      entity.snapTo(cast.at.x, cast.at.y, cast.at.z, cast.entity.yRot, 0f)
      Data(entity)[Keys.SUMMONER] = cast.boss.uuid.toString()
      Data(entity)[Keys.MINION_OF] = cast.boss.type.key
      if (entity is Mob && entity.type in BABIES) entity.setBaby(true)
      // Without per-player labels, the game's own name shows, in the default language, as in 1.x.
      if (!settings.nameTags.perPlayer) mod.texts.minionName(mod.languages.default, minion)?.let {
        entity.customName = mod.texts.vanilla(it)
        entity.isCustomNameVisible = true
      }
      if (cast.world.addFreshEntity(entity)) spawned++
    }
    cast.boss.type.summonSound?.let { World.sound(cast.world, cast.at, it) }
    cast.sound(SoundEvents.EVOKER_CAST_SPELL)
    return spawned > 0
  }
}
