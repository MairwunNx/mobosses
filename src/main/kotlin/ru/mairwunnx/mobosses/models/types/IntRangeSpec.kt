package ru.mairwunnx.mobosses.models.types

import kotlinx.serialization.Serializable
import ru.mairwunnx.mobosses.serializers.IntRangeSpecSerializer
import kotlin.random.Random

@Serializable(with = IntRangeSpecSerializer::class) class IntRangeSpec(val min: Int, val max: Int) {
  fun sample(rng: Random = Random): Int = if (min == max) min else rng.nextInt(min, max + 1)
  override fun toString() = if (min == max) "$min" else "$min-$max"
}
