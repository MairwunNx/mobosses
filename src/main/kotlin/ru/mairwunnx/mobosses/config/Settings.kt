package ru.mairwunnx.mobosses.config

import com.charleskorn.kaml.YamlMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `config.yml`. Every field has a default, so a config from 1.x, or one that sets only a few keys,
 * still loads. Game ids stay strings here and resolve against the server's registries on load.
 */
@Serializable
data class Settings(
  val enabled: Boolean = true,
  val debug: Boolean = false,
  /** 1.x's single language; [Locales.default] replaces it. Read only when `locales.default` isn't set. */
  val language: String? = null,
  val locales: Locales = Locales(),
  val geoip: GeoIpSettings = GeoIpSettings(),
  val spawn: Spawn = Spawn(),
  val despawn: Despawn = Despawn(),
  @SerialName("survival_bonus") val survivalBonus: SurvivalBonus = SurvivalBonus(),
  @SerialName("full_moon") val fullMoon: FullMoon = FullMoon(),
  val achievements: Achievements = Achievements(),
  @SerialName("group_health") val groupHealth: GroupHealth = GroupHealth(),
  @SerialName("spawner_fatigue") val spawnerFatigue: SpawnerFatigue = SpawnerFatigue(),
  val progression: Progression = Progression(),
  val formula: Formulas = Formulas(),
  val effects: Effects = Effects(),
  @SerialName("boss_bar") val bossBar: BossBar = BossBar(),
  @SerialName("name_tags") val nameTags: NameTags = NameTags(),
  val loot: Loot = Loot(),
  val broadcast: Broadcast = Broadcast(),
  @SerialName("kill_sound") val killSound: KillSound = KillSound(),
  val database: Database = Database(),
  val protection: Protection = Protection(),
  val coreprotect: CoreProtect = CoreProtect(),
) {
  @Serializable
  data class Locales(
    /** The language for players nothing else matches, for the console, and for the name tags over bosses. */
    val default: String = "en",
    /** Where a player's language comes from, tried in order: `ip` (country by address), `client` (game language). */
    val sources: List<Source> = listOf(Source.IP, Source.CLIENT),
    /** Language → the ISO 3166 country codes that speak it. Countries not listed fall through to the next source. */
    val countries: Map<String, List<String>> = DEFAULT_COUNTRIES,
  )

  @Serializable
  enum class Source {
    @SerialName("ip") IP,
    @SerialName("client") CLIENT,
  }

  @Serializable
  data class GeoIpSettings(
    val enabled: Boolean = true,
    /** A MaxMind-format country database, plain or gzipped. `{year}` and `{month}` fill in the current month. */
    val url: String = "https://download.db-ip.com/free/dbip-country-lite-{year}-{month}.mmdb.gz",
    @SerialName("refresh_days") val refreshDays: Int = 30,
  )

  /**
   * Time in the game without dying raises the chance of bosses near a player, as Sacred's survival
   * bonus did: half of [maxBonus] after [halfAtMinutes], nearing all of it later. Minutes idle for
   * [idleMinutes] or more don't count, so standing AFK earns nothing.
   */
  @Serializable
  data class SurvivalBonus(
    val enabled: Boolean = true,
    @SerialName("max_bonus") val maxBonus: Double = 1.0,
    @SerialName("half_at_minutes") val halfAtMinutes: Int = 180,
    @SerialName("idle_minutes") val idleMinutes: Int = 3,
  )

  /**
   * The game's advancements, in a tab of their own. The game shows every player the same advancement
   * text, so it comes in one [language]; empty means `locales.default`.
   */
  @Serializable
  data class Achievements(
    val enabled: Boolean = true,
    val language: String = "",
  )

  /** Nights of the hunt: under a full moon, more bosses, and rarer classes more often. Other phases change nothing. */
  @Serializable
  data class FullMoon(
    val enabled: Boolean = true,
    /** Multiplies the spawn chance. */
    val chance: Double = 1.5,
    /** Multiplies the weight of the rarest class; the ones between get a share of it by rank, Common none. */
    @SerialName("rarer_classes") val rarerClasses: Double = 2.0,
    /** Tells the players in the world as the full moon rises. */
    val announce: Boolean = true,
  )

  /**
   * A boss grows as more players truly fight it: each one who has dealt [fighterShare] of its health
   * adds [perPlayer] of its starting health, up to [maxPlayers]. Someone standing by changes nothing,
   * and it never shrinks back.
   */
  @Serializable
  data class GroupHealth(
    val enabled: Boolean = true,
    @SerialName("per_player") val perPlayer: Double = 0.35,
    @SerialName("max_players") val maxPlayers: Int = 6,
    @SerialName("fighter_share") val fighterShare: Double = 0.05,
  )

  /**
   * Bosses from spawners give less to a player who keeps killing them in one spot: each such kill
   * within [radius] counts, fading by half every [halfLifeMinutes]; past [free] of them, every one
   * more takes [step] off the reward, down to [floor]. It comes back with time.
   */
  @Serializable
  data class SpawnerFatigue(
    val enabled: Boolean = true,
    val radius: Int = 32,
    @SerialName("half_life_minutes") val halfLifeMinutes: Int = 20,
    val free: Double = 3.0,
    val step: Double = 0.25,
    val floor: Double = 0.2,
  )

  /** Bosses nobody comes near for a while leave the world, as vanilla mobs do, instead of piling up in caves for good. */
  @Serializable
  data class Despawn(
    val enabled: Boolean = true,
    /** How long a boss may go without a player within [radius] before it leaves. */
    @SerialName("after_minutes") val afterMinutes: Int = 10,
    val radius: Int = 64,
    /** Bosses from `/mobosses spawn` stay until killed or removed. */
    @SerialName("keep_spawned_by_command") val keepSpawnedByCommand: Boolean = true,
  )

  @Serializable
  data class Spawn(
    val enabled: Boolean = true,
    @SerialName("max_bosses_per_world") val maxBossesPerWorld: Int = 10_000_000,
    /** How far from a mob the nearest player may be for the mob to become a boss. Their level sets the boss's. */
    @SerialName("spawn_radius") val spawnRadius: Int = 100,
    @SerialName("world_multipliers") val worldMultipliers: Map<String, Double> = mapOf("world" to 1.0, "world_nether" to 1.1, "world_the_end" to 1.3),
    @SerialName("time_multipliers") val timeMultipliers: TimeMultipliers = TimeMultipliers(),
    @SerialName("weather_multipliers") val weatherMultipliers: WeatherMultipliers = WeatherMultipliers(),
    @SerialName("check_vertical_zone") val checkVerticalZone: Boolean = false,
    @SerialName("vertical_zone_threshold") val verticalZoneThreshold: Int = 14,
    @SerialName("allowed_spawn_reasons") val allowedSpawnReasons: List<String> = listOf(
      "NATURAL", "JOCKEY", "SPAWNER", "TRIAL_SPAWNER", "EGG", "SPAWNER_EGG", "VILLAGE_INVASION",
      "NETHER_PORTAL", "DROWNED", "RAID", "PATROL", "COMMAND", "CUSTOM",
    ),
    /** The players this close to the mob set its level together, by their average. */
    @SerialName("group_radius") val groupRadius: Int = 24,
    /** After a boss spawns for a player, the next one waits this long, stretched by up to the bias. */
    @SerialName("player_cooldown_seconds") val playerCooldownSeconds: Int = 60,
    @SerialName("player_cooldown_bias") val playerCooldownBias: Double = 1.3,
    /** No new boss while this many are already within nearby_radius. */
    @SerialName("max_nearby") val maxNearby: Int = 3,
    @SerialName("nearby_radius") val nearbyRadius: Int = 48,
  )

  @Serializable
  data class TimeMultipliers(val day: Double = 0.7, val night: Double = 1.0)

  @Serializable
  data class WeatherMultipliers(val clear: Double = 1.0, val rain: Double = 1.1, val thunder: Double = 1.3)

  @Serializable
  data class Progression(
    @SerialName("level_curve") val levelCurve: LevelCurve = LevelCurve(),
    @SerialName("mini_bosses_level_curve") val miniBossesLevelCurve: MiniBossesCurve = MiniBossesCurve(),
    @SerialName("exp_gain") val expGain: ExpGain = ExpGain(),
    val variance: Variance = Variance(),
    val relevance: Relevance = Relevance(),
    /** The share of a boss's health a player must deal for the kill to count toward their level. */
    @SerialName("min_share") val minShare: Double = 0.15,
    /** In a group of N players, no share threshold (kills, loot, rewards) asks for more than this over N. */
    @SerialName("group_share") val groupShare: Double = 0.6,
    @SerialName("pet_damage") val petDamage: PetDamage = PetDamage(),
    /** Dying to a boss burns the progress toward the next level; the level itself stays. */
    @SerialName("death_penalty") val deathPenalty: Boolean = true,
  )

  /**
   * What the damage of a player's pets is worth in the experience of a kill, against their own: almost
   * all of it at low levels, down to [minWeight] at the top. For the share, loot, and kills it counts whole.
   */
  @Serializable
  data class PetDamage(
    @SerialName("min_weight") val minWeight: Double = 0.5,
    val power: Double = 1.5,
    val offset: Double = 300.0,
  )

  @Serializable
  data class LevelCurve(
    @SerialName("max_level") val maxLevel: Int = 100,
    @SerialName("exp_scale") val expScale: Double = 30.0,
    @SerialName("kill_scale") val killScale: Double = 20.0,
    val power: Double = 1.6,
    val offset: Double = 60.0,
  )

  @Serializable
  data class MiniBossesCurve(
    @SerialName("min_delta") val minDelta: Int = 1,
    @SerialName("max_delta") val maxDelta: Int = 18,
    val power: Double = 1.15,
    val offset: Double = 90.0,
    @SerialName("percent_cap") val percentCap: Double = 0.28,
    val jitter: Int = 1,
  )

  @Serializable
  data class ExpGain(
    @SerialName("health_factor") val healthFactor: Double = 0.01,
    @SerialName("level_factor") val levelFactor: Double = 0.005,
    @SerialName("level_power") val levelPower: Double = 2.0,
    @SerialName("rarity_mul") val rarityMul: Boolean = true,
  )

  /** Now and then a boss comes weaker: by a share of the level, more often and by more at high levels. */
  @Serializable
  data class Variance(
    @SerialName("base_chance") val baseChance: Double = 0.15,
    @SerialName("extra_chance") val extraChance: Double = 0.35,
    @SerialName("min_cut") val minCut: Double = 0.05,
    @SerialName("max_cut") val maxCut: Double = 0.30,
    val power: Double = 1.2,
  )

  /**
   * Bosses below a player's level give them less, so farming weak ones doesn't level anyone up;
   * stronger ones give a little more. It compares levels by ratio, `(boss + offset) / (player +
   * offset)`, so a few levels mean little to newcomers and a lot to no one.
   */
  @Serializable
  data class Relevance(
    val offset: Double = 15.0,
    /** Below 1, the ratio to this power is the share of experience a kill brings. */
    val power: Double = 1.5,
    val floor: Double = 0.1,
    val bonus: Double = 1.5,
    /** The lowest ratio at which a kill still counts toward the player's level. */
    @SerialName("kill_ratio") val killRatio: Double = 0.65,
  )

  @Serializable
  data class Formulas(
    val health: Curve = Curve(maxScale = 32.0, power = 1.8, offset = 1000.0, flatten = 0.35, basePoints = 20.0),
    val damage: Curve = Curve(maxScale = 6.0, power = 1.3, offset = 180.0, flatten = 0.40),
    /** The share of the damage growth that bosses' own arrows, fireballs, and tridents get. */
    @SerialName("ranged_damage") val rangedDamage: Double = 0.6,
    val explosion: Explosion = Explosion(),
    /**
     * The vanilla experience a boss drops, over its mob's own (at least `base_points`): little more
     * than the mob at first, past the Ender Dragon's 500 once a boss outlasts the dragon, and up to
     * 20,000 for a level 100 Mythic, more than the dragon's first kill.
     */
    val experience: Curve = Curve(maxScale = 600.0, power = 3.0, offset = 200_000.0, basePoints = 5.0),
    val armor: CappedCurve = CappedCurve(maxScale = 2.0, power = 1.2, offset = 180.0, flatten = 0.35, capPoints = 28, basePoints = 2.0),
    val toughness: CappedCurve = CappedCurve(maxScale = 2.2, power = 1.1, offset = 200.0, flatten = 0.30, capPoints = 12, basePoints = 2.0),
    val minions: MinionsCurve = MinionsCurve(),
  )

  /** A growth curve by level: 1 at level 0, approaching [maxScale]. */
  @Serializable
  data class Curve(
    @SerialName("max_scale") val maxScale: Double = 1.0,
    val power: Double = 1.0,
    val offset: Double = 1.0,
    /** How much the class multipliers shrink toward 1 at high levels, 0 to 1. Unset means 0.3. */
    val flatten: Double? = null,
    /** The starting value for mobs that have less of their own, so a silverfish boss isn't a joke. */
    @SerialName("base_points") val basePoints: Double = 0.0,
  )

  @Serializable
  data class CappedCurve(
    @SerialName("max_scale") val maxScale: Double = 1.0,
    val power: Double = 1.0,
    val offset: Double = 1.0,
    val flatten: Double? = null,
    @SerialName("cap_points") val capPoints: Int = 30,
    /** The starting value for mobs that have less of their own: most mobs have no toughness, and skeletons no armor. */
    @SerialName("base_points") val basePoints: Double = 0.0,
  ) {
    val curve get() = Curve(maxScale, power, offset, flatten)
  }

  /**
   * A creeper boss's blast radius: the game's, 3 or 6 when charged, plus up to [maxExtra] blocks,
   * growing by level like the other curves. Added, not multiplied, so a level 100 creeper outblasts a
   * charged one of the game's, and a charged boss doesn't end up with a crater of 16.
   */
  @Serializable
  data class Explosion(
    @SerialName("max_extra") val maxExtra: Double = 6.0,
    val power: Double = 1.2,
    val offset: Double = 60.0,
  )

  @Serializable
  data class MinionsCurve(
    @SerialName("base_count") val base: Int = 2,
    @SerialName("max_count") val max: Int = 12,
    val power: Double = 1.2,
    val offset: Double = 80.0,
    val variance: Double = 0.15,
  )

  @Serializable
  data class Effects(
    val particles: Particles = Particles(),
    val glow: Glow = Glow(),
    /** Glow and particles show only while a player is this close to the boss. */
    @SerialName("visibility_radius") val visibilityRadius: Int = 32,
  )

  @Serializable
  data class Particles(
    val enabled: Boolean = true,
    @SerialName("update_interval") val updateInterval: Int = 20,
  )

  @Serializable
  data class Glow(
    val enabled: Boolean = true,
    /** Classes that glow; an empty list means all of them. */
    val classes: List<String> = listOf("uncommon", "rare", "epic", "legendary", "mythic"),
  )

  @Serializable
  data class BossBar(
    val enabled: Boolean = true,
    /** PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, or NOTCHED_20; 1.x's SOLID and SEGMENTED_* work too. */
    val style: String = "NOTCHED_20",
    /** A player farther than this from the boss, or in another world, stops seeing its bar. */
    @SerialName("hide_distance") val hideDistance: Int = 64,
  )

  @Serializable
  data class NameTags(
    /** Names over bosses and minions in each viewer's language; off shows the default language to everyone. */
    @SerialName("per_player") val perPlayer: Boolean = true,
    /** How close a player must be to read a name. */
    val radius: Int = 32,
  )

  @Serializable
  data class Loot(
    @SerialName("random_durability") val randomDurability: RandomDurability = RandomDurability(),
    /** The share of a boss's health a player must deal to get loot. */
    @SerialName("min_damage_share") val minDamageShare: Double = 0.27777777,
    val pity: Pity = Pity(),
    /** How much the survival bonus tips loot toward rare and better items: their weights grow by bonus × this × their tier. */
    @SerialName("survival_luck") val survivalLuck: Double = 1.0,
  )

  /**
   * A player whose loot brought nothing rare or better for a while gets a rare item at least: after a
   * number of drops rolled anew each time between [minDrops] and [maxDrops], so nobody sees a pattern.
   */
  @Serializable
  data class Pity(
    val enabled: Boolean = true,
    @SerialName("min_drops") val minDrops: Int = 8,
    @SerialName("max_drops") val maxDrops: Int = 12,
  )

  @Serializable
  data class RandomDurability(
    val enabled: Boolean = true,
    @SerialName("min_durability") val minDurability: Double = 0.1,
    @SerialName("max_durability") val maxDurability: Double = 0.9,
  )

  @Serializable
  data class Broadcast(
    /** “kill” to everyone when a boss dies to players. */
    val kills: Boolean = true,
    /** “loot.rare” and up to everyone when a player gets loot of that class. */
    @SerialName("rare_loot") val rareLoot: Boolean = true,
    /** “abilities.*” to the players fighting the boss when it uses an ability. */
    val abilities: Boolean = true,
    /** Where ability warnings show: `action_bar`, over the hotbar, or `chat`. */
    @SerialName("abilities_in") val abilitiesIn: String = "action_bar",
    /** “boss_nearby” to the players near a new boss of this class or rarer; `none` turns it off. */
    @SerialName("spawns_from") val spawnsFrom: String = "epic",
    @SerialName("spawns_radius") val spawnsRadius: Int = 64,
    /** “kill_notes.*” to each player after a kill: why they got less, or more, than the whole of it. */
    @SerialName("kill_notes") val killNotes: Boolean = true,
  )

  @Serializable
  data class KillSound(
    val enabled: Boolean = true,
    val key: String = "minecraft:ui.toast.challenge_complete",
  )

  @Serializable
  data class Database(
    /** sqlite, mysql (MySQL or MariaDB), or postgres. */
    val type: String = "sqlite",
    val sqlite: Sqlite = Sqlite(),
    val mysql: Remote = Remote(port = 3306, username = "root", password = "password"),
    val postgres: Remote = Remote(port = 5432, username = "mobosses", password = "secret"),
  )

  @Serializable
  data class Sqlite(val file: String = "bosses.db")

  @Serializable
  data class Remote(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "mobosses",
    val username: String = "",
    val password: String = "",
    val ssl: Boolean = false,
  )

  @Serializable
  data class Protection(
    val griefprevention: GriefPrevention = GriefPrevention(),
    val worldguard: WorldGuard = WorldGuard(),
    /** Whether `/mobosses spawn` ignores protected areas. */
    @SerialName("admin_spawn_bypass") val adminSpawnBypass: Boolean = true,
  )

  @Serializable
  data class GriefPrevention(
    val enabled: Boolean = true,
    /** No boss spawns naturally inside a claim. */
    @SerialName("no_bosses_in_claims") val noBossesInClaims: Boolean = true,
    /** No ability breaks, places, or ignites blocks inside a claim. */
    @SerialName("no_block_abilities_in_claims") val noBlockAbilitiesInClaims: Boolean = true,
  )

  @Serializable
  data class WorldGuard(
    val enabled: Boolean = true,
    /** Bosses spawn only where `mobosses-spawn` and `mob-spawning` allow it. */
    @SerialName("spawn_flags") val spawnFlags: Boolean = true,
    /** No boss spawns in a region with owners or members, a player's plot, unless `mobosses-spawn` allows it there. */
    @SerialName("no_bosses_in_owned_regions") val noBossesInOwnedRegions: Boolean = true,
    /** Abilities change blocks only where `mobosses-abilities` allows it and `build` isn't denied. */
    @SerialName("ability_flags") val abilityFlags: Boolean = true,
    /** Abilities hurt players only where `mob-damage` allows it. */
    @SerialName("damage_flags") val damageFlags: Boolean = true,
  )

  @Serializable
  data class CoreProtect(
    /** Logs blocks bosses and minions change as `#mobosses`, so `/co rollback u:#mobosses` undoes them. */
    val enabled: Boolean = true,
    val user: String = "#mobosses",
  )

  /** The language picked when nothing else matches: `locales.default`, or 1.x's `language` when the file still has only that. */
  fun defaultLanguage(raw: String): String {
    val root = runCatching { Yamls.parse(raw) as? YamlMap }.getOrNull()
    val locales = root?.entries?.entries?.firstOrNull { it.key.content == "locales" }?.value as? YamlMap
    val explicit = locales?.entries?.keys?.any { it.content == "default" } == true
    return if (explicit || language == null) this.locales.default else language
  }

  companion object {
    fun parse(text: String, warn: (String) -> Unit = {}): Settings = Yamls.decode(serializer(), text, warn)

    val DEFAULT_COUNTRIES = mapOf(
      "ru" to listOf("RU", "BY", "KZ", "KG", "TJ", "UZ", "AM", "AZ", "MD", "GE", "TM"),
      "es" to listOf(
        "ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN",
        "PY", "SV", "NI", "CR", "PA", "UY", "PR", "GQ",
      ),
      "pt" to listOf("BR", "PT", "AO", "MZ", "CV", "GW", "ST", "TL"),
      "de" to listOf("DE", "AT", "CH", "LI", "LU"),
    )
  }
}
