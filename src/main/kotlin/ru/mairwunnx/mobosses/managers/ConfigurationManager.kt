package ru.mairwunnx.mobosses.managers

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import ru.mairwunnx.mobosses.models.AbilitiesConfigurationModel
import ru.mairwunnx.mobosses.models.BossesConfigurationModel
import ru.mairwunnx.mobosses.models.ClassesConfigurationModel
import ru.mairwunnx.mobosses.models.GeneralConfigurationModel
import ru.mairwunnx.mobosses.models.LootTableConfigurationModel
import ru.mairwunnx.mobosses.models.MessagesConfigurationModel
import ru.mairwunnx.mobosses.models.MinionsConfigurationModel
import ru.mairwunnx.mobosses.models.RewardsConfigurationModel
import ru.mairwunnx.mobosses.serializers.ChatColorSerializer
import ru.mairwunnx.mobosses.serializers.EnchantmentSerializer
import ru.mairwunnx.mobosses.serializers.EntityTypeSerializer
import ru.mairwunnx.mobosses.serializers.MaterialSerializer
import ru.mairwunnx.mobosses.serializers.ParticleSerializer
import ru.mairwunnx.mobosses.serializers.PotionEffectTypeSerializer
import ru.mairwunnx.mobosses.serializers.SoundSerializer
import ru.mobosses.PluginUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ConfigurationManager(val plugin: PluginUnit) {
  private val serializersModule = SerializersModule {
    contextual(org.bukkit.Particle::class, ParticleSerializer)
    contextual(org.bukkit.Sound::class, SoundSerializer)
    contextual(org.bukkit.entity.EntityType::class, EntityTypeSerializer)
    contextual(org.bukkit.ChatColor::class, ChatColorSerializer)
    contextual(org.bukkit.Material::class, MaterialSerializer)
    contextual(org.bukkit.enchantments.Enchantment::class, EnchantmentSerializer)
    contextual(org.bukkit.potion.PotionEffectType::class, PotionEffectTypeSerializer)
  }

  private val yaml = Yaml(serializersModule = serializersModule, configuration = YamlConfiguration(encodeDefaults = true, allowAnchorsAndAliases = true, strictMode = false))

  private val files = listOf<ConfigurationFile<*>>(
    ConfigurationFile("abilities.yml", AbilitiesConfigurationModel.serializer(), AbilitiesConfigurationModel.default(), AbilitiesConfigurationModel::class),
    ConfigurationFile("bosses.yml", BossesConfigurationModel.serializer(), BossesConfigurationModel.default(), BossesConfigurationModel::class),
    ConfigurationFile("classes.yml", ClassesConfigurationModel.serializer(), ClassesConfigurationModel.default(), ClassesConfigurationModel::class),
    ConfigurationFile("config.yml", GeneralConfigurationModel.serializer(), GeneralConfigurationModel.default(), GeneralConfigurationModel::class),
    ConfigurationFile("loottable.yml", LootTableConfigurationModel.serializer(), LootTableConfigurationModel.default(), LootTableConfigurationModel::class),
    ConfigurationFile("messages.yml", MessagesConfigurationModel.serializer(), MessagesConfigurationModel.default(), MessagesConfigurationModel::class),
    ConfigurationFile("minions.yml", MinionsConfigurationModel.serializer(), MinionsConfigurationModel.default(), MinionsConfigurationModel::class),
    ConfigurationFile("rewards.yml", RewardsConfigurationModel.serializer(), RewardsConfigurationModel.default(), RewardsConfigurationModel::class),
  )

  private val configurations = ConcurrentHashMap<KClass<*>, Any>()

  private inner class ConfigurationFile<T : Any>(val path: String, val type: KSerializer<T>, val default: T, val kClass: KClass<T>) {
    suspend fun resolve(): T = withContext(IO) {
      plugin.logger.info { "🔄 Resolving configuration file: $path" }

      val result = runCatching {
        val resolvedFile = plugin.dataFolder.resolve(path)
        if (!resolvedFile.exists()) {
          plugin.logger.warn { "$path configuration file not found, creating new one" }
          plugin.saveResource(path, false)
        }
        val text = resolvedFile.readText(Charsets.UTF_8)
        yaml.decodeFromString(type, text)
      }.onFailure {
        plugin.logger.error({ "Can't load or read $path configuration file! Using default one." }, it)
      }.getOrDefault(default)

      plugin.logger.info { "✅ $path configuration file loaded" }
      result
    }
  }

  suspend fun initialize() = supervisorScope {
    plugin.logger.info { "🔄 Loading configuration files" }
    val loaded = files.map { async(IO) { it.kClass to it.resolve() } }.awaitAll()
    loaded.forEach { (k, v) -> configurations[k] = v }
    plugin.logger.info { "✅ Configuration files loaded successfully" }
  }

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(type: Class<T>): T =
    configurations[type.kotlin] as? T
      ?: run {
        plugin.logger.error({ "❌ No configuration ${type.simpleName} resolved at this moment!" }, NullPointerException())
        throw IllegalStateException("Configuration ${type.simpleName} not loaded")
      }

  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(type: KClass<T>): T =
    configurations[type] as? T
      ?: run {
        plugin.logger.error({ "❌ No configuration ${type.simpleName} resolved at this moment!" }, NullPointerException())
        throw IllegalStateException("Configuration ${type.simpleName} not loaded")
      }
}
