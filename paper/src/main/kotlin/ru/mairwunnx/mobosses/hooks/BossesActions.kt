package ru.mairwunnx.mobosses.hooks

import com.mairwunnx.worldapi.expansion.v2.Permission
import com.mairwunnx.worldapi.expansion.v2.Place
import com.mairwunnx.worldapi.expansion.v2.Request
import org.bukkit.Bukkit
import org.bukkit.Location
import ru.mairwunnx.mobosses.boss.AdminSpawns
import ru.mairwunnx.mobosses.boss.Catalog
import java.util.UUID

/**
 * The namespace for WorldAPI 4.6 and newer: the resources and events, and actions that spawn a boss where a client says
 * and remove one by its id. They need `world:write`, run on the server thread, and land in WorldAPI's audit log.
 * Spawns go through [AdminSpawns] with `/mobosses spawn`'s checks, removals as `/mobosses killall` takes bosses: no
 * loot, no kill.
 */
object BossesActions : BossesData() {
  /** The six classes, by their keys. */
  enum class Rank { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }

  val type = text("type", "A boss type's id, like zombie or minecraft:wither_skeleton").required()
  val rank = choice("class", "The boss class, Common to Mythic", Rank::class.java, Rank.COMMON)
  val level = integer("level", "The boss level, from 1 to the top level the server's curve allows", 1, 1000, 1)
  val count = integer("count", "How many bosses, from 1 to 5", 1, 5, 1)
  val at = place("at", "Where to spawn them: a loaded world by its folder name, and coordinates").required()
  val boss = text("boss", "A live boss's id: its uuid in bosses")

  val spawn = action("bosses/spawn", "Spawns bosses at a place, as /mobosses spawn does and under its rules: they stay until killed, and protection applies unless admin_spawn_bypass is on. An unknown type, a level past the server's top, or a world that isn't loaded answers 400", ::spawn).body(type, rank, level, count, at).permission(Permission.WORLD_WRITE).onServerThread().returns(Spawned::class.java)
  val kill = remove("bosses/{boss}", "Removes a live boss as /mobosses killall does: no loot, no experience, no kill for anyone. An id no boss alive has answers 404", ::kill).permission(Permission.WORLD_WRITE).onServerThread().returns(SpawnedBoss::class.java)

  @JvmRecord data class Spawned(val bosses: List<SpawnedBoss>, val failed: String?)

  @JvmRecord data class SpawnedBoss(val uuid: UUID, val type: String, val `class`: String, val level: Int, val location: Place)

  private fun spawn(req: Request): Spawned {
    val catalog = plugin.catalog
    val wanted = req[type].trim().removePrefix("minecraft:")
    val bossType = catalog.types.values.firstOrNull { it.key.equals(wanted, ignoreCase = true) } ?: throw IllegalArgumentException("no boss type $wanted here: ${catalog.types.values.map { it.key }.sorted().joinToString()}")
    val clazz = catalog.classes.byKey(req[rank].name) ?: throw IllegalArgumentException("this server has no class ${req[rank].name.lowercase()}")
    val maxLevel = maxOf(100, catalog.settings.progression.levelCurve.maxLevel)
    val bossLevel = req[level].takeIf { it in 1..maxLevel } ?: throw IllegalArgumentException("level goes from 1 to $maxLevel")
    val place = req[at]
    val world = Bukkit.getWorld(place.world()) ?: throw IllegalArgumentException("no world ${place.world()} is loaded")
    val where = Location(world, place.x(), place.y(), place.z())
    val spawned = ArrayList<SpawnedBoss>()
    repeat(req[count]) {
      when (val result = plugin.adminSpawns.spawn(bossType, clazz, bossLevel, where)) {
        is AdminSpawns.Result.Spawned -> spawned += spawned(result.boss.uuid, bossType, clazz.key, bossLevel, result.boss.entity.location)
        is AdminSpawns.Result.Failed -> if (spawned.isEmpty()) throw IllegalArgumentException("the boss didn't spawn: ${result.reason}") else return Spawned(spawned, result.reason)
      }
    }
    return Spawned(spawned, null)
  }

  private fun kill(req: Request): SpawnedBoss {
    val id = runCatching { UUID.fromString(req[boss].trim()) }.getOrNull()
    val found = plugin.bosses.all.firstOrNull { it.uuid == id && it.entity.isValid } ?: throw NoSuchElementException("no live boss has the id ${req[boss]}")
    val gone = spawned(found.uuid, found.type, found.clazz.key, found.level, found.entity.location)
    plugin.bosses.remove(found)
    return gone
  }

  private fun spawned(id: UUID, type: Catalog.BossType, clazz: String, level: Int, at: Location) = SpawnedBoss(id, type.key, clazz, level, place(at))
}
