package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.algebra.Rolls
import ru.mairwunnx.mobosses.algebra.Stats
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.config.Classes
import ru.mairwunnx.mobosses.config.Settings
import java.io.File
import java.util.Locale
import kotlin.random.Random

/**
 * `./gradlew balance` prints the curves for the bundled config, or for the `config.yml` and
 * `classes.yml` in the folder given with `--args`: the same formulas the plugin runs, so the tables
 * never drift from the game.
 */
fun main(args: Array<String>) {
  fun text(name: String) = args.firstOrNull()?.let { File(it, name).readText() }
    ?: Settings::class.java.classLoader.getResource(name)!!.readText()
  val settings = Settings.parse(text("config.yml")) { println("config.yml: $it") }
  val classes = Classes.parse(text("classes.yml")) { println("classes.yml: $it") }.all
  val levels = listOf(1, 5, 10, 20, 30, 40, 50, 70, 100)
  val formula = settings.formula

  fun table(title: String, cell: (level: Int, clazz: BossClass) -> String) {
    println("\n$title")
    println("Lvl\t" + classes.joinToString("\t") { it.key })
    levels.forEach { level -> println("$level\t" + classes.joinToString("\t") { cell(level, it) }) }
  }

  fun Double.digits(n: Int) = String.format(Locale.ROOT, "%.${n}f", this)

  table("Health of a zombie (20)") { l, c -> Stats.health(formula.health, 20.0, c.health, l).digits(0) }
  table("Damage of a zombie (3)") { l, c -> Stats.damage(formula.damage, 3.0, c.damage, l).digits(1) }
  table("Armor of a zombie (2)") { l, c -> Stats.armor(formula.armor, 2.0, c.defense, l).digits(1) }
  table("Toughness of any mob (0)") { l, c -> Stats.armor(formula.toughness, 0.0, c.toughness, l).digits(1) }
  table("Vanilla experience from a zombie (5)") { l, c -> Stats.experience(formula.experience, 5, c.experience, 1.0, l).toString() }
  table("Mo’Bosses experience from a whole zombie kill") { l, c ->
    Leveling.bossExperience(settings.progression.expGain, Stats.health(formula.health, 20.0, c.health, l), c.experience, l).toString()
  }

  println("\nMinions per summon, without variance")
  println(levels.joinToString("\t") { "$it: ${Rolls.minions(formula.minions.copy(variance = 0.0), it, Random(0))}" })

  println("\nPlayer level by counted kills, without experience")
  println(listOf(10, 25, 50, 100, 200, 400, 800, 1600).joinToString("\t") { "$it: ${Leveling.playerLevel(settings.progression.levelCurve, 0, it)}" })
}
