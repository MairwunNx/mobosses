@file:UseSerializers(SoundSerializer::class, ParticleSerializer::class, EntityTypeSerializer::class, ChatColorSerializer::class, MaterialSerializer::class, EnchantmentSerializer::class, PotionEffectTypeSerializer::class)

package ru.mairwunnx.mobosses.models

import kotlinx.serialization.Contextual
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
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment

@Serializable class BossesConfigurationModel(
  @SerialName("ZOMBIE") val zombie: BossConfig,
  @SerialName("SKELETON") val skeleton: BossConfig,
  @SerialName("PIGLIN") val piglin: BossConfig,
  @SerialName("PILLAGER") val pillager: BossConfig,
  @SerialName("SPIDER") val spider: BossConfig,
  @SerialName("ENDERMAN") val enderman: BossConfig,
  @SerialName("WITHER_SKELETON") val witherSkeleton: BossConfig,
  @SerialName("VINDICATOR") val vindicator: BossConfig,
  @SerialName("STRAY") val stray: BossConfig,
  @SerialName("CREEPER") val creeper: BossConfig,
  @SerialName("HUSK") val husk: BossConfig,
  @SerialName("WITCH") val witch: BossConfig,
  @SerialName("DROWNED") val drowned: BossConfig,
  @SerialName("EVOKER") val evoker: BossConfig
) {
  @Serializable class BossConfig(
    @SerialName("name") val name: String,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("spawn_chance") val spawnChance: Double,
    @SerialName("exp_modifier") val expModifier: Double = 1.0,
    @SerialName("is_powered") val isPowered: Boolean? = null,
    @SerialName("equipment") val equipment: Equipment? = null,
    @SerialName("effects") val effects: Effects,
  )

  @Serializable class Equipment(
    @SerialName("weapons") val weapons: List<Weapon>? = null,
    @SerialName("armors") val armors: List<Armor>? = null
  )

  @Serializable class Weapon(
    @SerialName("material") val material: @Contextual Material,
    @SerialName("min_lvl") val minLvl: Int,
    @SerialName("enchantments") val enchantments: Map<@Contextual Enchantment, Int>? = null
  )

  @Serializable class Armor(
    @SerialName("min_lvl") val minLvl: Int,
    @SerialName("helmet") val helmet: ArmorPiece? = null,
    @SerialName("chestplate") val chestplate: ArmorPiece? = null,
    @SerialName("leggings") val leggings: ArmorPiece? = null,
    @SerialName("boots") val boots: ArmorPiece? = null
  )

  @Serializable class ArmorPiece(
    @SerialName("material") val material: Material,
    @SerialName("enchantments") val enchantments: Map<@Contextual Enchantment, Int>? = null
  )

  @Serializable class Effects(
    @SerialName("particles") val particles: @Contextual Particle,
    @SerialName("sounds") val sounds: @Contextual Sounds
  )

  @Serializable class Sounds(
    @SerialName("spawn") val spawn: @Contextual Sound,
    @SerialName("death") val death: @Contextual Sound,
    @SerialName("summon") val summon: @Contextual Sound
  )

  companion object {
    fun default() = BossesConfigurationModel(
      zombie = BossConfig(
        name = "Зомбак",
        enabled = true,
        spawnChance = 0.05,
        effects = Effects(
          particles = Particle.FLAME,
          sounds = Sounds(Sound.ENTITY_ZOMBIE_AMBIENT, Sound.ENTITY_ZOMBIE_DEATH, Sound.ENTITY_ZOMBIE_VILLAGER_CURE)
        )
      ),
      skeleton = BossConfig(
        name = "Костян",
        enabled = true,
        spawnChance = 0.04,
        expModifier = 0.7,
        effects = Effects(
          particles = Particle.CRIT,
          sounds = Sounds(Sound.ENTITY_SKELETON_AMBIENT, Sound.ENTITY_SKELETON_DEATH, Sound.ENTITY_STRAY_AMBIENT)
        )
      ),
      piglin = BossConfig(
        name = "Свиноголовый",
        enabled = true,
        spawnChance = 0.03,
        expModifier = 0.4,
        effects = Effects(
          particles = Particle.LAVA,
          sounds = Sounds(Sound.ENTITY_PIGLIN_AMBIENT, Sound.ENTITY_PIGLIN_DEATH, Sound.ENTITY_PIGLIN_BRUTE_AMBIENT)
        )
      ),
      pillager = BossConfig(
        name = "Пилладжерон",
        enabled = true,
        spawnChance = 0.04,
        expModifier = 0.7,
        effects = Effects(
          particles = Particle.ANGRY_VILLAGER,
          sounds = Sounds(Sound.ENTITY_PILLAGER_AMBIENT, Sound.ENTITY_PILLAGER_DEATH, Sound.ENTITY_VINDICATOR_AMBIENT)
        )
      ),
      spider = BossConfig(
        name = "Паукан",
        enabled = true,
        spawnChance = 0.06,
        expModifier = 0.377,
        effects = Effects(
          particles = Particle.SPORE_BLOSSOM_AIR,
          sounds = Sounds(Sound.ENTITY_SPIDER_AMBIENT, Sound.ENTITY_SPIDER_DEATH, Sound.AMBIENT_CAVE)
        )
      ),
      enderman = BossConfig(
        name = "Шпала",
        enabled = true,
        spawnChance = 0.08,
        expModifier = 0.4,
        effects = Effects(
          particles = Particle.PORTAL,
          sounds = Sounds(Sound.ENTITY_ENDERMAN_AMBIENT, Sound.ENTITY_ENDERMAN_DEATH, Sound.ENTITY_ENDERMAN_TELEPORT)
        )
      ),
      witherSkeleton = BossConfig(
        name = "Босс иссушитель",
        enabled = true,
        spawnChance = 0.05,
        expModifier = 0.688,
        effects = Effects(
          particles = Particle.LARGE_SMOKE,
          sounds = Sounds(Sound.ENTITY_WITHER_SKELETON_AMBIENT, Sound.ENTITY_WITHER_SKELETON_DEATH, Sound.ENTITY_WITHER_SPAWN)
        )
      ),
      vindicator = BossConfig(
        name = "Херобрин",
        enabled = true,
        spawnChance = 0.25,
        effects = Effects(
          particles = Particle.ANGRY_VILLAGER,
          sounds = Sounds(Sound.ENTITY_VINDICATOR_AMBIENT, Sound.ENTITY_VINDICATOR_DEATH, Sound.ENTITY_EVOKER_FANGS_ATTACK)
        )
      ),
      stray = BossConfig(
        name = "Отмороженный",
        enabled = true,
        spawnChance = 0.1,
        expModifier = 0.733,
        effects = Effects(
          particles = Particle.SNOWFLAKE,
          sounds = Sounds(Sound.ENTITY_STRAY_AMBIENT, Sound.ENTITY_STRAY_DEATH, Sound.BLOCK_SNOW_BREAK)
        )
      ),
      creeper = BossConfig(
        name = "Рико",
        enabled = true,
        spawnChance = 0.04,
        expModifier = 0.666,
        isPowered = true,
        effects = Effects(
          particles = Particle.ELECTRIC_SPARK,
          sounds = Sounds(Sound.ENTITY_CREEPER_PRIMED, Sound.ENTITY_CREEPER_DEATH, Sound.ENTITY_CREEPER_PRIMED)
        )
      ),
      husk = BossConfig(
        name = "Пустынник",
        enabled = true,
        spawnChance = 0.045,
        expModifier = 0.9,
        effects = Effects(
          particles = Particle.ANGRY_VILLAGER,
          sounds = Sounds(Sound.ENTITY_HUSK_AMBIENT, Sound.ENTITY_HUSK_DEATH, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE)
        )
      ),
      witch = BossConfig(
        name = "Ведьмачка",
        enabled = true,
        spawnChance = 0.08,
        expModifier = 0.377,
        effects = Effects(
          particles = Particle.WITCH,
          sounds = Sounds(Sound.ENTITY_WITCH_AMBIENT, Sound.ENTITY_WITCH_DEATH, Sound.ENTITY_WITCH_DRINK)
        )
      ),
      drowned = BossConfig(
        name = "Утонувший",
        enabled = true,
        spawnChance = 0.06,
        effects = Effects(
          particles = Particle.BUBBLE_POP,
          sounds = Sounds(Sound.ENTITY_DROWNED_AMBIENT, Sound.ENTITY_DROWNED_DEATH, Sound.ENTITY_DROWNED_SHOOT)
        )
      ),
      evoker = BossConfig(
        name = "Джаггернаут",
        enabled = true,
        spawnChance = 0.08,
        expModifier = 0.7,
        effects = Effects(
          particles = Particle.ENCHANT,
          sounds = Sounds(Sound.ENTITY_EVOKER_AMBIENT, Sound.ENTITY_EVOKER_DEATH, Sound.ENTITY_EVOKER_PREPARE_SUMMON)
        )
      ),
    )
  }
}