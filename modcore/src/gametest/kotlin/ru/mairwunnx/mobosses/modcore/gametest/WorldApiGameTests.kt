package ru.mairwunnx.mobosses.modcore.gametest

import com.mairwunnx.worldapi.expansion.v2.Param
import com.mairwunnx.worldapi.expansion.v2.Permission
import com.mairwunnx.worldapi.expansion.v2.Place
import com.mairwunnx.worldapi.expansion.v2.Request
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod
import ru.mairwunnx.mobosses.modcore.hooks.BossesActions
import java.util.UUID

/**
 * Mo’Bosses' WorldAPI actions without WorldAPI: each handler runs on the server thread with a request of our own, as
 * WorldAPI would call it once it read the body. A spawn makes pinned bosses where the place says and names them by the
 * uuid `bosses` shows, a removal takes one by that id, and bad input is refused the way WorldAPI turns into 400 and 404.
 */
class WorldApiGameTests {
  private class Ask(private val values: Map<Param<*>, Any?>) : Request {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(param: Param<T>): T = (if (param in values) values[param] else param.fallback()) as T
    override fun anonymous() = false
    override fun seesCoordinates() = true
    override fun can(permission: Permission) = true
  }

  private fun <T> refused(kind: Class<T>, block: () -> Unit): Boolean = try {
    block()
    false
  } catch (error: Throwable) {
    kind.isInstance(error)
  }

  @GameTest
  fun actionsSpawnAndRemoveBossesByTheirIds(helper: GameTestHelper) {
    BossesActions.mod = mod
    val world = helper.level.dimension().identifier().path
    val spot = helper.absolutePos(BlockPos(4, 2, 4))
    val place = Place(world, spot.x + 0.5, spot.y.toDouble(), spot.z + 0.5)
    val body = mapOf<Param<*>, Any?>(BossesActions.type to "minecraft:zombie", BossesActions.rank to BossesActions.Rank.EPIC, BossesActions.level to 33, BossesActions.count to 2, BossesActions.at to place)
    val spawned = BossesActions.spawn.handler().read(Ask(body)) as BossesActions.Spawned
    helper.assertTrue(spawned.bosses.size == 2 && spawned.failed == null && spawned.bosses.all { it.type == "zombie" && it.`class` == "epic" && it.level == 33 && it.location.world() == world }, "two Epic zombies at level 33: $spawned")
    val first = spawned.bosses.first()
    val live = mod.bosses.all.firstOrNull { it.uuid == first.uuid }
    helper.assertTrue(live != null && live.pinned, "the answer's uuid names a live, pinned boss")

    val removed = BossesActions.kill.handler().read(Ask(mapOf(BossesActions.boss to first.uuid.toString()))) as BossesActions.SpawnedBoss
    helper.assertTrue(removed.uuid == first.uuid && mod.bosses.all.none { it.uuid == first.uuid } && !live!!.entity.isAlive, "removed by its id")
    helper.assertTrue(refused(NoSuchElementException::class.java) { BossesActions.kill.handler().read(Ask(mapOf(BossesActions.boss to first.uuid.toString()))) }, "a boss already gone is 404")
    helper.assertTrue(refused(NoSuchElementException::class.java) { BossesActions.kill.handler().read(Ask(mapOf(BossesActions.boss to UUID.randomUUID().toString()))) }, "an unknown id is 404")
    helper.assertTrue(refused(NoSuchElementException::class.java) { BossesActions.kill.handler().read(Ask(mapOf(BossesActions.boss to "not-a-uuid"))) }, "a malformed id is 404")

    helper.assertTrue(refused(IllegalArgumentException::class.java) { BossesActions.spawn.handler().read(Ask(body + (BossesActions.type to "nothing"))) }, "an unknown type is 400")
    helper.assertTrue(refused(IllegalArgumentException::class.java) { BossesActions.spawn.handler().read(Ask(body + (BossesActions.at to place.copyWorld("nowhere")))) }, "an unknown world is 400")
    helper.assertTrue(refused(IllegalArgumentException::class.java) { BossesActions.spawn.handler().read(Ask(body + (BossesActions.level to 1000))) }, "a level past the top is 400")
    helper.assertTrue(BossesActions.spawn.permission() == Permission.WORLD_WRITE && BossesActions.kill.permission() == Permission.WORLD_WRITE && BossesActions.spawn.serverThread() && BossesActions.kill.serverThread(), "both need world:write and run on the server thread")
    mod.bosses.all.filter { it.uuid in spawned.bosses.map { boss -> boss.uuid } }.forEach(mod.bosses::remove)
    helper.succeed()
  }

  private fun Place.copyWorld(name: String) = Place(name, x(), y(), z())
}
