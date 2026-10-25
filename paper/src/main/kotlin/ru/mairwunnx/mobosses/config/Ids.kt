package ru.mairwunnx.mobosses.config

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.mobosses.Log

/**
 * Game ids from the config files, resolved against the server's registries. Each takes the
 * namespaced form (`minecraft:entity.zombie.death`) or 1.x's enum names (`ENTITY_ZOMBIE_DEATH`), and
 * a bad one logs a warning with where it was and resolves to null.
 */
class Ids(private val log: Log) {
  fun entity(id: String, where: String): EntityType? = entry(RegistryKey.ENTITY_TYPE, id, where)

  fun material(id: String, where: String): Material? =
    Material.matchMaterial(id.trim())?.takeIf { it.isItem } ?: null.also { log.warn("$where: unknown item \"$id\"") }

  fun enchantment(id: String, where: String): Enchantment? = entry(RegistryKey.ENCHANTMENT, ENCHANTMENT_ALIASES[normalize(id)] ?: id, where)

  fun effect(id: String, where: String): PotionEffectType? = entry(RegistryKey.MOB_EFFECT, id, where)

  fun sound(id: String, where: String): Sound? = if (id.isBlank()) null else entry(RegistryKey.SOUND_EVENT, id, where)

  /** Only particles that need no extra data, since bosses spawn them plain. */
  fun particle(id: String, where: String): Particle? = entry(RegistryKey.PARTICLE_TYPE, id, where)?.takeIf { particle ->
    (particle.dataType == Void::class.java).also { if (!it) log.warn("$where: $id needs extra data; pick another particle") }
  }

  private fun <T : Keyed> entry(key: RegistryKey<T>, id: String, where: String): T? {
    val registry = RegistryAccess.registryAccess().getRegistry(key)
    val text = normalize(id)
    val found = NamespacedKey.fromString(text)?.let(registry::get)
      // 1.x's enum names: entity.zombie.death was ENTITY_ZOMBIE_DEATH.
      ?: registry.firstOrNull { legacyName(it.key) == text }
    return found ?: null.also { log.warn("$where: unknown id \"$id\"") }
  }

  companion object {
    fun normalize(id: String) = id.trim().lowercase()

    private fun legacyName(key: NamespacedKey) = key.key.replace('.', '_')

    /** Names 1.x configs used that the game calls otherwise. */
    private val ENCHANTMENT_ALIASES = mapOf("sweeping" to "sweeping_edge")
  }
}
