package ru.mairwunnx.mobosses.modcore.data

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.component.CustomData
import ru.mairwunnx.mobosses.modcore.mixin.CustomDataAccess

/**
 * Mo’Bosses' data on an entity, as Paper keeps it in the entity's persistent data container: a compound under
 * `mobosses` in the entity's own custom data, which the game saves with it and `/data` shows. It outlives chunk
 * unloads and restarts; a player's outlives death too ([ru.mairwunnx.mobosses.modcore.mixin.RespawnMixin]).
 */
class Data(private val entity: Entity) {
  /** The compound as it is now, read without a copy; never change it. */
  private val tag: CompoundTag?
    get() = entity.get(DataComponents.CUSTOM_DATA)?.let { (it as CustomDataAccess).`mobosses$tag`().getCompound(ROOT).orElse(null) }

  fun has(key: String) = tag?.contains(key) == true

  fun string(key: String): String? = tag?.getString(key)?.orElse(null)

  fun int(key: String): Int? = tag?.getInt(key)?.orElse(null)

  fun long(key: String): Long? = tag?.getLong(key)?.orElse(null)

  fun double(key: String): Double? = tag?.getDouble(key)?.orElse(null)

  /** Booleans are bytes, as everywhere in the game's data. */
  fun boolean(key: String): Boolean? = tag?.getBoolean(key)?.orElse(null)

  operator fun set(key: String, value: String) = edit { it.putString(key, value) }

  operator fun set(key: String, value: Int) = edit { it.putInt(key, value) }

  operator fun set(key: String, value: Long) = edit { it.putLong(key, value) }

  operator fun set(key: String, value: Double) = edit { it.putDouble(key, value) }

  operator fun set(key: String, value: Boolean) = edit { it.putBoolean(key, value) }

  fun remove(key: String) {
    if (has(key)) edit { it.remove(key) }
  }

  /** Everything Mo’Bosses keeps on this entity, for a player who respawns as a new one. */
  fun copyTo(other: Entity) {
    val all = tag?.copy() ?: return
    Data(other).edit { mine -> all.keySet().forEach { key -> all.get(key)?.let { mine.put(key, it) } } }
  }

  private fun edit(change: (CompoundTag) -> Unit) {
    val data = entity.get(DataComponents.CUSTOM_DATA) ?: CustomData.EMPTY
    entity.setComponent(DataComponents.CUSTOM_DATA, data.update { root ->
      val mine = root.getCompound(ROOT).orElseGet(::CompoundTag)
      change(mine)
      if (mine.isEmpty) root.remove(ROOT) else root.put(ROOT, mine)
    })
  }

  companion object {
    const val ROOT = "mobosses"
  }
}

val Entity.data: Data get() = Data(this)
