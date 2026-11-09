package ru.mairwunnx.mobosses.hooks

import chat.allay.expansion.Asker
import chat.allay.expansion.ItemInfo
import chat.allay.expansion.ToolException
import chat.allay.expansion.v2.Call
import chat.allay.expansion.v2.Expansion
import chat.allay.expansion.v2.PlayerRef
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.ability.Abilities
import ru.mairwunnx.mobosses.ability.AbilityGuide
import ru.mairwunnx.mobosses.ability.kinds.Enrage
import ru.mairwunnx.mobosses.ability.signature.Signature
import ru.mairwunnx.mobosses.algebra.Leveling
import ru.mairwunnx.mobosses.boss.AdminSpawns
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.boss.WorldBosses
import ru.mairwunnx.mobosses.combat.LootGuide
import ru.mairwunnx.mobosses.combat.LootRules
import ru.mairwunnx.mobosses.combat.Shares
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.spawn.Hunt as HuntRules
import java.util.UUID
import java.util.concurrent.CompletionStage
import kotlin.math.roundToInt

/**
 * Mo’Bosses' tools in Allay, through its expansion API 2 (`chat.allay:allay-expansion`, package `v2`). Allay brings
 * the API's classes since 6.33.0, so this object loads only when it runs; [AllayLink] is the side the rest of the
 * plugin calls. Every tool but `spawn` only reads, so Allay offers them to everyone, in `/askx` too; `spawn` is for
 * operators with `mobosses.spawn`, as the command is. Names come in the language Allay answers the asker in. The
 * answers keep every key API 1 wrote. The loot table's named drops go to Allay's item catalog.
 */
object AllayBosses : Expansion("mobosses", "Mo’Bosses") {
  /** The plugin the tools read, set before [register]. */
  @Volatile lateinit var plugin: MoBossesPlugin

  private const val NEARBY = 64.0
  private const val MAX_NEARBY = 10
  private const val MAX_DROPS = 20
  private const val MAX_BONUS = 8
  private const val MAX_ITEMS = 400

  /** The six classes, as the model passes them. */
  enum class Rank { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }

  val bossClass = choice("class", "A boss class, Common to Mythic", Rank::class.java)
  val level = integer("level", "A boss level", 0, 1000)
  val type = string("type", "A boss type's id, like zombie or minecraft:wither_skeleton", 64)
  val item = string("item", "An item to look for, by id or by name in any language, like diamond_sword or Tracker’s Leather Vest", 64)
  val ability = string("ability", "One ability's key, like fire_ring or pounce", 32)
  val limit = integer("limit", "How many hunters, 1 to 10", 1, 10).orElse(10)
  val count = integer("count", "How many bosses, 1 to 5", 1, 5).orElse(1)

  val classes = tool("classes", "The six boss classes, Common to Mythic, as this server sets them: each one's name, its share of new bosses in percent, what it multiplies (health, damage, experience, armor, toughness), and the boss level each ability starts at in it. Ability keys like fire_ring, and `signature` stands for every boss type's own abilities.", ::classes).inPublic()
  val bosses = tool("bosses", "The mobs that can become bosses on this server: each one's id, name, the chance in percent that it spawns as a boss near a player, its bonus loot chance, the minion it summons, and its own signature abilities. Also whether bosses spawn by themselves at all here. With type, that boss alone, and in full under boss: its experience multiplier, whether only adults or charged ones turn, whether it's a world boss, each weapon and piece of armor it may wear with the lowest boss level that does, and how many loot entries are its own.", ::bosses).args(type).inPublic()
  val hunt = toolLater("hunt", "The asker's own Mo’Bosses hunt: their level and the top level, experience and the experience the next level needs, bosses killed, deaths to bosses and how many of them in a row (from the third in a row, each costs a level), damage dealt to bosses killed, place among hunters, seconds until a boss may spawn near them again, minutes and percent of their survival bonus (more bosses near them), whether a full moon is up in their world and what it multiplies spawn chances by, whether dying to a boss burns progress here, and their bestiary (kills and best level by boss type and class).", ::hunt).summary { "level ${it.level}" }
  val nearby = tool("nearby", "The bosses within $NEARBY blocks of the asker, nearest first, at most $MAX_NEARBY: type, name, class, level, health and max health, whether it's enraged or in its second phase, its coordinates, the distance, the abilities it has reached, the asker's own share of its damage in percent, how many players fight it, and how much its health grew for the group in percent.", ::nearby).summary { "${it.bosses.size}" }
  val loot = tool("loot", "What Mo’Bosses bosses drop on this server. With class and level (and type for a boss type's own entries), the likeliest items, each with its chance in percent to be one item a player gets, the amount, and its name. Also how many items each player who dealt enough damage gets, the damage share that takes alone, and the type's bonus roll. Without class it reads Common, and without level the asker's own Mo’Bosses level. With item, where that item comes from: the class, levels, and types that drop it, with its chance from a boss of its own class.", ::loot).args(bossClass, level, type, item).inPublic()
  val abilities = tool("abilities", "What boss abilities do on this server. Without ability, each shared ability with a line on what it does and the boss level it starts at in each class, Common to Mythic, and each boss type's own (signature) ability with its types, which all start at the signature levels. With ability, that one in full: what it does, a tip to counter it, its cooldown in seconds, its reach and damage from a low level to the cap, the health that sets it off, and where it starts.", ::abilities).args(ability).inPublic()
  val hunters = toolLater("hunters", "The best Mo’Bosses hunters on this server, by experience: place, the player, their level, bosses killed, and experience.", ::hunters).args(limit).inPublic().summary { "${it.hunters.size}" }
  val spawn = tool("spawn", "Spawns bosses two blocks ahead of the asker, as /mobosses spawn does and under the command's rules: they stay until killed. class defaults to Common, level to the asker's own Mo’Bosses level, and count to 1. Only on an operator's clear request.", ::spawn).args(type, bossClass, level, count).required(type).operator().permission(Permissions.SPAWN).summary { "${it.spawned.size}" }

