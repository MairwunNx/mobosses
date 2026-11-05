package ru.mairwunnx.mobosses.progress

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.config.Settings

/** What a boss's statistics count: one spawned, one players killed, or a player it killed. */
enum class BossEvent { SPAWNED, KILLED, KILLED_A_PLAYER }

/**
 * What dying to bosses, and killing them, does to a player's record. Each death counts, and so does the streak of
 * deaths with no kill that counts between. With `death_penalty` on, a death burns the way to the next level; from
 * `death_demotion` deaths in a row, it drops a level too.
 */
object Deaths {
  /** A death to a boss: [demoted] when it cost a level, [lost] the experience it took. */
  data class Death(val before: Progress, val after: Progress, val demoted: Boolean) {
    val lost get() = before.experience - after.experience
  }

  fun died(progress: Progress, settings: Settings.Progression): Death {
    val streak = progress.streak + 1
    val demoted = settings.deathPenalty && settings.deathDemotion > 0 && streak >= settings.deathDemotion
    val experience = when {
      !settings.deathPenalty -> progress.experience
      demoted -> Leveling.demotedFloor(settings.levelCurve, progress.experience)
      else -> Leveling.levelFloor(settings.levelCurve, progress.experience)
    }
    return Death(progress, progress.copy(experience = experience, deaths = progress.deaths + 1, streak = streak), demoted)
  }

  /** A kill's share: its experience and damage, and a kill that counts, which ends the streak of deaths. */
  fun killed(progress: Progress, kills: Int, experience: Long, damage: Double): Progress = progress.copy(
    kills = progress.kills + kills, experience = progress.experience + experience, damage = progress.damage + damage,
    streak = if (kills > 0) 0 else progress.streak,
  )
}
