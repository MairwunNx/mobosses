package ru.mairwunnx.mobosses.modcore.ability.signature

import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.AbilityThresholds
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast

/**
 * An ability only some boss types have, set up under `signature` in `abilities.yml`, as on Paper. Each class gets
 * them from its `signature` level. [scaled] grows a number with the boss's level, up to a cap, times the ability's
 * `strength`.
 */
abstract class Signature(final override val key: String, final override val message: Message) : Ability {
  private fun spec(settings: AbilitySettings) = settings.signatures[key] ?: AbilitySettings.Signature()

  final override fun enabled(settings: AbilitySettings) = spec(settings).enabled

  final override fun threshold(thresholds: AbilityThresholds) = thresholds.signature

  final override fun cooldown(settings: AbilitySettings) = spec(settings).let { it.cooldown to it.cooldownBias }

  override fun usable(cast: Cast) = cast.entity.type in cast.mod.catalog.signatureTypes[key].orEmpty()

  protected fun strength(cast: Cast) = spec(cast.settings).strength.coerceAtLeast(0.0)

  protected fun scaled(cast: Cast, base: Double, perLevel: Double, max: Double) = (base + perLevel * cast.level).coerceAtMost(max) * strength(cast)

  /** Seconds as ticks, grown by level and strength. */
  protected fun ticks(cast: Cast, base: Double, perLevel: Double, max: Double) = (scaled(cast, base, perLevel, max) * 20).toInt().coerceAtLeast(1)
}
