package ru.mairwunnx.mobosses.hooks

import chat.allay.expansion.AllayApi
import chat.allay.expansion.AllayExpansion
import chat.allay.expansion.AllayExpansions
import chat.allay.expansion.AllayTool
import chat.allay.expansion.Asker
import chat.allay.expansion.ToolCall
import chat.allay.expansion.ToolException
import chat.allay.expansion.ToolResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.ability.Abilities
import ru.mairwunnx.mobosses.ability.kinds.Enrage
import ru.mairwunnx.mobosses.ability.signature.Signature
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.lang.LocaleResolver
import kotlin.math.roundToInt

/**
 * Mo’Bosses' tools in Allay, through its expansion API (`chat.allay:allay-expansion`). Allay brings
 * the API's classes, so this class loads only when it runs; [AllayLink] is the side the rest of the
 * plugin calls. Every tool only reads, so Allay offers them to everyone, in `/askx` too. Names come in
 * the language Allay answers the asker in.
 */
class AllayBosses private constructor(private val plugin: MoBossesPlugin) : AllayExpansion {
  override fun id() = ID

  override fun requiresApi(): String = AllayApi.VERSION

  override fun name() = "Mo’Bosses"

  override fun version(): String = plugin.pluginMeta.version

  override fun promptExpansion() = PROMPT

  override fun tools(): List<AllayTool> = listOf(
    tool("classes", "The six boss classes, Common to Mythic, as this server sets them: each one's name, its share of new bosses in percent, what it multiplies (health, damage, experience, armor, toughness), and the boss level each ability starts at in it. Ability keys like fire_ring; `signature` stands for every boss type's own abilities.", true) { call ->
      val code = language(call.asker())
      val catalog = plugin.catalog
      val classes = catalog.classes.all
      val total = classes.sumOf { it.chance.coerceAtLeast(0.0) }.takeIf { it > 0 } ?: 1.0
      val abilities = (Abilities.PICKED.filterNot { it is Signature } + Enrage).filter { it.enabled(catalog.abilities) }
      ToolResult.of(mapOf("classes" to classes.map { clazz ->
        mapOf(
          "key" to clazz.key, "name" to plain(plugin.texts.className(code, clazz)), "chance_percent" to round(clazz.chance.coerceAtLeast(0.0) * 100 / total),
          "health" to clazz.health, "damage" to clazz.damage, "experience" to clazz.experience, "armor" to clazz.defense, "toughness" to clazz.toughness,
          "abilities_from_level" to abilities.associate { it.key to it.threshold(clazz.abilityThresholds) } + ("signature" to clazz.abilityThresholds.signature),
        )
      }))
    },
    tool("bosses", "The mobs that can become bosses on this server: each one's id, name, the chance in percent that it spawns as a boss near a player, its bonus loot chance, the minion it summons, and its own signature abilities. Also whether bosses spawn by themselves at all here.", true) { call ->
      val code = language(call.asker())
      val catalog = plugin.catalog
      val types = catalog.types.values.filter { it.spec.enabled }.sortedBy { it.key }
      ToolResult.of(mapOf(
        "natural_spawns" to catalog.settings.spawn.enabled,
        "bosses" to types.map { type ->
          mapOf(
            "id" to type.entity.key.toString(), "name" to plain(plugin.texts.bossName(code, type)),
            "spawn_chance_percent" to round(type.spec.spawnChance * 100), "bonus_loot_chance_percent" to round(type.spec.bonusLootChance * 100),
            "minion" to catalog.minions[type.entity]?.entity?.key?.toString(),
            "signature_abilities" to catalog.signatureTypes.filter { (key, types) -> type.entity in types && catalog.abilities.signatures[key]?.enabled != false }.keys.sorted(),
          )
        },
      ))
    },
    tool("hunt", "The asker's own Mo’Bosses hunt: their level and the top level, experience and the experience the next level needs, bosses killed, place among hunters, seconds until a boss may spawn near them again, minutes of their survival bonus, whether dying to a boss burns progress here, and their bestiary (kills and best level by boss type and class).", false) { call ->
      val player = player(call.asker())
      val curve = plugin.catalog.settings.progression.levelCurve
      val deathPenalty = plugin.catalog.settings.progression.deathPenalty
      ToolResult.later(plugin.menuStats.card(player).thenCombine(plugin.progression.bestiary(player.uniqueId)) { card, bestiary ->
        ToolResult.of(mapOf(
          "level" to card.level, "max_level" to curve.maxLevel, "experience" to card.progress.experience,
          "next_level_experience" to Leveling.experienceFor(curve, card.level + 1),
          "kills" to card.progress.kills, "rank" to card.rank, "next_boss_in_seconds" to card.nextBossInSeconds,
          "survival_minutes" to card.survivalMinutes, "death_penalty" to deathPenalty,
          "bestiary" to bestiary.map { mapOf("type" to it.type, "class" to it.clazz, "kills" to it.kills, "best_level" to it.bestLevel) },
        )).summary("level ${card.level}")
      })
    },
    tool("nearby", "The bosses within $NEARBY blocks of the asker, nearest first, at most $MAX_NEARBY: type, name, class, level, health and max health, whether it's enraged or in its second phase, its coordinates, and the distance.", false) { call ->
      val player = player(call.asker())
      val code = language(call.asker())
      val at = player.location
      val bosses = plugin.bosses.all
        .filter { it.entity.isValid && it.entity.world == at.world && it.entity.location.distanceSquared(at) <= NEARBY * NEARBY }
        .sortedBy { it.entity.location.distanceSquared(at) }
        .take(MAX_NEARBY)
      ToolResult.of(mapOf("bosses" to bosses.map { boss ->
        val where = boss.entity.location
        mapOf(
          "type" to boss.type.entity.key.toString(), "name" to plain(plugin.texts.bossName(code, boss.type)),
          "class" to boss.clazz.key, "level" to boss.level,
          "health" to round(boss.entity.health), "max_health" to round(boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0),
          "enraged" to boss.enraged, "phase_two" to boss.phaseTwo,
          "x" to where.blockX, "y" to where.blockY, "z" to where.blockZ, "distance" to where.distance(at).roundToInt(),
        )
      })).summary("${bosses.size}")
    },
  )

