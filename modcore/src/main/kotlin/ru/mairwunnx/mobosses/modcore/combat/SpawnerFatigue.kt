package ru.mairwunnx.mobosses.modcore.combat

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import ru.mairwunnx.mobosses.combat.Fatigue
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data

/** Bosses from spawners give less to a player who keeps killing them in one spot ([Fatigue]); the kills live on the player. */
class SpawnerFatigue(private val mod: MoBosses) {
  /** What the reward for a spawner boss killed where [where] stands is multiplied by, and the kill noted. */
  fun take(player: ServerPlayer, where: Entity, now: Long = mod.now()): Double {
    val cfg = mod.catalog.settings.spawnerFatigue
    if (!cfg.enabled) return 1.0
    val data = Data(player)
    val (factor, kills) = Fatigue.take(cfg, data.string(Keys.SPAWNER_KILLS), where.level().dimension().identifier().toString(), where.blockX, where.blockZ, now)
    data[Keys.SPAWNER_KILLS] = kills
    return factor
  }
}
