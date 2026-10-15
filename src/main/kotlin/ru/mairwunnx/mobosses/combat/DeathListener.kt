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
      val experience = Stats.experience(formula.experience, event.droppedExp, boss.clazz.experience, boss.type.spec.expModifier, boss.level)
      event.droppedExp = 0
      val pets = boss.petDamage
      val dealt = boss.damage
      val group = shares.count { it.value > 0.0 }
      shares.forEach { (id, share) ->
        val worth = Shares.petWorth(dealt[id] ?: 0.0, pets[id] ?: 0.0, Leveling.petWeight(plugin.catalog.settings.progression.petDamage, boss.level))
        Bukkit.getPlayer(id)?.let { pay(it, boss, share, worth, group, experience) }
      }
      plugin.progression.count(boss.type.key, boss.clazz.key, killed = true)
      announce(boss, shares, experience)
      plugin.worldApi.bossKilled(boss, shares, experience)
    }
    boss.type.deathSound?.let { boss.entity.world.playSound(boss.entity.location, it, 1f, 1f) }
    plugin.bosses.forget(boss)
  }

  /** [petWorth] is the share of the player's damage that counts for experience: 1 without pets. */
  private fun pay(player: Player, boss: Boss, share: Double, petWorth: Double, group: Int, experience: Int) {
    val settings = plugin.catalog.settings
    val progression = settings.progression
    // A spawner boss farmed in one spot gives less, and comes back to full with time.
    val fatigue = if (boss.fromSpawner) plugin.fatigue.take(player, boss.entity.location) else 1.0
    val vanilla = Math.round(experience * share * petWorth * fatigue).toInt()
    player.giveExp(vanilla)

    val level = plugin.progression.level(player.uniqueId)
    val worth = Leveling.bossExperience(progression.expGain, boss.entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 0.0, boss.clazz.experience, boss.level)
    val counts = Leveling.countsAsKill(progression, share, group, level, boss.level)
    val relevance = Leveling.relevance(progression.relevance, level, boss.level)
    val gained = Leveling.gained(worth * share * petWorth * relevance * fatigue, counts)
    val levelBefore = plugin.progression.level(player.uniqueId)
    plugin.progression.add(player, gained, counts)
    plugin.progression.level(player.uniqueId).takeIf { it > levelBefore }?.let { plugin.worldApi.levelUp(player, levelBefore, it) }
    if (PlanData.available) PlanData.update(player)

    // Four players who split a boss evenly each deal 25%; the thresholds come down to meet them.
    val lootShare = Leveling.threshold(settings.loot.minDamageShare, progression.groupShare, group)
    if (share >= lootShare && Random.nextDouble() < fatigue) plugin.loot.drop(boss, player)

    // Achievements and the bestiary take anyone whose share made the kill theirs, whatever the levels.
    val took = share >= Leveling.threshold(progression.minShare, progression.groupShare, group)
    plugin.achievements.killed(player, boss, took, counts, alone = group == 1)
    if (took) plugin.progression.bestiaryKill(player.uniqueId, boss.type.key, boss.clazz.key, boss.level).thenAccept { entries ->
      Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) plugin.achievements.bestiary(player, entries) })
    }
    val rewardsShare = Leveling.threshold(plugin.catalog.rewards.minDamageShare, progression.groupShare, group)
    val rewardsIn = if (share >= rewardsShare) plugin.rewards.give(player, boss) else null
    val volume = (0.2f + 0.15f * plugin.catalog.classes.rank(boss.clazz.key).coerceAtLeast(0)).coerceAtMost(1f)
    plugin.catalog.killSound?.let { player.playSound(player.location, it, volume, 1f) }
    plugin.log.debug { "${player.name} dealt ${"%.0f".format(share * 100)}% of a level ${boss.level} ${boss.clazz.key} ${boss.type.key}: +$gained Mo’Bosses experience" }

    if (!settings.broadcast.killNotes) return
    // One block in the chat: what they got, then why it was less, or more, line by line.
    val killShare = Leveling.threshold(progression.minShare, progression.groupShare, group)
    fun percent(name: String, value: Double) = Placeholder.unparsed(name, Math.round(value * 100).toString())
    val levels = arrayOf(Placeholder.unparsed("boss_level", boss.level.toString()), Placeholder.unparsed("your_level", level.toString()))
    val notes = mutableListOf<Pair<Message, Array<out TagResolver>>>()
    notes += Message.NOTE_HEADER to arrayOf(
      *plugin.texts.boss(plugin.texts.languageOf(player), boss), percent("share", share),
      Placeholder.unparsed("experience", vanilla.toString()), Placeholder.unparsed("mobosses_experience", gained.toString()),
    )
    // A few percent either way isn't worth a line.
    when {
      relevance >= 1.05 -> notes += Message.NOTE_ABOVE to arrayOf(*levels, percent("percent", relevance - 1.0))
      relevance <= 0.95 -> notes += Message.NOTE_BELOW to arrayOf(*levels, percent("percent", relevance))
    }
    when {
      share < killShare -> notes += Message.NOTE_SMALL_SHARE to arrayOf(percent("share", share), percent("kill", killShare), percent("loot", lootShare))
      !counts -> notes += Message.NOTE_NOT_COUNTED to levels
      share < lootShare -> notes += Message.NOTE_NO_LOOT to arrayOf(percent("share", share), percent("loot", lootShare))
    }
    if (fatigue < 0.95) notes += Message.NOTE_SPAWNER to arrayOf(percent("percent", fatigue))
    rewardsIn?.let { notes += Message.NOTE_REWARDS_COOLDOWN to arrayOf(Placeholder.unparsed("seconds", it.toString())) }
    if (petWorth <= 0.95) {
      val pets = (boss.petDamage[player.uniqueId] ?: 0.0) / (boss.damage[player.uniqueId] ?: 1.0)
      notes += Message.NOTE_PETS to arrayOf(percent("pets", pets), percent("weight", Leveling.petWeight(progression.petDamage, boss.level)))
    }
    plugin.texts.sendBlock(player, notes)
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
