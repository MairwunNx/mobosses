package ru.mairwunnx.mobosses.modcore.boss

import net.minecraft.core.Holder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.hoglin.Hoglin
import net.minecraft.world.entity.monster.piglin.AbstractPiglin
import ru.mairwunnx.mobosses.algebra.Difficulty
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.DifficultySettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.modcore.mixin.CreeperAccess
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Turns a mob into a boss: its data on the entity, then its stats and gear, as Paper's `BossFactory`. A boss is never
 * weaker than the mob it was: every stat is at least its type's own, whatever the classes, curves, and difficulty say,
 * and gear only gets better.
 */
object BossFactory {
  fun apply(
    entity: LivingEntity, type: Catalog.BossType, clazz: BossClass, level: Int, summoner: UUID?,
    formula: Settings.Formulas, difficulty: Double, weights: DifficultySettings.Weights,
  ) {
    Data(entity).apply {
      set(Keys.TYPE, type.key)
      set(Keys.CLASS, clazz.key)
      set(Keys.LEVEL, level)
      summoner?.let { set(Keys.SUMMONER, it.toString()) }
      set(Keys.DIFFICULTY, difficulty)
    }
    fun graded(weight: Double) = Difficulty.part(difficulty, weight)

    // From the type's own values: a minion that rolls into a boss may have been changed already.
    stat(entity, Attributes.MAX_HEALTH) { (Stats.health(formula.health, it, clazz.health, level) * graded(weights.health)).coerceAtMost(MAX_HEALTH) }
      ?.let { entity.health = entity.maxHealth }
    stat(entity, Attributes.ATTACK_DAMAGE) { Stats.damage(formula.damage, it, clazz.damage, level) * graded(weights.damage) }
    if (entity is Creeper) {
      if (type.spec.isPowered == true) entity.entityData.set(CreeperAccess.`mobosses$powered`(), true)
      // The game doubles a charged creeper's radius when it blows, so it gets half of the whole.
      val charge = if (entity.isPowered) 2 else 1
      (entity as CreeperAccess).`mobosses$radius`((Stats.explosionRadius(formula.explosion, CREEPER_RADIUS * charge, level) / charge.toDouble()).roundToInt())
    }
    // A piglin or hoglin in the overworld turns into a zombified one after a few seconds, and the boss would go with it.
    if (entity is AbstractPiglin) entity.setImmuneToZombification(true)
    if (entity is Hoglin) entity.setImmuneToZombification(true)
    if (!Equipment.equip(entity, type, level)) {
      stat(entity, Attributes.ARMOR) { (Stats.armor(formula.armor, it, clazz.defense, level) * graded(weights.armor)).coerceAtMost(formula.armor.capPoints.toDouble()) }
      stat(entity, Attributes.ARMOR_TOUGHNESS) { (Stats.armor(formula.toughness, it, clazz.toughness, level) * graded(weights.armor)).coerceAtMost(formula.toughness.capPoints.toDouble()) }
    }
  }

  /** Sets a stat from its type's default, never below it; null when the mob has no such stat. */
  private fun stat(entity: LivingEntity, attribute: Holder<Attribute>, value: (Double) -> Double): Unit? {
    val instance = entity.getAttribute(attribute) ?: return null
    val base = default(entity, attribute)
    instance.baseValue = value(base).coerceAtLeast(base)
    return Unit
  }

  /** The stat its type comes with, as Bukkit's `defaultValue`. */
  @Suppress("UNCHECKED_CAST")
  fun default(entity: LivingEntity, attribute: Holder<Attribute>): Double {
    val type = entity.type as net.minecraft.world.entity.EntityType<out LivingEntity>
    return if (DefaultAttributes.hasSupplier(type)) DefaultAttributes.getSupplier(type).let { if (it.hasAttribute(attribute)) it.getBaseValue(attribute) else attribute.value().defaultValue }
    else attribute.value().defaultValue
  }

  /** A creeper's blast radius in the game. */
  private const val CREEPER_RADIUS = 3

  /** The game's limit on max health. */
  const val MAX_HEALTH = 1024.0
}
