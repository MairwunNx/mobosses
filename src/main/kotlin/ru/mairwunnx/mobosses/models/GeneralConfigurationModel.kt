@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import ru.mairwunnx.mobosses.serializers.SoundSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer

@Serializable class GeneralConfigurationModel(
  @SerialName("enabled") val enabled: Boolean,
  @SerialName("debug") val debug: Boolean,
  @SerialName("language") val language: String,
  @SerialName("spawn") val spawn: SpawnConfig,
  @SerialName("progression") val progression: ProgressionConfig,
  @SerialName("formula") val formula: FormulaConfig,
  @SerialName("effects") val effects: EffectsConfig,
  @SerialName("boss_bar") val bossBar: BossBarConfig,
  @SerialName("loot") val loot: LootConfig,
  @SerialName("database") val database: DatabaseConfig,
) {
  @Serializable class SpawnConfig(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("max_bosses_per_world") val maxBossesPerWorld: Int,
    @SerialName("spawn_radius") val spawnRadius: Int,
    @SerialName("world_multipliers") val worldMultipliers: Map<String, Double>,
    @SerialName("time_multipliers") val timeMultipliers: TimeMultipliers,
    @SerialName("weather_multipliers") val weatherMultipliers: WeatherMultipliers,
    @SerialName("check_vertical_zone") val checkVerticalZone: Boolean,
    @SerialName("vertical_zone_threshold") val verticalZoneThreshold: Int,
    @SerialName("allowed_spawn_reasons") val allowedSpawnReasons: List<String>
  )

  @Serializable class TimeMultipliers(
    @SerialName("day") val day: Double,
    @SerialName("night") val night: Double
  )

  @Serializable class WeatherMultipliers(
    @SerialName("clear") val clear: Double,
    @SerialName("rain") val rain: Double,
    @SerialName("thunder") val thunder: Double
  )

  @Serializable class ProgressionConfig(
    @SerialName("level_curve") val levelCurve: LevelCurve,
    @SerialName("exp_gain") val expGain: ExpGain,
    @SerialName("variance") val variance: Variance
  )

  @Serializable class LevelCurve(
    @SerialName("max_level") val maxLevel: Int,
    @SerialName("exp_scale") val expScale: Double,
    @SerialName("kill_scale") val killScale: Double,
    @SerialName("power") val power: Double,
    @SerialName("offset") val offset: Double
  )

  @Serializable class ExpGain(
    @SerialName("health_factor") val healthFactor: Double,
    @SerialName("level_factor") val levelFactor: Double,
    @SerialName("level_power") val levelPower: Double,
    @SerialName("rarity_mul") val rarityMul: Boolean
  )

  @Serializable class Variance(
    @SerialName("base_chance") val baseChance: Double,
    @SerialName("extra_chance") val extraChance: Double,
    @SerialName("min_reduction") val minReduction: Int,
    @SerialName("max_reduction") val maxReduction: Int,
    @SerialName("power") val power: Double
  )

  @Serializable class FormulaConfig(
    @SerialName("health") val health: FormulaSection,
    @SerialName("damage") val damage: FormulaSection,
    @SerialName("experience") val experience: FormulaSection,
    @SerialName("minions") val minions: MinionsFormula,
    @SerialName("armor") val armor: ArmorFormula,
    @SerialName("toughness") val toughness: ToughnessFormula,
  )

  @Serializable class FormulaSection(
    @SerialName("max_scale") val maxScale: Double,
    @SerialName("power") val power: Double,
    @SerialName("offset") val offset: Double,
    @SerialName("flatten") val flatten: Double? = null
  )

  @Serializable class MinionsFormula(
    @SerialName("base_count") val base: Int,
    @SerialName("max_count") val max: Int,
    @SerialName("power") val power: Double,
    @SerialName("offset") val offset: Double,
    @SerialName("variance") val variance: Double,
  )

  @Serializable class ArmorFormula(
    @SerialName("max_scale") val maxScale: Double,
    @SerialName("power") val power: Double,
    @SerialName("offset") val offset: Double,
    @SerialName("flatten") val flatten: Double? = null,
    @SerialName("cap_points") val capPoints: Int
  )

  @Serializable class ToughnessFormula(
    @SerialName("max_scale") val maxScale: Double,
    @SerialName("power") val power: Double,
    @SerialName("offset") val offset: Double,
    @SerialName("flatten") val flatten: Double? = null,
    @SerialName("cap_points") val capPoints: Int
  )

  @Serializable class EffectsConfig(
    @SerialName("particles") val particles: ParticlesConfig,
    @SerialName("glow") val glow: GlowConfig,
    @SerialName("visibility_radius") val visibilityRadius: Int
  )

  @Serializable class ParticlesConfig(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("update_interval") val updateInterval: Int
  )

  @Serializable class GlowConfig(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("classes") val classes: List<String>
  )

  @Serializable class BossBarConfig(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("style") val style: String
  )

  @Serializable class LootConfig(
    @SerialName("random_durability") val randomDurability: RandomDurabilityConfig
  )

  @Serializable class RandomDurabilityConfig(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("min_durability") val minDurability: Double,
    @SerialName("max_durability") val maxDurability: Double
  )

  @Serializable class DatabaseConfig(
    @SerialName("type") val type: String,
    @SerialName("sqlite") val sqlite: SqliteConfig,
    @SerialName("mysql") val mysql: MySqlConfig,
    @SerialName("postgres") val postgres: PostgresConfig
  )

  @Serializable class SqliteConfig(
    @SerialName("file") val file: String
  )

  @Serializable class MySqlConfig(
    @SerialName("host") val host: String,
    @SerialName("port") val port: Int,
    @SerialName("database") val database: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String
  )

  @Serializable class PostgresConfig(
    @SerialName("host") val host: String,
    @SerialName("port") val port: Int,
    @SerialName("database") val database: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("ssl") val ssl: Boolean
  )

  companion object {
    fun default() = GeneralConfigurationModel(
      enabled = true,
      debug = false,
      language = "ru",
      spawn = SpawnConfig(
        enabled = true,
        maxBossesPerWorld = 10000000,
        spawnRadius = 100,
        worldMultipliers = mapOf(
          "world" to 1.0,
          "world_nether" to 1.5,
          "world_the_end" to 2.0
        ),
        timeMultipliers = TimeMultipliers(
          day = 0.7,
          night = 1.1
        ),
        weatherMultipliers = WeatherMultipliers(
          clear = 1.0,
          rain = 1.2,
          thunder = 1.5
        ),
        checkVerticalZone = false,
        verticalZoneThreshold = 14,
        allowedSpawnReasons = listOf(
          "NATURAL", "JOCKEY", "SPAWNER", "TRIAL_SPAWNER", "EGG", "SPAWNER_EGG",
          "VILLAGE_INVASION", "NETHER_PORTAL", "DROWNED", "RAID", "PATROL", "COMMAND", "CUSTOM"
        )
      ),
      progression = ProgressionConfig(
        levelCurve = LevelCurve(
          maxLevel = 100,
          expScale = 30.0,
          killScale = 20.0,
          power = 1.3,
          offset = 20.0
        ),
        expGain = ExpGain(
          healthFactor = 0.01,
          levelFactor = 0.005,
          levelPower = 2.0,
          rarityMul = true
        ),
        variance = Variance(
          baseChance = 0.10,
          extraChance = 0.35,
          minReduction = 5,
          maxReduction = 80,
          power = 1.2
        )
      ),
      formula = FormulaConfig(
        health = FormulaSection(
          maxScale = 20.0,
          power = 1.8,
          offset = 400.0,
          flatten = 0.35
        ),
        damage = FormulaSection(
          maxScale = 6.0,
          power = 1.2,
          offset = 600.0,
          flatten = 0.40
        ),
        experience = FormulaSection(
          maxScale = 270.0,
          power = 2.0,
          offset = 1000.0,
          flatten = 0.0
        ),
        minions = MinionsFormula(
          base = 2,
          max = 12,
          power = 1.2,
          offset = 80.0,
          variance = 0.0,
        ),
        armor = ArmorFormula(
          maxScale = 2.0,
          power = 1.2,
          offset = 180.0,
          flatten = 0.35,
          capPoints = 28,
        ),
        toughness = ToughnessFormula(
          maxScale = 2.2,
          power = 1.1,
          offset = 200.0,
          flatten = 0.30,
          capPoints = 12,
        )
      ),
      effects = EffectsConfig(
        particles = ParticlesConfig(
          enabled = true,
          updateInterval = 20
        ),
        glow = GlowConfig(
          enabled = true,
          classes = listOf("uncommon", "rare", "epic", "legendary", "mythic")
        ),
        visibilityRadius = 32
      ),
      bossBar = BossBarConfig(
        enabled = true,
        style = "SEGMENTED_20"
      ),
      loot = LootConfig(
        randomDurability = RandomDurabilityConfig(
          enabled = true,
          minDurability = 0.1,
          maxDurability = 0.9
        )
      ),
      database = DatabaseConfig(
        type = "sqlite",
        sqlite = SqliteConfig(
          file = "bosses.db"
        ),
        mysql = MySqlConfig(
          host = "localhost",
          port = 3306,
          database = "mobosses",
          username = "root",
          password = "password"
        ),
        postgres = PostgresConfig(
          host = "localhost",
          port = 5432,
          database = "mobosses",
          username = "mobosses",
          password = "secret",
          ssl = false
        )
      ),
    )
  }
}