  @JvmRecord data class Classes(val classes: List<ClassInfo>)

  @JvmRecord data class ClassInfo(val key: String, val name: String, val chancePercent: Double, val health: Double, val damage: Double, val experience: Double, val armor: Double, val toughness: Double, val abilitiesFromLevel: Map<String, Int>)

  @JvmRecord data class Bosses(val naturalSpawns: Boolean, val bosses: List<BossInfo>, val boss: BossDetail?)

  @JvmRecord data class BossInfo(val id: String, val name: String, val spawnChancePercent: Double, val bonusLootChancePercent: Double, val minion: String?, val signatureAbilities: List<String>)

  @JvmRecord data class BossDetail(val id: String, val experienceMultiplier: Double, val adultsOnly: Boolean, val charged: Boolean?, val worldBoss: Boolean, val weapons: Map<String, Int>, val armor: Map<String, Int>, val ownLootEntries: Int)

  @JvmRecord data class Hunt(val level: Int, val maxLevel: Int, val experience: Long, val nextLevelExperience: Long?, val kills: Int, val deaths: Int, val deathsInARow: Int, val damage: Double, val rank: Int?, val nextBossInSeconds: Long, val survivalMinutes: Int, val deathPenalty: Boolean, val bestiary: List<Kills>, val survivalBonusPercent: Int, val fullMoon: Boolean, val fullMoonChance: Double?)

  @JvmRecord data class Kills(val type: String, val `class`: String, val kills: Long, val bestLevel: Int)

  @JvmRecord data class Nearby(val bosses: List<NearbyBoss>)

  @JvmRecord data class NearbyBoss(val type: String, val name: String, val `class`: String, val level: Int, val health: Double, val maxHealth: Double, val enraged: Boolean, val phaseTwo: Boolean, val x: Int, val y: Int, val z: Int, val distance: Int, val abilities: List<String>, val yourSharePercent: Double, val fighters: Int, val groupHealthBonusPercent: Double)

  @JvmRecord data class Loot(val `class`: String?, val level: Int?, val type: String?, val item: String?, val itemsPerPlayer: Int?, val lootSharePercent: Double, val drops: List<Drop>, val more: Int, val bonusChancePercent: Double?, val bonus: List<Drop>)

  @JvmRecord data class Drop(val item: String, val name: String?, val chancePercent: Double?, val amount: String, val itemClass: String?, val fromClass: String, val minLevel: Int, val maxLevel: Int, val bossTypes: List<String>)

  @JvmRecord data class AbilitiesAnswer(val classes: List<String>, val abilities: List<AbilitySummary>, val signatureFromLevel: List<Int>, val signatures: List<SignatureSummary>, val disabled: List<String>, val ability: AbilityDetail?)

  @JvmRecord data class AbilitySummary(val key: String, val description: String, val fromLevel: List<Int>)

