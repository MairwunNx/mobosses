package ru.mairwunnx.mobosses.modcore.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import ru.mairwunnx.mobosses.combat.LootRules
import net.minecraft.world.entity.ai.attributes.Attributes
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.combat.Payout
import ru.mairwunnx.mobosses.combat.Shares
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.World
import ru.mairwunnx.mobosses.modcore.boss.Boss
import java.util.UUID
import java.util.function.Consumer
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A boss's death, as Paper's `DeathListener`. Each player's share of its health, their pets' damage included, decides
 * their part ([Payout]): vanilla and Mo’Bosses experience, the kill toward their level, loot, rewards, and a note in
 * the chat about why it was less or more. The game's own experience orb stays home: the boss's goes out by share. The
 * game's own drops grow with the boss.
 */
class Deaths(private val mod: MoBosses) {
  /** Bosses whose experience went out by share, so the game drops none of its own. */
  private val paid = HashSet<UUID>()

  /** What the game's own drops from each dying boss multiply by, until they drop. */
  private val multipliers = HashMap<UUID, Double>()

  fun paid(entity: Entity) = paid.remove(entity.uuid)

  /** Whether the dying dragon's experience went out by share: it drops its own over several seconds, not through [paid]. */
  fun paidDragon(entity: Entity) = entity.uuid in paid

  /** A boss gone from the world: whatever it didn't use goes too. */
  fun forget(entity: Entity) {
    paid.remove(entity.uuid)
    multipliers.remove(entity.uuid)
  }

  /**
   * The game's own drops from a dying boss's loot table, Looting's included, multiplied as [died] decided, through
   * [drop]. Its gear drops elsewhere, as it is.
   */
  fun drops(entity: LivingEntity, drop: Consumer<ItemStack>): Consumer<ItemStack> {
    val multiplier = multipliers.remove(entity.uuid) ?: return drop
    val settings = mod.catalog.settings.loot.vanillaDrops
    return Consumer { stack ->
      if (!LootRules.multiplies(settings, BuiltInRegistries.ITEM.getKey(stack.item).toString(), stack.maxStackSize)) return@Consumer drop.accept(stack)
      var left = LootRules.vanillaCount(stack.count, multiplier)
      while (left > 0) {
        val count = minOf(left, stack.maxStackSize)
        drop.accept(stack.copyWithCount(count))
        left -= count
      }
    }
  }

  fun died(boss: Boss, source: DamageSource) {
    val entity = boss.entity
    val level = entity.level() as? ServerLevel ?: return
    val maxHealth = entity.getAttribute(Attributes.MAX_HEALTH)?.value ?: 0.0
    // Health groups added counts as the boss's, so the shares still add up to the whole of it.
    val shares = Shares.of(boss.damage, mod.groupHealth.whole(boss, maxHealth))
    if (shares.isNotEmpty()) {
      // What the game would drop: nothing for a boss no player hurt lately, as in vanilla.
      val dropped = if (entity.lastHurtByPlayer != null && entity.shouldDropExperience()) entity.getExperienceReward(level, source.entity) else 0
      val formula = mod.catalog.settings.formula
      val graded = boss.graded(mod.catalog.settings.difficulty.weights.experience)
      val experience = (Stats.experience(formula.experience, dropped, boss.clazz.experience, boss.type.spec.expModifier, boss.level) * graded).roundToInt().coerceAtLeast(dropped)
      paid += boss.uuid
      val pets = boss.petDamage
      val dealt = boss.damage
      val group = shares.count { it.value > 0.0 }
      val looted = HashMap<ServerPlayer, Double>()
      val fatigues = HashMap<UUID, Double>()
      shares.forEach { (id, share) ->
        val worth = Shares.petWorth(dealt[id] ?: 0.0, pets[id] ?: 0.0, Leveling.petWeight(mod.catalog.settings.progression.petDamage, boss.level))
        mod.server.playerList.getPlayer(id)?.let { pay(it, boss, share, worth, group, experience, looted, fatigues) }
      }
      mod.loot.bonus(boss, looted)
      // The fighters' spawner fatigue, by their shares, cuts the game's own drops too.
      val fatigue = shares.entries.sumOf { (id, share) -> share * (fatigues[id] ?: 1.0) } / shares.values.sum().coerceAtLeast(1e-9)
      val multiplier = LootRules.vanillaMultiplier(mod.catalog.settings.loot.vanillaDrops, boss.clazz.key, boss.level, fatigue)
      if (multiplier > 1.0) multipliers[boss.uuid] = multiplier
      mod.progression.count(boss.type.key, boss.clazz.key, killed = true)
      announce(boss, shares, experience)
      mod.worldApi.bossKilled(boss, shares, experience)
    }
    boss.type.deathSound?.let { World.sound(level, entity.position(), it) }
    mod.bosses.forget(boss)
    mod.decoys.removeOf(boss.uuid)
  }

