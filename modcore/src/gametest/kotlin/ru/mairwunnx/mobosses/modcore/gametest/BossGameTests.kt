package ru.mairwunnx.mobosses.modcore.gametest

import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.util.ProblemReporter
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntitySpawnRequest
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import ru.mairwunnx.mobosses.modcore.boss.Boss
import ru.mairwunnx.mobosses.modcore.boss.Keys
import ru.mairwunnx.mobosses.modcore.data.Data
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod

/**
 * Bosses on a real server: what turning a mob into one writes and sets, that it outlives a save, that the difficulty
 * grades it and peaceful makes none, and that it keeps out of boats and portals.
 */
class BossGameTests {
  private fun zombie(helper: GameTestHelper, x: Int = 1): Zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(x, 2, 1)).also { it.setNoAi(false) }

  private fun boss(helper: GameTestHelper, entity: LivingEntity, clazz: String = "common", level: Int = 30): Boss {
    val catalog = mod.catalog
    return mod.bosses.make(entity, catalog.types.getValue(entity.type), catalog.classes.byKey(clazz)!!, level)
  }

  private fun <T> withDifficulty(helper: GameTestHelper, difficulty: Difficulty, block: () -> T): T {
    val server = helper.level.server
    val before = server.worldData.difficulty
    server.setDifficulty(difficulty, true)
    return try {
      block()
    } finally {
      server.setDifficulty(before, true)
    }
  }

  @GameTest
  fun aMobBecomesABossWithItsDataOnIt(helper: GameTestHelper) {
    val zombie = zombie(helper)
    val boss = withDifficulty(helper, Difficulty.HARD) { boss(helper, zombie, "epic", 40) }
    val data = Data(zombie)
    helper.assertTrue(data.string(Keys.TYPE) == "zombie" && data.string(Keys.CLASS) == "epic" && data.int(Keys.LEVEL) == 40, "the boss's type, class, and level are on it")
    helper.assertTrue(data.double(Keys.DIFFICULTY) == 1.0, "a hard boss carries hard's modifier")
    helper.assertTrue(zombie.maxHealth > 20f && zombie.health == zombie.maxHealth, "a level 40 Epic boss has more than a zombie's health, all of it")
    helper.assertTrue(mod.bosses.get(zombie) === boss, "the boss is live")
    mod.bosses.remove(boss)
    helper.succeed()
  }

  @GameTest
  fun theDifficultyGradesNewBosses(helper: GameTestHelper) {
    val hard = withDifficulty(helper, Difficulty.HARD) { boss(helper, zombie(helper, 1)) }
    val normal = withDifficulty(helper, Difficulty.NORMAL) { boss(helper, zombie(helper, 3)) }
    val ratio = normal.entity.maxHealth / hard.entity.maxHealth
    helper.assertTrue(kotlin.math.abs(ratio - 0.8777777773f) < 0.001f, "a normal boss has 88% of a hard one's health, not ${ratio * 100}%")
    helper.assertTrue(normal.difficulty == 0.8777777773, "a normal boss keeps normal's modifier")
    withDifficulty(helper, Difficulty.PEACEFUL) { helper.assertTrue(mod.bosses.difficulty(helper.level) == null, "peaceful makes no bosses") }
    mod.bosses.remove(hard)
    mod.bosses.remove(normal)
    helper.succeed()
  }

  @GameTest
  fun aBossOutlivesASave(helper: GameTestHelper) {
    val zombie = zombie(helper)
    val boss = boss(helper, zombie, "rare", 25)
    boss.addDamage(java.util.UUID.randomUUID(), 12.0)
    val output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.level.registryAccess())
    zombie.save(output)
    val input = TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), output.buildResult())
    val copy = EntityType.create(input, helper.level, EntitySpawnRequest(EntitySpawnReason.LOAD, true)).orElseThrow()
    val data = Data(copy)
    helper.assertTrue(data.string(Keys.CLASS) == "rare" && data.int(Keys.LEVEL) == 25 && data.string(Keys.DAMAGE)?.contains("=12") == true, "the copy keeps the boss's class, level, and damage")
    mod.bosses.remove(boss)
    helper.succeed()
  }

  @GameTest
  fun aBossKeepsOutOfBoatsAndPortals(helper: GameTestHelper) {
    val boss = boss(helper, zombie(helper))
    val boat = helper.spawn(EntityTypes.OAK_BOAT, BlockPos(3, 2, 3))
    helper.assertFalse(boss.entity.startRiding(boat, false, true), "the boss got into a boat")
    helper.assertFalse(boss.entity.canUsePortal(false), "the boss may go through a portal")
    val plain = zombie(helper, 5)
    helper.assertTrue(plain.startRiding(boat, false, true), "a plain zombie can't get into the boat")
    mod.bosses.remove(boss)
    helper.succeed()
  }

  @GameTest
  fun spawnPutsAPinnedBossWhereTold(helper: GameTestHelper) {
    val at = helper.absolutePos(BlockPos(2, 2, 2))
    val server = helper.level.server
    server.commands.performPrefixedCommand(server.createCommandSourceStack(), "mobosses spawn skeleton 12 legendary ${at.x} ${at.y} ${at.z} overworld")
    val boss = mod.bosses.all.firstOrNull { it.entity.blockPosition() == at && it.type.key == "skeleton" }
    helper.assertTrue(boss != null, "no skeleton boss at the spot")
    helper.assertTrue(boss!!.pinned && boss.level == 12 && boss.clazz.key == "legendary", "the spawned boss isn't a pinned level 12 Legendary")
    mod.bosses.remove(boss)
    helper.succeed()
  }

  @GameTest
  fun aNaturalSpawnWaitsForAPlayerToSeeIt(helper: GameTestHelper) {
    val zombie = EntityTypes.ZOMBIE.spawn(helper.level, helper.absolutePos(BlockPos(2, 2, 2)), EntitySpawnReason.NATURAL)!!
    helper.assertTrue(Data(zombie).string(Keys.PENDING) == "NATURAL", "a natural zombie doesn't wait to roll: ${Data(zombie).string(Keys.PENDING)}")
    val egg = EntityTypes.ZOMBIE.spawn(helper.level, helper.absolutePos(BlockPos(4, 2, 2)), EntitySpawnReason.BREEDING)!!
    helper.assertFalse(Data(egg).has(Keys.PENDING), "a bred zombie waits to roll, though breeding isn't an allowed reason")
    helper.succeed()
  }
}
