package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.boss.Boss
import ru.mairwunnx.mobosses.hooks.PlanData
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * A boss's death. Each player's share of its health, their pets' damage included, decides their part
 * of the vanilla experience and of the Mo’Bosses experience, the latter weighed by how close the boss
 * was to their level; the pets' part of it is worth less at high levels. A share past the thresholds,
 * lower in a group, brings loot and rewards, and the kill counts toward their level. A creeper boss
 * that blows itself up never dies here: nobody killed it, and nobody gets anything.
 */
class DeathListener(private val plugin: MoBossesPlugin) : Listener {
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onDeath(event: EntityDeathEvent) {
    val boss = plugin.bosses.get(event.entity) ?: return
    val maxHealth = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0
    // Health groups added counts as the boss's, so the shares still add up to the whole of it.
    val shares = Shares.of(boss.damage, plugin.groupHealth.whole(boss, maxHealth))
    if (shares.isNotEmpty()) {
      val formula = plugin.catalog.settings.formula
      val graded = boss.graded(plugin.catalog.settings.difficulty.weights.experience)
      val experience = (Stats.experience(formula.experience, event.droppedExp, boss.clazz.experience, boss.type.spec.expModifier, boss.level) * graded).roundToInt().coerceAtLeast(event.droppedExp)
      event.droppedExp = 0
      val pets = boss.petDamage
      val dealt = boss.damage
      val group = shares.count { it.value > 0.0 }
      val looted = HashMap<Player, Double>()
      shares.forEach { (id, share) ->
        val worth = Shares.petWorth(dealt[id] ?: 0.0, pets[id] ?: 0.0, Leveling.petWeight(plugin.catalog.settings.progression.petDamage, boss.level))
        Bukkit.getPlayer(id)?.let { pay(it, boss, share, worth, group, experience, looted) }
      }
      plugin.loot.bonus(boss, looted)
      plugin.progression.count(boss.type.key, boss.clazz.key, killed = true)
      announce(boss, shares, experience)
      plugin.worldApi.bossKilled(boss, shares, experience)
    }
    boss.type.deathSound?.let { boss.entity.world.playSound(boss.entity.location, it, 1f, 1f) }
    plugin.bosses.forget(boss)
    plugin.decoys.removeOf(boss.uuid)
  }

  /** [petWorth] is the share of the player's damage that counts for experience: 1 without pets. Players who get loot go into [looted]. */
  private fun pay(player: Player, boss: Boss, share: Double, petWorth: Double, group: Int, experience: Int, looted: MutableMap<Player, Double>) {
    val settings = plugin.catalog.settings
    // A spawner boss farmed in one spot gives less, and comes back to full with time.
    val fatigue = if (boss.fromSpawner) plugin.fatigue.take(player, boss.entity.location) else 1.0
    val baseHealth = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 0.0
    val payout = Payout.of(settings, plugin.catalog.rewards, share, petWorth, fatigue, experience, baseHealth, boss.clazz.experience, boss.level, plugin.progression.level(player.uniqueId), group)
    player.giveExp(payout.vanilla)

    val levelBefore = plugin.progression.level(player.uniqueId)
    plugin.progression.add(player.uniqueId, player.name, payout.gained, payout.counts)
    plugin.progression.level(player.uniqueId).takeIf { it > levelBefore }?.let { plugin.worldApi.levelUp(player, levelBefore, it) }
    if (PlanData.available) PlanData.update(player)

    if (share >= payout.lootShare && Random.nextDouble() < fatigue) {
      plugin.loot.drop(boss, player)
      looted[player] = share
    }

    plugin.achievements.killed(player, boss, payout.took, payout.counts, alone = group == 1)
    if (payout.took) plugin.progression.bestiaryKill(player.uniqueId, boss.type.key, boss.clazz.key, boss.level).thenAccept { entries ->
      Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) plugin.achievements.bestiary(player, entries) })
    }
    val rewardsIn = if (share >= payout.rewardsShare) plugin.rewards.give(player, boss) else null
    val volume = (0.2f + 0.15f * plugin.catalog.classes.rank(boss.clazz.key).coerceAtLeast(0)).coerceAtMost(1f)
    plugin.catalog.killSound?.let { player.playSound(player.location, it, volume, 1f) }
    plugin.log.debug { "${player.name} dealt ${"%.0f".format(share * 100)}% of a level ${boss.level} ${boss.clazz.key} ${boss.type.key}: +${payout.gained} Mo’Bosses experience" }

    if (!settings.broadcast.killNotes) return
    val petShare = (boss.petDamage[player.uniqueId] ?: 0.0) / (boss.damage[player.uniqueId] ?: 1.0)
    plugin.texts.sendBlock(player, payout.notes(settings, plugin.texts.boss(plugin.texts.languageOf(player), boss), petShare, rewardsIn))
  }

  private fun announce(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    if (!plugin.catalog.settings.broadcast.kills) return
    // Players who left before the boss fell are named too.
    val names = shares.keys.mapNotNull { Bukkit.getPlayer(it)?.name ?: Bukkit.getOfflinePlayer(it).name }.joinToString(", ")
    plugin.texts.broadcast(Message.KILL) { code ->
      arrayOf(Placeholder.unparsed("player", names), Placeholder.unparsed("experience", experience.toString()), *plugin.texts.boss(code, boss))
    }
  }
}
