package ru.mairwunnx.mobosses.modcore.hooks

import chat.allay.expansion.Asker
import chat.allay.expansion.v2.Call
import chat.allay.expansion.v2.Expansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.ability.Abilities
import ru.mairwunnx.mobosses.modcore.ability.kinds.Enrage
import java.util.concurrent.CompletionStage
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Mo’Bosses' tools in Allay, through its expansion API 2 (`chat.allay:allay-expansion`, package `v2`). Allay brings
 * the API's classes since 6.33.0, so this object loads only when it runs; [AllayLink] is the side the rest of the mod
 * calls. The same tools, prompt, and answers as on Paper. Every tool only reads, so Allay offers them to everyone, in
 * `/askx` too. Names come in the language Allay answers the asker in. The answers keep every key API 1 wrote.
 */
object AllayBosses : Expansion("mobosses", "Mo’Bosses") {
  /** The server the tools read, set before [register]. */
  @Volatile lateinit var mod: MoBosses

  private const val NEARBY = 64.0
  private const val MAX_NEARBY = 10

  val classes = tool("classes", "The six boss classes, Common to Mythic, as this server sets them: each one's name, its share of new bosses in percent, what it multiplies (health, damage, experience, armor, toughness), and the boss level each ability starts at in it. Ability keys like fire_ring; `signature` stands for every boss type's own abilities.", ::classes).inPublic()
  val bosses = tool("bosses", "The mobs that can become bosses on this server: each one's id, name, the chance in percent that it spawns as a boss near a player, its bonus loot chance, the minion it summons, and its own signature abilities. Also whether bosses spawn by themselves at all here.", ::bosses).inPublic()
  val hunt = toolLater("hunt", "The asker's own Mo’Bosses hunt: their level and the top level, experience and the experience the next level needs, bosses killed, deaths to bosses and how many of them in a row (from the third in a row, each costs a level), damage dealt to bosses killed, place among hunters, seconds until a boss may spawn near them again, minutes of their survival bonus, whether dying to a boss burns progress here, and their bestiary (kills and best level by boss type and class).", ::hunt).summary { "level ${it.level}" }
  val nearby = tool("nearby", "The bosses within $NEARBY blocks of the asker, nearest first, at most $MAX_NEARBY: type, name, class, level, health and max health, whether it's enraged or in its second phase, its coordinates, and the distance.", ::nearby).summary { "${it.bosses.size}" }

  @JvmRecord data class Classes(val classes: List<ClassInfo>)

  @JvmRecord data class ClassInfo(val key: String, val name: String, val chancePercent: Double, val health: Double, val damage: Double, val experience: Double, val armor: Double, val toughness: Double, val abilitiesFromLevel: Map<String, Int>)

  @JvmRecord data class Bosses(val naturalSpawns: Boolean, val bosses: List<BossInfo>)

  @JvmRecord data class BossInfo(val id: String, val name: String, val spawnChancePercent: Double, val bonusLootChancePercent: Double, val minion: String?, val signatureAbilities: List<String>)

  @JvmRecord data class Hunt(val level: Int, val maxLevel: Int, val experience: Long, val nextLevelExperience: Long?, val kills: Int, val deaths: Int, val deathsInARow: Int, val damage: Double, val rank: Int?, val nextBossInSeconds: Long, val survivalMinutes: Int, val deathPenalty: Boolean, val bestiary: List<Kills>)

  @JvmRecord data class Kills(val type: String, val `class`: String, val kills: Long, val bestLevel: Int)

  @JvmRecord data class Nearby(val bosses: List<NearbyBoss>)

  @JvmRecord data class NearbyBoss(val type: String, val name: String, val `class`: String, val level: Int, val health: Double, val maxHealth: Double, val enraged: Boolean, val phaseTwo: Boolean, val x: Int, val y: Int, val z: Int, val distance: Int)

  override fun version(): String = mod.loader.version

  override fun prompt() = PROMPT

  private fun classes(call: Call): Classes {
    val code = language(call.asker())
    val catalog = mod.catalog
    val classes = catalog.classes.all
    val total = classes.sumOf { it.chance.coerceAtLeast(0.0) }.takeIf { it > 0 } ?: 1.0
    // Each boss type's own go as one, `signature`.
    val abilities = (Abilities.PICKED.filterNot { it.key in catalog.abilities.signatures } + Enrage).filter { it.enabled(catalog.abilities) }
    return Classes(classes.map { clazz ->
      val from = abilities.associate { it.key to it.threshold(clazz.abilityThresholds) } + ("signature" to clazz.abilityThresholds.signature)
      ClassInfo(clazz.key, plain(mod.texts.className(code, clazz)), round(clazz.chance.coerceAtLeast(0.0) * 100 / total), clazz.health, clazz.damage, clazz.experience, clazz.defense, clazz.toughness, from)
    })
  }

