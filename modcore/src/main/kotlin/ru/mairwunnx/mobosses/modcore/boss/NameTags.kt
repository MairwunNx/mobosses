package ru.mairwunnx.mobosses.modcore.boss

import com.mojang.math.Transformation
import io.netty.buffer.Unpooled
import net.kyori.adventure.text.Component
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.mixin.DisplayAccess
import java.util.UUID

/**
 * Names over bosses and minions, each player reading them in their own language. The game shows one name to everyone,
 * so each name is a text display riding the mob, one per language among the players near it, as on Paper. Here the
 * displays live only in their viewers' games, sent by packet: nothing goes into the world, so nothing is saved, shed,
 * or left behind after a crash.
 */
class NameTags(private val mod: MoBosses) {
  private class Label(val display: Display.TextDisplay, val viewers: MutableSet<UUID> = HashSet())

  private val labels = HashMap<UUID, MutableMap<String, Label>>()

  /** Twice a second. */
  fun tick() {
    if (!mod.catalog.settings.nameTags.perPlayer) return clearAll()
    val radius = mod.catalog.settings.nameTags.radius.toDouble()
    val named = mod.bosses.all.map { it.entity } + mod.minions.all + mod.decoys.all
    val ids = named.mapTo(HashSet()) { it.uuid }
    labels.keys.filter { it !in ids }.forEach { id -> labels.remove(id)?.values?.forEach(::remove) }

    named.forEach { entity ->
      if (!entity.isAlive) return@forEach clear(entity)
      val viewers = mod.players(entity, radius).groupBy(mod.texts::languageOf)
      val current = labels.getOrPut(entity.uuid) { HashMap() }
      current.keys.filter { it !in viewers }.forEach { current.remove(it)?.let(::remove) }
      viewers.forEach { (code, players) ->
        val label = current[code] ?: create(entity, code)?.also { current[code] = it } ?: return@forEach
        show(entity, label, players)
      }
    }
  }

  /** Drops an entity's labels; the next tick makes new ones if it still needs them, with fresh text. */
  fun clear(entity: Entity) {
    labels.remove(entity.uuid)?.values?.forEach(::remove)
  }

  fun clearAll() {
    labels.values.flatMap { it.values }.forEach(::remove)
    labels.clear()
  }

  private fun show(entity: LivingEntity, label: Label, players: List<ServerPlayer>) {
    val ids = players.mapTo(HashSet()) { it.uuid }
    label.viewers.filter { it !in ids }.forEach { id -> mod.server.playerList.getPlayer(id)?.connection?.send(ClientboundRemoveEntitiesPacket(label.display.id)) }
    val display = label.display
    players.forEach { player ->
      if (player.uuid !in label.viewers) {
        display.setPos(entity.position())
        player.connection.send(ClientboundAddEntityPacket(display.id, display.uuid, entity.x, entity.y, entity.z, 0f, 0f, EntityTypes.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0))
        display.entityData.nonDefaultValues?.let { player.connection.send(ClientboundSetEntityDataPacket(display.id, it)) }
      }
      // A mob's riders change, as a jockey's do, and the game then tells its viewers without the label; it climbs back on.
      player.connection.send(riders(entity, display))
    }
    label.viewers.clear()
    label.viewers.addAll(ids)
  }

  private fun remove(label: Label) {
    val packet = ClientboundRemoveEntitiesPacket(label.display.id)
    label.viewers.forEach { mod.server.playerList.getPlayer(it)?.connection?.send(packet) }
    label.viewers.clear()
  }

  private fun create(entity: LivingEntity, code: String): Label? {
    val text = text(entity, code) ?: return null
    // Never added to the world: it takes an id of its own, and its data makes the packets.
    val display = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, entity.level())
    (display as DisplayAccess.Text).`mobosses$text`(mod.texts.vanilla(text))
    (display as DisplayAccess).`mobosses$billboard`(Display.BillboardConstraints.CENTER)
    display.`mobosses$transformation`(Transformation(Vector3f(0f, LIFT, 0f), Quaternionf(), Vector3f(1f, 1f, 1f), Quaternionf()))
    return Label(display)
  }

  private fun riders(entity: Entity, display: Entity): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(entity.id)
    buffer.writeVarIntArray((entity.passengers.map { it.id } + display.id).toIntArray())
    return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
  }

  private fun text(entity: LivingEntity, code: String): Component? {
    // A boss's copy wears the boss's name, so nothing tells them apart.
    (mod.bosses.get(entity) ?: mod.decoys.bossOf(entity))?.let { return mod.languages.render(code, Message.BOSS_TAG, *mod.texts.boss(code, it)) }
    val type = mod.minions.typeOf(entity) ?: return null
    val minion = mod.catalog.minions.values.firstOrNull { Catalog.keyOf(it.summoner) == type }
    return if (minion != null) mod.texts.minionName(code, minion) else mod.languages.render(code, "minions.$type")
  }

  private companion object {
    /** How far above the mob's head the name floats, as the game's own name tag does. */
    const val LIFT = 0.25f
  }
}
