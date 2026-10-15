package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.config.BossClass
import java.util.UUID

/** The keys under which a boss, a minion, or a meteor keeps its data on the entity, so it survives restarts and chunk unloads. */
class Keys(plugin: Plugin) {
  /** The boss type, like `zombie`. */
  val type = NamespacedKey(plugin, "type")
  val clazz = NamespacedKey(plugin, "class")
  val level = NamespacedKey(plugin, "level")
  /** On minions: the UUID of the boss that summoned them. */
  val summoner = NamespacedKey(plugin, "summoner")
  /** On meteors: they land without leaving a block. */
  val meteor = NamespacedKey(plugin, "meteor")
}

/**
 * A boss in a loaded chunk. Its type, class, and level live on the entity itself; the rest (who
 * hit it, cooldowns, effects) lasts as long as the chunk stays loaded.
 */
class Boss(
  val entity: LivingEntity,
  val type: Catalog.BossType,
  val clazz: BossClass,
  val level: Int,
  /** The boss that summoned this one, for minions that rolled into bosses. */
  val summoner: UUID?,
) {
  val uuid: UUID = entity.uniqueId

  /** Damage each player dealt, capped by the health the boss had left at each hit. */
  val damage = HashMap<UUID, Double>()
  val cooldowns = HashMap<String, Long>()
  var lastAbility = 0L
  var aura: BukkitTask? = null

  val color: TextColor get() = colorOf(clazz)

  companion object {
    fun colorOf(clazz: BossClass): TextColor =
      clazz.color.trim().let { TextColor.fromHexString(it) ?: NamedTextColor.NAMES.value(it.lowercase()) } ?: NamedTextColor.WHITE
  }
}
