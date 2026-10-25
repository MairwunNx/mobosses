package ru.mairwunnx.mobosses.boss

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.lang.Message
import java.util.UUID

/**
 * Names over bosses and minions, each player reading them in their own language. The game shows one
 * name to everyone, so each name is a text display riding the mob, one per language among the
 * players near it, hidden from everyone but the players of its language. The displays aren't saved:
 * they come back on the next tick after a restart or a chunk load.
 */
class NameTags(private val plugin: MoBossesPlugin) {
  private class Label(val display: TextDisplay, val viewers: MutableSet<UUID> = HashSet())

  private val labels = HashMap<UUID, MutableMap<String, Label>>()

  /** Twice a second. */
  fun tick() {
    if (!plugin.catalog.settings.nameTags.perPlayer) return clearAll()
    val radius = plugin.catalog.settings.nameTags.radius.toDouble()
    val named = plugin.bosses.all.map { it.entity } + plugin.minions.all + plugin.decoys.all
    labels.keys.toList().filter { id -> named.none { it.uniqueId == id } }.forEach { id -> labels.remove(id)?.values?.forEach { it.display.remove() } }

    named.forEach { entity ->
      if (!entity.isValid) return@forEach clear(entity)
      val viewers = entity.world.getNearbyPlayers(entity.location, radius).groupBy(plugin.texts::languageOf)
      val current = labels.getOrPut(entity.uniqueId) { HashMap() }
      current.keys.filter { it !in viewers }.forEach { current.remove(it)?.display?.remove() }
      viewers.forEach { (code, players) ->
        val label = current[code]?.takeIf { it.display.isValid } ?: spawn(entity, code)?.also { current[code] = it } ?: return@forEach
        // A mob can shed its riders, as an enderman teleporting may; the label climbs back on.
        if (label.display.vehicle != entity) entity.addPassenger(label.display)
        show(label, players)
      }
    }
  }

  /** Drops an entity's labels; the next tick makes new ones if it still needs them, with fresh text. */
  fun clear(entity: Entity) {
    labels.remove(entity.uniqueId)?.values?.forEach { it.display.remove() }
  }

  fun clearAll() {
    labels.values.flatMap { it.values }.forEach { it.display.remove() }
    labels.clear()
  }

  /** A label left over from a crash, which the game saved before the plugin could remove it. */
  fun isStray(entity: Entity) = entity is TextDisplay && entity.persistentDataContainer.has(plugin.keys.label, PersistentDataType.BOOLEAN)

  private fun show(label: Label, players: List<Player>) {
    val ids = players.mapTo(HashSet()) { it.uniqueId }
    label.viewers.filter { it !in ids }.forEach { id -> Bukkit.getPlayer(id)?.hideEntity(plugin, label.display) }
    players.filter { it.uniqueId !in label.viewers }.forEach { it.showEntity(plugin, label.display) }
    label.viewers.clear()
    label.viewers.addAll(ids)
  }

  private fun spawn(entity: LivingEntity, code: String): Label? {
    val text = text(entity, code) ?: return null
    val display = entity.world.spawn(entity.location, TextDisplay::class.java) {
      it.text(text)
      it.billboard = Display.Billboard.CENTER
      it.isPersistent = false
      it.isVisibleByDefault = false
      it.transformation = Transformation(Vector3f(0f, LIFT, 0f), AxisAngle4f(), Vector3f(1f, 1f, 1f), AxisAngle4f())
      it.persistentDataContainer.set(plugin.keys.label, PersistentDataType.BOOLEAN, true)
    }
    entity.addPassenger(display)
    return Label(display)
  }

  private fun text(entity: LivingEntity, code: String): Component? {
    // A boss's copy wears the boss's name, so nothing tells them apart.
    (plugin.bosses.get(entity) ?: plugin.decoys.bossOf(entity))?.let { return plugin.languages.render(code, Message.BOSS_TAG, *plugin.texts.boss(code, it)) }
    val type = plugin.minions.typeOf(entity) ?: return null
    val minion = plugin.catalog.minions.values.firstOrNull { it.summoner.key.key == type }
    return if (minion != null) plugin.texts.minionName(code, minion) else plugin.languages.render(code, "minions.$type")
  }

  private companion object {
    /** How far above the mob's head the name floats, as the game's own name tag does. */
    const val LIFT = 0.25f
  }
}
