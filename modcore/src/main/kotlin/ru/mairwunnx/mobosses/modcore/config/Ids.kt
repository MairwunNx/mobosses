package ru.mairwunnx.mobosses.modcore.config

import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import ru.mairwunnx.mobosses.Log

/**
 * Game ids from the config files, resolved against the server's registries, other mods' entries and data packs'
 * enchantments included. Each takes the namespaced form (`minecraft:entity.zombie.death`, `alexsmobs:grizzly_bear`) or
 * 1.x's enum names (`ENTITY_ZOMBIE_DEATH`), and a bad one logs a warning with where it was and resolves to null, as
 * Paper's `Ids` does.
 */
class Ids(private val server: MinecraftServer, private val log: Log) {
  fun entity(id: String, where: String): EntityType<*>? = entry(BuiltInRegistries.ENTITY_TYPE, id, where)?.value()

  /** Whether a mob of [type] may become a boss: a living one the game can spawn. */
  fun living(type: EntityType<*>) = type.canSummon() && DefaultAttributes.hasSupplier(type)

  fun item(id: String, where: String): Item? =
    entry(BuiltInRegistries.ITEM, id, where, quiet = true)?.value()?.takeIf { it != Items.AIR } ?: null.also { log.warn("$where: unknown item \"$id\"") }

  fun enchantment(id: String, where: String): Holder<Enchantment>? =
    entry(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT), ENCHANTMENT_ALIASES[normalize(id)] ?: id, where)

  fun effect(id: String, where: String): Holder<MobEffect>? = entry(BuiltInRegistries.MOB_EFFECT, id, where)

  fun sound(id: String, where: String): Holder<SoundEvent>? = if (id.isBlank()) null else entry(BuiltInRegistries.SOUND_EVENT, id, where)

  /** Only particles that need no extra data, since bosses spawn them plain. */
  fun particle(id: String, where: String): ParticleOptions? {
    val type = entry(BuiltInRegistries.PARTICLE_TYPE, id, where)?.value() ?: return null
    return type as? SimpleParticleType ?: null.also { log.warn("$where: $id needs extra data; pick another particle") }
  }

  private fun <T : Any> entry(lookup: HolderLookup.RegistryLookup<T>, id: String, where: String, quiet: Boolean = false): Holder.Reference<T>? {
    val text = normalize(id)
    val key = Identifier.tryParse(text)
    val found = lookup.listElements().filter { it.key().identifier() == key }.findFirst().orElse(null)
      // 1.x's enum names: entity.zombie.death was ENTITY_ZOMBIE_DEATH.
      ?: lookup.listElements().filter { it.key().identifier().namespace == Identifier.DEFAULT_NAMESPACE && it.key().identifier().path.replace('.', '_') == text }.findFirst().orElse(null)
    return found ?: null.also { if (!quiet) log.warn("$where: unknown id \"$id\"") }
  }

  private companion object {
    fun normalize(id: String) = id.trim().lowercase()

    /** Names 1.x configs used that the game calls otherwise. */
    val ENCHANTMENT_ALIASES = mapOf("sweeping" to "sweeping_edge")
  }
}