  private fun player(asker: Asker): Player = asker.unwrap(Player::class.java) ?: throw ToolException("the asker isn't a player in the world")

  /** The asker's language among Mo’Bosses' own: `pt_br`, else `pt`, else the server's default. */
  private fun language(asker: Asker): String {
    val full = LocaleResolver.normalize(asker.language())
    return listOf(full, full.substringBefore('_')).firstOrNull { it in plugin.languages.codes } ?: plugin.languages.default
  }

  private fun plain(name: Component) = PlainTextComponentSerializer.plainText().serialize(name)

  private fun round(value: Double) = (value * 100).roundToInt() / 100.0

  private fun tool(name: String, description: String, inPublic: Boolean, run: (ToolCall) -> ToolResult) = object : AllayTool {
    override fun name() = name
    override fun description() = description
    override fun inPublic() = inPublic
    override fun run(call: ToolCall) = run(call)
  }

  companion object {
    const val ID = "mobosses"
    private const val NEARBY = 64.0
    private const val MAX_NEARBY = 10

    private val PROMPT = """
      Mo’Bosses turns ordinary mobs into bosses. A boss has a level and one of six classes: Common, Uncommon, Rare, Epic, Legendary, Mythic; the rarer the class, the more health, damage, and armor. Its name tag and boss bar show both. A boss casts abilities it has reached the level for in its class: lightning, minions, fire and web rings, meteors, arrow volleys, and its mob type's signature move, among others. It drops loot of its class and the classes below.
      Players have a Mo’Bosses level of their own, apart from vanilla experience, from killing bosses. A natural boss's level follows the levels of the players near it. Experience, kills, and loot go by each player's share of the damage; a boss far below a player's level gives less and may not count as a kill.
      Commands: /mobosses opens the menu (the player's card, the best hunters, the bestiary, the language), /mobosses info [player], /mobosses top, /mobosses language <code|auto>, /mobosses reset.
      This server's classes, bosses, and numbers differ from the defaults: read them with the tools instead of guessing. For a fight, read the boss with the nearby tool and advise on its class, level, and abilities.
    """.trimIndent()

    fun register(plugin: MoBossesPlugin) = AllayExpansions.register(AllayBosses(plugin))

    fun unregister() = AllayExpansions.unregister(ID)
  }
}
