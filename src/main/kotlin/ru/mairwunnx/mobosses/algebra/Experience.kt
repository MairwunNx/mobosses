package ru.mairwunnx.mobosses.algebra

import org.bukkit.attribute.Attribute
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.ExpGain
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.FormulaSection
import ru.mobosses.entities.BossEntity
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 *   maxScale (L)  – потолок кэфа опыта по уровню
 *   power    (p)  – влияет на крутизну
 *   offset   (K)  – смещает точку перегиба
 */

context (preset: FormulaSection) fun experience(boss: BossEntity, baseExp: Int): Int {
  val level = boss.level.toDouble()
  val levelScale = 1.0 + (preset.maxScale - 1.0) * level.pow(preset.power) / (preset.offset + level.pow(preset.power))

  val classMul = boss.clazz.experience
  val expModifier = boss.bossType.expModifier
  return (baseExp * classMul * levelScale * expModifier).roundToInt()
}

context(preset: ExpGain) fun innerexperience(boss: BossEntity): Long {
  val hpPart    = boss.entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue * preset.healthFactor
  val lvlPart   = preset.levelFactor * boss.level.toDouble().pow(preset.levelPower)
  val rarity    = if (preset.rarityMul) boss.clazz.experience else 1.0
  return ((hpPart + lvlPart) * rarity).toLong()
}