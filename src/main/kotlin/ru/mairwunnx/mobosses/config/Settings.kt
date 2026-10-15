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
  val progression: Progression = Progression(),
  val formula: Formulas = Formulas(),
  val effects: Effects = Effects(),
  @SerialName("boss_bar") val bossBar: BossBar = BossBar(),
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
  )

  @Serializable
  data class LevelCurve(
    @SerialName("max_level") val maxLevel: Int = 100,
    @SerialName("exp_scale") val expScale: Double = 30.0,
    @SerialName("kill_scale") val killScale: Double = 20.0,
    val power: Double = 1.3,
    val offset: Double = 20.0,
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

  @Serializable
  data class Variance(
    @SerialName("base_chance") val baseChance: Double = 0.15,
    @SerialName("extra_chance") val extraChance: Double = 0.45,
    @SerialName("min_reduction") val minReduction: Int = 5,
    @SerialName("max_reduction") val maxReduction: Int = 80,
    val power: Double = 1.2,
  )

  @Serializable
  data class Formulas(
    val health: Curve = Curve(maxScale = 20.0, power = 1.8, offset = 400.0, flatten = 0.35),
    val damage: Curve = Curve(maxScale = 6.0, power = 1.2, offset = 600.0, flatten = 0.40),
    val experience: Curve = Curve(maxScale = 270.0, power = 2.0, offset = 1000.0),
    val armor: CappedCurve = CappedCurve(maxScale = 2.0, power = 1.2, offset = 180.0, flatten = 0.35, capPoints = 28),
    val toughness: CappedCurve = CappedCurve(maxScale = 2.2, power = 1.1, offset = 200.0, flatten = 0.30, capPoints = 12),
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
  )

  @Serializable
  data class CappedCurve(
    @SerialName("max_scale") val maxScale: Double = 1.0,
    val power: Double = 1.0,
    val offset: Double = 1.0,
    val flatten: Double? = null,
    @SerialName("cap_points") val capPoints: Int = 30,
  ) {
    val curve get() = Curve(maxScale, power, offset, flatten)
  }

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
  data class Loot(
    @SerialName("random_durability") val randomDurability: RandomDurability = RandomDurability(),
    /** The share of a boss's health a player must deal to get loot. */
    @SerialName("min_damage_share") val minDamageShare: Double = 0.27777777,
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
    /** “abilities.*” to the player who hit the boss when it uses an ability. */
    val abilities: Boolean = true,
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
