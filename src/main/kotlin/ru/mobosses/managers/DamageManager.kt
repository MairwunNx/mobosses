package ru.mobosses.managers

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID

class DamageManager {
    private val damageMap = mutableMapOf<UUID, MutableMap<UUID, Double>>()

    fun addDamage(boss: LivingEntity, player: Player, damage: Double) {
        val bossDamagers = damageMap.getOrPut(boss.uniqueId) { mutableMapOf() }
        bossDamagers[player.uniqueId] = bossDamagers.getOrDefault(player.uniqueId, 0.0) + damage
    }

    fun getDamageContributors(boss: LivingEntity): Map<UUID, Double>? {
        return damageMap[boss.uniqueId]
    }

    fun clearDamage(boss: LivingEntity) {
        damageMap.remove(boss.uniqueId)
    }
}
