package ru.mobosses.entities

import org.bukkit.entity.LivingEntity
import ru.mairwunnx.mobosses.models.BossesConfigurationModel.BossConfig
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel.BossClass
import java.util.UUID

class BossEntity(val entity: LivingEntity, val bossType: BossConfig, val clazz: BossClass, var level: Int) {
  val uuid: UUID = entity.uniqueId
  val displayName: String get() = "${bossType.name} ${color}[Lv. ${level}]"
  val color = clazz.color
}