package ru.mobosses.entities

import org.bukkit.entity.LivingEntity
import ru.mairwunnx.mobosses.models.BossesConfigurationModel.BossConfig
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel.BossClass
import java.util.UUID

class BossEntity(
  val entity: LivingEntity,
  val bossType: BossConfig,
  val clazz: BossClass,
  var level: Int,
  val parentBoss: UUID? = null,
  val hierarchyDepth: Int = 0
) {
  val uuid: UUID = entity.uniqueId
  val color = clazz.color
  val displayName: String get() = "${bossType.name} ${color}[Lv. $level]"

  val childBosses: MutableSet<UUID> = mutableSetOf()

  fun addChildBoss(child: UUID) { childBosses += child }
  fun removeChildBoss(child: UUID) { childBosses -= child }

  fun canHaveMoreChildBosses(maxAllowed: Int) = childBosses.size < maxAllowed
}