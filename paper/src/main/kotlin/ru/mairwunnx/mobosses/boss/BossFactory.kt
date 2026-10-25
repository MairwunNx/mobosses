package ru.mairwunnx.mobosses.boss

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Creeper
import org.bukkit.entity.Hoglin
import org.bukkit.entity.PiglinAbstract
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.DifficultySettings
import ru.mairwunnx.mobosses.config.Settings
import kotlin.math.roundToInt
import java.util.UUID

/**
 * Turns a mob into a boss: its data on the entity, then its stats and gear. A boss is never weaker
 * than the mob it was: every stat is at least the mob's own, whatever the classes and curves say,
 * and gear only gets better.
 */
class BossFactory(private val data: Keys) {
  fun apply(entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID?, formula: Settings.Formulas, difficulty: Double, weights: DifficultySettings.Weights) {
    entity.persistentDataContainer.apply {
      set(data.type, PersistentDataType.STRING, type.key)
      set(data.clazz, PersistentDataType.STRING, clazz.key)
      set(data.level, PersistentDataType.INTEGER, level)
      summoner?.let { set(data.summoner, PersistentDataType.STRING, it.toString()) }
      set(data.difficulty, PersistentDataType.DOUBLE, difficulty)
    }
    fun graded(weight: Double) = Difficulty.part(difficulty, weight)

    // From the type's own values: a minion that rolls into a boss may have been changed already.
    entity.getAttribute(Attribute.MAX_HEALTH)?.let {
      it.baseValue = (Stats.health(formula.health, it.defaultValue, clazz.health, level) * graded(weights.health)).coerceAtMost(MAX_HEALTH).coerceAtLeast(it.defaultValue)
      entity.health = it.value
    }
    entity.getAttribute(Attribute.ATTACK_DAMAGE)?.let { it.baseValue = (Stats.damage(formula.damage, it.defaultValue, clazz.damage, level) * graded(weights.damage)).coerceAtLeast(it.defaultValue) }
    if (entity is Creeper) {
      if (type.spec.isPowered == true) entity.isPowered = true
      // The game doubles a charged creeper's radius when it blows, so it gets half of the whole.
      val charge = if (entity.isPowered) 2 else 1
      entity.explosionRadius = (Stats.explosionRadius(formula.explosion, CREEPER_RADIUS * charge, level) / charge.toDouble()).roundToInt()
    }
    // A piglin or hoglin in the overworld turns into a zombified one after a few seconds, and the boss would go with it.
    if (entity is PiglinAbstract) entity.isImmuneToZombification = true
    if (entity is Hoglin) entity.isImmuneToZombification = true
    if (!Equipment.equip(entity, type, level)) {
      entity.getAttribute(Attribute.ARMOR)?.let { it.baseValue = (Stats.armor(formula.armor, it.defaultValue, clazz.defense, level) * graded(weights.armor)).coerceAtMost(formula.armor.capPoints.toDouble()).coerceAtLeast(it.defaultValue) }
      entity.getAttribute(Attribute.ARMOR_TOUGHNESS)?.let { it.baseValue = (Stats.armor(formula.toughness, it.defaultValue, clazz.toughness, level) * graded(weights.armor)).coerceAtMost(formula.toughness.capPoints.toDouble()).coerceAtLeast(it.defaultValue) }
    }
  }

  private companion object {
    /** A creeper's blast radius in the game. */
    const val CREEPER_RADIUS = 3
    /** The game's limit on max health, which [Stats.health] keeps to before the difficulty. */
    const val MAX_HEALTH = 1024.0
  }
}
