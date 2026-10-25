package ru.mairwunnx.mobosses.combat

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.RewardSettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.Message

/**
 * What one player gets from a boss they had a [share] of, the same on every platform: vanilla experience, Mo’Bosses
 * experience, whether the kill counts and makes the kill theirs, and the shares loot and rewards ask in a group of
 * [group]. [petWorth] is the share of their damage that counts for experience, 1 without pets; [fatigue] what spawner
 * fatigue leaves.
 */
class Payout(
  val share: Double,
  val petWorth: Double,
  val fatigue: Double,
  /** Vanilla experience. */
  val vanilla: Int,
  /** Mo’Bosses experience. */
  val gained: Long,
  /** The kill counts toward their kills. */
  val counts: Boolean,
  /** Their share made the kill theirs, for achievements and the bestiary, whatever the levels. */
  val took: Boolean,
  val relevance: Double,
  val playerLevel: Int,
  val bossLevel: Int,
  val killShare: Double,
  val lootShare: Double,
  val rewardsShare: Double,
) {
  /**
   * The kill note, one block in the chat: what they got, then why it was less, or more, line by line. [bossTags] name
   * the boss in their language; [petShare] is the part of their damage their pets dealt; [rewardsIn] the seconds until
   * they may get rewards again, when they got none for the wait.
   */
  fun notes(settings: Settings, bossTags: Array<TagResolver>, petShare: Double, rewardsIn: Long?): List<Pair<Message, Array<out TagResolver>>> {
    val progression = settings.progression
    fun percent(name: String, value: Double) = Placeholder.unparsed(name, Math.round(value * 100).toString())
    val levels = arrayOf(Placeholder.unparsed("boss_level", bossLevel.toString()), Placeholder.unparsed("your_level", playerLevel.toString()))
    val notes = mutableListOf<Pair<Message, Array<out TagResolver>>>()
    notes += Message.NOTE_HEADER to arrayOf(
      *bossTags, percent("share", share),
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
    if (petWorth <= 0.95) notes += Message.NOTE_PETS to arrayOf(percent("pets", petShare), percent("weight", Leveling.petWeight(progression.petDamage, bossLevel)))
    return notes
  }

  companion object {
    /**
     * [experience] is the vanilla experience the whole boss drops, [baseHealth] its max health before groups grew it,
     * [classMul] its class's experience multiplier.
     */
    fun of(
      settings: Settings, rewards: RewardSettings, share: Double, petWorth: Double, fatigue: Double, experience: Int,
      baseHealth: Double, classMul: Double, bossLevel: Int, playerLevel: Int, group: Int,
    ): Payout {
      val progression = settings.progression
      val vanilla = Math.round(experience * share * petWorth * fatigue).toInt()
      val worth = Leveling.bossExperience(progression.expGain, baseHealth, classMul, bossLevel)
      val counts = Leveling.countsAsKill(progression, share, group, playerLevel, bossLevel)
      val relevance = Leveling.relevance(progression.relevance, playerLevel, bossLevel)
      val gained = Leveling.gained(worth * share * petWorth * relevance * fatigue, counts)
      val killShare = Leveling.threshold(progression.minShare, progression.groupShare, group)
      // Four players who split a boss evenly each deal 25%; the thresholds come down to meet them.
      val lootShare = Leveling.threshold(settings.loot.minDamageShare, progression.groupShare, group)
      val rewardsShare = Leveling.threshold(rewards.minDamageShare, progression.groupShare, group)
      return Payout(share, petWorth, fatigue, vanilla, gained, counts, share >= killShare, relevance, playerLevel, bossLevel, killShare, lootShare, rewardsShare)
    }
  }
}
