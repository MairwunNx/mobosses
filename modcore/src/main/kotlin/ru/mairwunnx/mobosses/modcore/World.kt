package ru.mairwunnx.mobosses.modcore

import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.random.Random

/** What Bukkit's `World` and `Player` offer in one call and the game spreads over its classes. */
object World {
  /** The players within [radius] of [at] in [level], spectators left out, as Paper's `getNearbyPlayers`. */
  fun players(level: ServerLevel, at: Vec3, radius: Double): List<ServerPlayer> =
    level.players().filter { !it.isSpectator && it.position().distanceToSqr(at) <= radius * radius }

  fun players(entity: Entity, radius: Double): List<ServerPlayer> = (entity.level() as? ServerLevel)?.let { players(it, entity.position(), radius) }.orEmpty()

  fun holder(sound: SoundEvent): Holder<SoundEvent> = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound)

  /** A sound only [player] hears, as Bukkit's `Player.playSound`. */
  fun sound(player: ServerPlayer, sound: Holder<SoundEvent>, at: Vec3 = player.position(), volume: Float = 1f, pitch: Float = 1f) =
    player.connection.send(ClientboundSoundPacket(sound, SoundSource.HOSTILE, at.x, at.y, at.z, volume, pitch, Random.nextLong()))

  fun sound(player: ServerPlayer, sound: SoundEvent, at: Vec3 = player.position(), volume: Float = 1f, pitch: Float = 1f) = sound(player, holder(sound), at, volume, pitch)

  /** A sound everyone near [at] hears, as Bukkit's `World.playSound`. */
  fun sound(level: ServerLevel, at: Vec3, sound: Holder<SoundEvent>, volume: Float = 1f, pitch: Float = 1f) =
    level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch)

  fun sound(level: ServerLevel, at: Vec3, sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) = sound(level, at, holder(sound), volume, pitch)

  fun particles(level: ServerLevel, particle: ParticleOptions, at: Vec3, count: Int, dx: Double, dy: Double, dz: Double, speed: Double) {
    level.sendParticles(particle, at.x, at.y, at.z, count, dx, dy, dz, speed)
  }
}
