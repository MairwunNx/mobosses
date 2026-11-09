package ru.mairwunnx.mobosses.modcore.hooks

import com.mairwunnx.worldapi.expansion.v2.Permission
import com.mairwunnx.worldapi.expansion.v2.Place
import com.mairwunnx.worldapi.expansion.v2.Request
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.boss.AdminSpawns
import ru.mairwunnx.mobosses.modcore.boss.Boss
import java.util.UUID

/**
 * The namespace for WorldAPI 4.6 and newer, as on Paper: the resources and events, and actions that spawn a boss where a
 * client says and remove one by its id. They need `world:write`, run on the server thread, and land in WorldAPI's audit
 * log. Spawns go through [AdminSpawns] with `/mobosses spawn`'s checks, removals as `/mobosses killall` takes bosses: no
 * loot, no kill. A world is a dimension, as WorldAPI names it on the mods: `overworld`, or another mod's as
 * `namespace:path`; Paper's `world`, `world_nether`, and `world_the_end` work too.
 */
object BossesActions : BossesData() {
  /** The six classes, by their keys. */
  enum class Rank { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }

  val type = text("type", "A boss type's id, like zombie or minecraft:wither_skeleton").required()
  val rank = choice("class", "The boss class, Common to Mythic", Rank::class.java, Rank.COMMON)
  val level = integer("level", "The boss level, from 1 to the top level the server's curve allows", 1, 1000, 1)
  val count = integer("count", "How many bosses, from 1 to 5", 1, 5, 1)
  val at = place("at", "Where to spawn them: a loaded world (overworld, the_nether, the_end, or a mod’s namespace:path) and coordinates").required()
  val boss = text("boss", "A live boss's id: its uuid in bosses")

  val spawn = action("bosses/spawn", "Spawns bosses at a place, as /mobosses spawn does and under its rules: they stay until killed, and protection applies unless admin_spawn_bypass is on. An unknown type, a level past the server's top, or a world that isn't loaded answers 400", ::spawn).body(type, rank, level, count, at).permission(Permission.WORLD_WRITE).onServerThread().returns(Spawned::class.java)
  val kill = remove("bosses/{boss}", "Removes a live boss as /mobosses killall does: no loot, no experience, no kill for anyone. An id no boss alive has answers 404", ::kill).permission(Permission.WORLD_WRITE).onServerThread().returns(SpawnedBoss::class.java)

  @JvmRecord data class Spawned(val bosses: List<SpawnedBoss>, val failed: String?)

  @JvmRecord data class SpawnedBoss(val uuid: UUID, val type: String, val `class`: String, val level: Int, val location: Place)

  private fun spawn(req: Request): Spawned {
    val catalog = mod.catalog
    val wanted = req[type].trim().removePrefix("minecraft:")
    val bossType = catalog.types.values.firstOrNull { it.key.equals(wanted, ignoreCase = true) } ?: throw IllegalArgumentException("no boss type $wanted here: ${catalog.types.values.map { it.key }.sorted().joinToString()}")
    val clazz = catalog.classes.byKey(req[rank].name) ?: throw IllegalArgumentException("this server has no class ${req[rank].name.lowercase()}")
    val maxLevel = maxOf(100, catalog.settings.progression.levelCurve.maxLevel)
    val bossLevel = req[level].takeIf { it in 1..maxLevel } ?: throw IllegalArgumentException("level goes from 1 to $maxLevel")
    val place = req[at]
    val world = world(place.world()) ?: throw IllegalArgumentException("no world ${place.world()} is loaded")
    val where = Vec3(place.x(), place.y(), place.z())
    val spawned = ArrayList<SpawnedBoss>()
    repeat(req[count]) {
      when (val result = mod.adminSpawns.spawn(bossType, clazz, bossLevel, world, where)) {
        is AdminSpawns.Result.Spawned -> spawned += spawned(result.boss)
        is AdminSpawns.Result.Failed -> if (spawned.isEmpty()) throw IllegalArgumentException("the boss didn't spawn: ${result.reason}") else return Spawned(spawned, result.reason)
      }
    }
    return Spawned(spawned, null)
  }

  private fun kill(req: Request): SpawnedBoss {
    val id = runCatching { UUID.fromString(req[boss].trim()) }.getOrNull()
    val found = mod.bosses.all.firstOrNull { it.uuid == id && it.entity.isAlive } ?: throw NoSuchElementException("no live boss has the id ${req[boss]}")
    val gone = spawned(found)
    mod.bosses.remove(found)
    return gone
  }

  private fun spawned(boss: Boss) = SpawnedBoss(boss.uuid, boss.type.key, boss.clazz.key, boss.level, place(boss.entity))

  /** A world by WorldAPI's name for it on the mods, or by Paper's. */
  private fun world(name: String): ServerLevel? {
    val id = when (val plain = name.trim().lowercase()) {
      "world" -> "minecraft:overworld"
      "world_nether" -> "minecraft:the_nether"
      "world_the_end" -> "minecraft:the_end"
      else -> if (':' in plain) plain else "minecraft:$plain"
    }
    return Identifier.tryParse(id)?.let { mod.server.getLevel(ResourceKey.create(Registries.DIMENSION, it)) }
  }
}