  @JvmRecord data class SignatureSummary(val key: String, val bossTypes: List<String>)

  @JvmRecord data class AbilityDetail(val key: String, val signature: Boolean, val enabled: Boolean, val description: String, val tip: String, val fromLevel: Map<String, Int>, val bossTypes: List<String>, val cooldownSeconds: Double?, val radius: Double?, val radiusMax: Double?, val damage: Double?, val damageMax: Double?, val healthPercent: Double?)

  @JvmRecord data class Hunters(val hunters: List<HunterInfo>)

  @JvmRecord data class HunterInfo(val place: Int, val player: PlayerRef, val level: Int, val kills: Int, val experience: Long)

  @JvmRecord data class Spawned(val spawned: List<SpawnedBoss>, val failed: String?)

  @JvmRecord data class SpawnedBoss(val type: String, val name: String, val `class`: String, val level: Int)

  override fun version(): String = plugin.pluginMeta.version

  override fun prompt() = PROMPT

  /** The loot table's named drops, so Allay's item search knows what each one is and where it comes from. */
  override fun items(): List<ItemInfo> = LootGuide.named(plugin.catalog.configs.loot.items, plugin.catalog.classes).filter { ID.matches(it.id) }.take(MAX_ITEMS).map(::itemInfo)

  private fun classes(call: Call): Classes {
    val code = language(call.asker())
    val catalog = plugin.catalog
    val classes = catalog.classes.all
    val total = classes.sumOf { it.chance.coerceAtLeast(0.0) }.takeIf { it > 0 } ?: 1.0
    val abilities = (Abilities.PICKED.filterNot { it is Signature } + Enrage).filter { it.enabled(catalog.abilities) }
    return Classes(classes.map { clazz ->
      val from = abilities.associate { it.key to it.threshold(clazz.abilityThresholds) } + ("signature" to clazz.abilityThresholds.signature)
      ClassInfo(clazz.key, plain(plugin.texts.className(code, clazz)), round(clazz.chance.coerceAtLeast(0.0) * 100 / total), clazz.health, clazz.damage, clazz.experience, clazz.defense, clazz.toughness, from)
    })
  }

  private fun bosses(call: Call): Bosses {
    val code = language(call.asker())
    val catalog = plugin.catalog
    val one = call[type]?.let(::bossType)
    val types = one?.let(::listOf) ?: catalog.types.values.filter { it.spec.enabled }.sortedBy { it.key }
    return Bosses(catalog.settings.spawn.enabled, types.map { type ->
      val signatures = catalog.signatureTypes.filter { (key, types) -> type.entity in types && catalog.abilities.signatures[key]?.enabled != false }.keys.sorted()
      BossInfo(type.entity.key.toString(), plain(plugin.texts.bossName(code, type)), round(type.spec.spawnChance * 100), round(type.spec.bonusLootChance * 100), catalog.minions[type.entity]?.entity?.key?.toString(), signatures)
    }, one?.let(::detail))
  }

  /** One boss type in full; its gear as each item and the lowest level that carries it, so 70 tiers of swords stay short. */
  private fun detail(type: Catalog.BossType): BossDetail {
    val weapons = type.weapons.groupBy { it.material.key.toString() }.mapValues { (_, tiers) -> tiers.minOf { it.minLevel } }
    val armor = type.armors.flatMap { set -> listOfNotNull(set.helmet, set.chestplate, set.leggings, set.boots).map { it.material.key.toString() to set.minLevel } }.groupBy({ it.first }, { it.second }).mapValues { (_, levels) -> levels.min() }
    return BossDetail(type.entity.key.toString(), type.spec.expModifier, type.spec.adultsOnly, type.spec.isPowered, WorldBosses.isWorldBoss(type.key), weapons, armor, plugin.catalog.loot.count { type.entity in it.types })
  }

  /** Reads the player and their world on the server thread, then answers once the database has. */
  private fun hunt(call: Call): CompletionStage<Hunt> {
    val player = call.player(Player::class.java)
    val settings = plugin.catalog.settings
    val curve = settings.progression.levelCurve
    val deathPenalty = settings.progression.deathPenalty
    val moon = plugin.fullMoon.isUp(player.world)
    val moonChance = if (moon) plugin.fullMoon.chance(player.world) else null
    return plugin.menuStats.card(player).thenCombine(plugin.progression.bestiary(player.uniqueId)) { card, bestiary ->
      val progress = card.progress
      val bonus = if (settings.survivalBonus.enabled) (HuntRules.bonus(settings.survivalBonus, card.survivalMinutes) * 100).roundToInt() else 0
      Hunt(card.level, curve.maxLevel, progress.experience, Leveling.experienceFor(curve, card.level + 1), progress.kills, progress.deaths, progress.streak, progress.damage, card.rank, card.nextBossInSeconds, card.survivalMinutes, deathPenalty, bestiary.map { Kills(it.type, it.clazz, it.kills, it.bestLevel) }, bonus, moon, moonChance)
    }
  }

