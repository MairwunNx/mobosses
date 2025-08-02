package ru.mairwunnx.mobosses.algebra

import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.LevelCurve
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.MiniBossesCurve
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel.Variance
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

context(preset: LevelCurve) fun playerlevel(totalExp: Long, bossKills: Int): Int {
  val raw = sqrt(totalExp.toDouble()) / preset.expScale +
    bossKills.toDouble() / preset.killScale       //   ≥ 0
  val progress = raw.pow(preset.power) /
    (preset.offset + raw.pow(preset.power))       // 0‥1
  return (1 + (preset.maxLevel - 1) * progress).toInt()
}

context(preset: Variance) fun bosslevel(playerLvl: Int, rng: Random): Int {
  val progress = playerLvl / 100.0                    // 0‥1
  val chance   = preset.baseChance + preset.extraChance * progress
  if (rng.nextDouble() > chance) return playerLvl     // без разброса

  val maxRed = (preset.minReduction +
    (preset.maxReduction - preset.minReduction) *
    progress.pow(preset.power)).toInt()
  val reduction = rng.nextInt(preset.minReduction, maxRed + 1)
  return (playerLvl - reduction).coerceAtLeast(1)
}

fun childlevel(parentLevel: Int, c: MiniBossesCurve, rng: Random = Random.Default): Int {
  val L = parentLevel.coerceAtLeast(1).toDouble()
  val lp = L.pow(c.power)
  val progress = lp / (c.offset + lp)                // 0..~1
  val curveDelta = (c.minDelta + (c.maxDelta - c.minDelta) * progress).roundToInt()
  val percentDelta = floor(parentLevel * c.percentCap).toInt().coerceAtLeast(c.minDelta)
  val baseDelta = min(curveDelta, percentDelta)
  val jitter = if (c.jitter > 0) rng.nextInt(-c.jitter, c.jitter + 1) else 0
  val finalDelta = (baseDelta + jitter).coerceAtLeast(c.minDelta)
  return (parentLevel - finalDelta).coerceAtLeast(1)
}