  private fun bosses(call: Call): Bosses {
    val code = language(call.asker())
    val catalog = mod.catalog
    val types = catalog.types.values.filter { it.spec.enabled }.sortedBy { it.key }
    return Bosses(catalog.settings.spawn.enabled, types.map { type ->
      val signatures = catalog.signatureTypes.filter { (key, types) -> type.entity in types && catalog.abilities.signatures[key]?.enabled != false }.keys.sorted()
      BossInfo(id(type.entity), plain(mod.texts.bossName(code, type)), round(type.spec.spawnChance * 100), round(type.spec.bonusLootChance * 100), catalog.minions[type.entity]?.entity?.let(::id), signatures)
    })
  }

  /** Reads the player on the server thread, then answers once the database has. */
  private fun hunt(call: Call): CompletionStage<Hunt> {
    val player = call.player(ServerPlayer::class.java)
    val curve = mod.catalog.settings.progression.levelCurve
    val deathPenalty = mod.catalog.settings.progression.deathPenalty
    return mod.menuStats.card(player.uuid, player.plainTextName).thenCombine(mod.progression.bestiary(player.uuid)) { card, bestiary ->
      val progress = card.progress
      Hunt(card.level, curve.maxLevel, progress.experience, Leveling.experienceFor(curve, card.level + 1), progress.kills, progress.deaths, progress.streak, progress.damage, card.rank, card.nextBossInSeconds, card.survivalMinutes, deathPenalty, bestiary.map { Kills(it.type, it.clazz, it.kills, it.bestLevel) })
    }
  }

  private fun nearby(call: Call): Nearby {
    val player = call.player(ServerPlayer::class.java)
    val code = language(call.asker())
    val bosses = mod.bosses.all.filter { it.entity.isAlive && it.entity.level() == player.level() && it.entity.distanceToSqr(player) <= NEARBY * NEARBY }.sortedBy { it.entity.distanceToSqr(player) }.take(MAX_NEARBY)
    return Nearby(bosses.map { boss ->
      val where = boss.entity
      NearbyBoss(id(boss.type.entity), plain(mod.texts.bossName(code, boss.type)), boss.clazz.key, boss.level, round(where.health.toDouble()), round(where.maxHealth.toDouble()), boss.enraged, boss.phaseTwo, where.blockX, where.blockY, where.blockZ, sqrt(where.distanceToSqr(player)).roundToInt())
    })
  }

  /** The asker's language among Mo’Bosses' own: `pt_br`, else `pt`, else the server's default. */
  private fun language(asker: Asker): String {
    val full = LocaleResolver.normalize(asker.language())
    return listOf(full, full.substringBefore('_')).firstOrNull { it in mod.languages.codes } ?: mod.languages.default
  }

  private fun id(type: EntityType<*>) = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()

  private fun plain(name: Component) = PlainTextComponentSerializer.plainText().serialize(name)

  private fun round(value: Double) = (value * 100).roundToInt() / 100.0

  private val PROMPT = """
    Mo’Bosses turns ordinary mobs into bosses. A boss has a level and one of six classes: Common, Uncommon, Rare, Epic, Legendary, Mythic; the rarer the class, the more health, damage, and armor. Its name tag and boss bar show both. A boss casts abilities it has reached the level for in its class: lightning, minions, fire and web rings, meteors, arrow volleys, and its mob type's signature move, among others. It drops loot of its class and the classes below.
    Players have a Mo’Bosses level of their own, apart from vanilla experience, from killing bosses. A natural boss's level follows the levels of the players near it. Experience, kills, and loot go by each player's share of the damage; a boss far below a player's level gives less and may not count as a kill.
    Commands: /mobosses opens the menu (the player's card, the best hunters, the bestiary, the language), /mobosses info [player], /mobosses top, /mobosses language <code|auto>, /mobosses reset.
    This server's classes, bosses, and numbers differ from the defaults: read them with the tools instead of guessing. For a fight, read the boss with the nearby tool and advise on its class, level, and abilities.
  """.trimIndent()
}