  private fun nearby(call: Call): Nearby {
    val player = call.player(Player::class.java)
    val code = language(call.asker())
    val at = player.location
    val bosses = plugin.bosses.all.filter { it.entity.isValid && it.entity.world == at.world && it.entity.location.distanceSquared(at) <= NEARBY * NEARBY }.sortedBy { it.entity.location.distanceSquared(at) }.take(MAX_NEARBY)
    return Nearby(bosses.map { boss ->
      val where = boss.entity.location
      val maxHealth = boss.entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 0.0
      // Only the asker's own share: never another player's name or share.
      val share = Shares.of(boss.damage, plugin.groupHealth.whole(boss, maxHealth))[player.uniqueId] ?: 0.0
      val grown = boss.baseHealth?.takeIf { it > 0.0 }?.let { ((maxHealth / it - 1.0) * 100).coerceAtLeast(0.0) } ?: 0.0
      NearbyBoss(boss.type.entity.key.toString(), plain(plugin.texts.bossName(code, boss.type)), boss.clazz.key, boss.level, round(boss.entity.health), round(maxHealth), boss.enraged, boss.phaseTwo, where.blockX, where.blockY, where.blockZ, where.distance(at).roundToInt(), Abilities.reached(plugin.catalog, boss), round(share * 100), boss.damage.count { it.value > 0.0 }, round(grown))
    })
  }

  private fun loot(call: Call): Loot {
    val code = language(call.asker())
    val catalog = plugin.catalog
    val bossType = call[type]?.let(::bossType)
    val lootShare = round(catalog.settings.loot.minDamageShare * 100)
    val query = call[item] ?: return lootOf(call, bossType, lootShare, code)
    val names = LootGuide.named(catalog.configs.loot.items, catalog.classes).associate { named -> named.id to plugin.languages.codes.mapNotNull { name(named.id, it) } }
    val found = catalog.loot.filter { entry -> (bossType == null || entry.types.isEmpty() || bossType.entity in entry.types) && LootGuide.matches(query, entry.material.key.toString(), entry.spec.id?.let(names::get).orEmpty()) }
    val drops = found.map { entry ->
      // Its chance from a boss of its own class, at its lowest level, unless the question names others.
      val clazz = call[bossClass]?.name?.lowercase() ?: entry.spec.mobclass.lowercase()
      val at = call[level] ?: entry.spec.minLevel
      val owner = bossType ?: entry.types.firstOrNull()?.let(catalog.types::get)
      drop(entry, LootGuide.chances(pool(owner, clazz, at, own = false)).firstOrNull { it.first === entry }?.second, at, code)
    }.sortedByDescending { it.chancePercent ?: -1.0 }
    return Loot(call[bossClass]?.name?.lowercase(), call[level], bossType?.key, query, null, lootShare, drops.take(MAX_DROPS), (drops.size - MAX_DROPS).coerceAtLeast(0), null, emptyList())
  }

  /** What a boss of the asked class, level, and type drops: the likeliest items first, and its type's bonus roll. */
  private fun lootOf(call: Call, bossType: Catalog.BossType?, lootShare: Double, code: String): Loot {
    val clazz = (call[bossClass] ?: Rank.COMMON).name.lowercase()
    val at = (call[level] ?: plugin.progression.level(call.asker().uuid())).coerceAtLeast(0)
    val drops = LootGuide.chances(pool(bossType, clazz, at, own = false))
    val bonusChance = bossType?.spec?.bonusLootChance?.takeIf { it > 0.0 }
    val bonus = if (bonusChance == null) emptyList() else LootGuide.chances(pool(bossType, clazz, at, own = true))
    return Loot(clazz, at, bossType?.key, null, LootRules.items(plugin.catalog.settings.loot.items, at), lootShare, drops.take(MAX_DROPS).map { (entry, chance) -> drop(entry, chance, at, code) }, (drops.size - MAX_DROPS).coerceAtLeast(0), bonusChance?.let { round(it * 100) }, bonus.take(MAX_BONUS).map { (entry, chance) -> drop(entry, chance, at, code) })
  }