  /**
   * [petWorth] is the share of the player's damage that counts for experience: 1 without pets. Players who get loot go
   * into [looted], and each player's spawner fatigue into [fatigues].
   */
  private fun pay(player: ServerPlayer, boss: Boss, share: Double, petWorth: Double, group: Int, experience: Int, looted: MutableMap<ServerPlayer, Double>, fatigues: MutableMap<UUID, Double>) {
    val settings = mod.catalog.settings
    // A spawner boss farmed in one spot gives less, and comes back to full with time.
    val fatigue = if (boss.fromSpawner) mod.fatigue.take(player, boss.entity) else 1.0
    fatigues[player.uuid] = fatigue
    val baseHealth = boss.entity.getAttribute(Attributes.MAX_HEALTH)?.baseValue ?: 0.0
    val levelBefore = mod.progression.level(player.uuid)
    val payout = Payout.of(settings, mod.catalog.rewards, share, petWorth, fatigue, experience, baseHealth, boss.clazz.experience, boss.level, levelBefore, group)
    player.giveExperiencePoints(payout.vanilla)

    mod.progression.add(player.uuid, player.plainTextName, payout.gained, payout.counts)
    mod.progression.level(player.uuid).takeIf { it > levelBefore }?.let { mod.worldApi.levelUp(player, levelBefore, it) }

    if (share >= payout.lootShare && Random.nextDouble() < fatigue) {
      mod.loot.drop(boss, player)
      looted[player] = share
    }

    mod.achievements.killed(player, boss, payout.took, payout.counts, alone = group == 1)
    if (payout.took) mod.progression.bestiaryKill(player.uuid, boss.type.key, boss.clazz.key, boss.level).thenAccept { entries ->
      mod.server.execute { if (!player.hasDisconnected()) mod.achievements.bestiary(player, entries) }
    }
    val rewardsIn = if (share >= payout.rewardsShare) mod.rewards.give(player, boss) else null
    val volume = (0.2f + 0.15f * mod.catalog.classes.rank(boss.clazz.key).coerceAtLeast(0)).coerceAtMost(1f)
    mod.catalog.killSound?.let { World.sound(player, it, volume = volume) }
    mod.log.debug { "${player.plainTextName} dealt ${"%.0f".format(share * 100)}% of a level ${boss.level} ${boss.clazz.key} ${boss.type.key}: +${payout.gained} Mo’Bosses experience" }

    if (!settings.broadcast.killNotes) return
    val petShare = (boss.petDamage[player.uuid] ?: 0.0) / (boss.damage[player.uuid] ?: 1.0)
    mod.texts.sendBlock(player, payout.notes(settings, mod.texts.boss(mod.texts.languageOf(player), boss), petShare, rewardsIn))
  }

  private fun announce(boss: Boss, shares: Map<UUID, Double>, experience: Int) {
    if (!mod.catalog.settings.broadcast.kills) return
    // Players who left before the boss fell are named too.
    val names = shares.keys.mapNotNull(mod::nameOf).joinToString(", ")
    mod.texts.broadcast(Message.KILL) { code ->
      arrayOf(Placeholder.unparsed("player", names), Placeholder.unparsed("experience", experience.toString()), *mod.texts.boss(code, boss))
    }
  }
}
