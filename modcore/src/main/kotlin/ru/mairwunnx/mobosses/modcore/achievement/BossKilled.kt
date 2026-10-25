package ru.mairwunnx.mobosses.modcore.achievement

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.predicates.MinMaxBounds
import net.minecraft.advancements.triggers.SimpleCriterionTrigger
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import ru.mairwunnx.mobosses.config.Classes
import java.util.Optional

/**
 * `mobosses:boss_killed`, a trigger for a server's own advancements in data packs: met by every player whose share
 * made a boss's kill theirs. Every condition is optional:
 *
 * ```json
 * "criteria": { "slayer": { "trigger": "mobosses:boss_killed", "conditions": {
 *   "type": "zombie", "class": { "min": "epic" }, "level": { "min": 50 } } } }
 * ```
 *
 * `type` takes a boss type or a list of them, `class` a class or `min` and `max` by rank, `level` a number or `min`
 * and `max`, and `player` the game's usual predicate. The game never tells clients about triggers, so a vanilla client
 * plays such advancements.
 */
class BossKilled : SimpleCriterionTrigger<BossKilled.Instance>() {
  override fun codec(): Codec<Instance> = Instance.CODEC

  fun trigger(player: ServerPlayer, type: String, clazz: String, level: Int) = trigger(player) { it.matches(type, clazz, level) }

  /** A class, or the classes from [min] to [max] by rank. */
  data class ClassRange(val min: String?, val max: String?) {
    fun matches(clazz: String): Boolean {
      val rank = RANKS.indexOf(clazz.lowercase())
      if (rank < 0) return false
      val low = min?.let { RANKS.indexOf(it.lowercase()) } ?: 0
      val high = max?.let { RANKS.indexOf(it.lowercase()) } ?: RANKS.lastIndex
      return rank in low..high
    }

    companion object {
      private val RANKS = Classes.KEYS

      private val RANGE: Codec<ClassRange> = RecordCodecBuilder.create { i ->
        i.group(
          Codec.STRING.optionalFieldOf("min").forGetter { Optional.ofNullable(it.min) },
          Codec.STRING.optionalFieldOf("max").forGetter { Optional.ofNullable(it.max) },
        ).apply(i) { min, max -> ClassRange(min.orElse(null), max.orElse(null)) }
      }

      val CODEC: Codec<ClassRange> = Codec.either(Codec.STRING, RANGE).xmap(
        { either -> either.map({ ClassRange(it, it) }, { it }) },
        { range -> if (range.min != null && range.min == range.max) Either.left(range.min) else Either.right(range) },
      )
    }
  }

  data class Instance(
    private val player: Optional<Holder<LootItemCondition>>,
    val types: Optional<List<String>>,
    val clazz: Optional<ClassRange>,
    val level: Optional<MinMaxBounds.Ints>,
  ) : SimpleInstance {
    override fun player(): Optional<Holder<LootItemCondition>> = player

    fun matches(type: String, clazz: String, level: Int): Boolean =
      types.map { list -> list.any { it.equals(type, ignoreCase = true) || it.equals("minecraft:$type", ignoreCase = true) } }.orElse(true) &&
        this.clazz.map { it.matches(clazz) }.orElse(true) &&
        this.level.map { it.matches(level) }.orElse(true)

    companion object {
      private val TYPES: Codec<List<String>> = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
        { either -> either.map({ listOf(it) }, { it }) },
        { list -> if (list.size == 1) Either.left(list[0]) else Either.right(list) },
      )

      val CODEC: Codec<Instance> = RecordCodecBuilder.create { i ->
        i.group(
          LootItemCondition.CODEC.optionalFieldOf("player").forGetter { it.player() },
          TYPES.optionalFieldOf("type").forGetter(Instance::types),
          ClassRange.CODEC.optionalFieldOf("class").forGetter(Instance::clazz),
          MinMaxBounds.Ints.CODEC.optionalFieldOf("level").forGetter(Instance::level),
        ).apply(i, ::Instance)
      }
    }
  }

  companion object {
    val ID: Identifier = Identifier.fromNamespaceAndPath("mobosses", "boss_killed")

    /** The one the game knows: each loader registers it under [ID] before the game's registries close. */
    val TRIGGER = BossKilled()
  }
}