  /** What a boss of [type], or of none, with [clazz] and [level] picks from: the shared table and its type's own entries, or with [own] the latter alone. */
  private fun pool(type: Catalog.BossType?, clazz: String, level: Int, own: Boolean) =
    LootRules.pool(plugin.catalog.loot, { it.spec }, { entry -> if (entry.types.isEmpty()) !own else type != null && type.entity in entry.types }, plugin.catalog.classes, clazz, level)

  private fun drop(entry: Catalog.Loot, chance: Double?, level: Int, code: String): Drop {
    val spec = entry.spec
    val amounts = LootGuide.amounts(spec, plugin.catalog.configs.loot.settings, level, entry.material.maxStackSize)
    return Drop(entry.material.key.toString(), plugin.loot.name(spec, code)?.let(::plain), chance?.let(::round), LootGuide.text(amounts), spec.itemClass?.lowercase(), spec.mobclass.lowercase(), spec.minLevel, spec.maxLevel, entry.types.map { it.key.key }.sorted())
  }

  private fun abilities(call: Call): AbilitiesAnswer {
    val catalog = plugin.catalog
    val settings = catalog.abilities
    val classes = catalog.classes.all
    val known = (Abilities.PICKED + Enrage).associateBy { it.key }
    fun fromLevel(entry: AbilityGuide.Entry) = classes.map { clazz -> if (entry.signature) clazz.abilityThresholds.signature else known[entry.key]?.threshold(clazz.abilityThresholds) ?: settings.phaseTwo.minLevel }
    fun enabled(key: String) = known[key]?.enabled(settings) ?: settings.phaseTwo.enabled
    fun types(key: String) = catalog.signatureTypes[key].orEmpty().map { it.key.key }.sorted()
    val asked = call[ability]
    if (asked != null) {
      val entry = AbilityGuide.of(asked) ?: throw ToolException("no ability $asked: call abilities without ability for the keys")
      val numbers = entry.numbers(settings)
      val from = classes.map { it.key }.zip(fromLevel(entry)).toMap()
      val detail = AbilityDetail(entry.key, entry.signature, enabled(entry.key), entry.description, entry.tip, from, types(entry.key), numbers.cooldownSeconds, numbers.radius, numbers.radiusMax, numbers.damage, numbers.damageMax, numbers.healthPercent)
      return AbilitiesAnswer(classes.map { it.key }, emptyList(), emptyList(), emptyList(), emptyList(), detail)
    }
    val (signatures, shared) = AbilityGuide.ALL.partition { it.signature }
    val summaries = shared.filter { enabled(it.key) }.map { AbilitySummary(it.key, it.description, fromLevel(it)) }
    val disabled = AbilityGuide.ALL.filterNot { enabled(it.key) }.map { it.key }
    return AbilitiesAnswer(classes.map { it.key }, summaries, classes.map { it.abilityThresholds.signature }, signatures.filter { enabled(it.key) }.map { SignatureSummary(it.key, types(it.key)) }, disabled, null)
  }

  /** Reads the board off the server thread; Allay names each player as its rules allow and leaves out those the asker mustn't see. */
  private fun hunters(call: Call): CompletionStage<Hunters> {
    val curve = plugin.catalog.settings.progression.levelCurve
    return plugin.progression.board(call[limit]) { it.experience.toDouble() }.thenApply { (board, _) ->
      Hunters(board.mapIndexedNotNull { i, hunter ->
        val id = runCatching { UUID.fromString(hunter.uuid) }.getOrNull() ?: return@mapIndexedNotNull null
        HunterInfo(i + 1, PlayerRef(id, hunter.name.orEmpty()), Leveling.playerLevel(curve, hunter.progress.experience), hunter.progress.kills, hunter.progress.experience)
      })
    }
  }

  /** `/mobosses spawn` for an operator: the same checks, and the same [AdminSpawns] behind it. */
  private fun spawn(call: Call): Spawned {
    val player = call.player(Player::class.java)
    if (!player.hasPermission(Permissions.SPAWN)) throw ToolException("the asker may not spawn bosses")
    val catalog = plugin.catalog
    val bossType = bossType(call[type])
    val clazz = catalog.classes.byKey((call[bossClass] ?: Rank.COMMON).name) ?: throw ToolException("this server has no such class")
    val maxLevel = maxOf(100, catalog.settings.progression.levelCurve.maxLevel)
    val bossLevel = (call[level] ?: plugin.progression.level(player.uniqueId)).coerceIn(1, maxLevel)
    val at = ahead(player.location)
    val name = plain(plugin.texts.bossName(language(call.asker()), bossType))
    val spawned = ArrayList<SpawnedBoss>()
    repeat(call[count]) {
      when (val result = plugin.adminSpawns.spawn(bossType, clazz, bossLevel, at)) {
        is AdminSpawns.Result.Spawned -> spawned += SpawnedBoss(bossType.entity.key.toString(), name, clazz.key, bossLevel)
        is AdminSpawns.Result.Failed -> return Spawned(spawned, result.reason)
      }
    }
    return Spawned(spawned, null)
  }

  /** Two blocks ahead of [at], as the command spawns them; right there when looking straight up or down. */
  private fun ahead(at: Location): Location {
    val facing = at.direction.setY(0)
    return if (facing.lengthSquared() < 1e-6) at.clone() else at.clone().add(facing.normalize().multiply(2.0))
  }

  /** A boss type by its id, as the command reads it; the model reads why when there's none. */
  private fun bossType(id: String): Catalog.BossType = plugin.catalog.types.values.firstOrNull { it.key.equals(id.trim().removePrefix("minecraft:"), ignoreCase = true) }
    ?: throw ToolException("no boss type $id here: the bosses tool lists them")

  private fun itemInfo(named: LootGuide.Named): ItemInfo {
    val english = plugin.languages.codes.firstOrNull { it == "en" } ?: plugin.languages.default
    val lore = plugin.languages.text(english, "items.${named.id}.lore")?.split('\n')?.filter { it.isNotBlank() }?.joinToString(" ") { plain(plugin.languages.deserialize(it)) }
    val info = ItemInfo.of("mobosses:${named.id}").description(LootGuide.describe(named, lore)).base(named.material)
    plugin.languages.codes.forEach { code -> name(named.id, code)?.let { name -> LootGuide.locales(code).forEach { info.name(it, name) } } }
    return info
  }

  /** A named drop's name in a language, plain. */
  private fun name(id: String, code: String): String? = plugin.languages.text(code, "items.$id.name")?.let { plain(plugin.languages.deserialize(it)) }?.takeIf { it.isNotBlank() }

  /** The asker's language among Mo’Bosses' own: `pt_br`, else `pt`, else the server's default. */
  private fun language(asker: Asker): String {
    val full = LocaleResolver.normalize(asker.language())
    return listOf(full, full.substringBefore('_')).firstOrNull { it in plugin.languages.codes } ?: plugin.languages.default
  }

  private fun plain(name: Component) = PlainTextComponentSerializer.plainText().serialize(name)

  private fun round(value: Double) = (value * 100).roundToInt() / 100.0

  /** What Allay takes as an item's path. */
  private val ID = Regex("[a-z0-9_./-]+")

  private val PROMPT = """
    Mo’Bosses turns ordinary mobs into bosses. A boss has a level and one of six classes: Common, Uncommon, Rare, Epic, Legendary, Mythic. The rarer the class, the more health, damage, and armor. Its name tag and boss bar show both. A boss casts abilities it has reached the level for in its class: lightning, minions, fire and web rings, meteors, arrow volleys, and its mob type's signature move, among others. It drops loot of its class and the classes below.
    Players have a Mo’Bosses level of their own, apart from vanilla experience, from killing bosses. A natural boss's level follows the levels of the players near it. Experience, kills, and loot go by each player's share of the damage. Loot and rewards need a big enough share (about 28% alone by default, less in a group). A kill gives little or doesn't count when the share was small, the boss was far below the player's level, or it came from a spawner farmed in one spot, which gives less each time and recovers with time. On a full-moon night bosses come more often, rarer classes more still, and the survival bonus, time played without dying, brings more bosses too.
    Commands: /mobosses opens the menu (the player's card, the best hunters, the bestiary, the language), /mobosses info [player], /mobosses top, /mobosses language <code|auto>, /mobosses reset. Operators place bosses with /mobosses spawn <type> <level> <class>.
    This server's classes, bosses, loot, and numbers differ from the defaults: read them with the tools instead of guessing. For a fight, read the boss with the nearby tool and its abilities with the abilities tool, and advise on its class, level, and abilities.
  """.trimIndent()
}
