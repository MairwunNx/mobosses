package ru.mairwunnx.mobosses.modcore.gametest

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.TagValueOutput
import java.util.UUID
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.modcore.achievement.Achievements
import ru.mairwunnx.mobosses.modcore.achievement.BossKilled
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod
import ru.mairwunnx.mobosses.modcore.gametest.Players.survivor

/**
 * A fight on a real server: a player's hits make their share, a solo kill pays it all with the boss's loot for them
 * alone, abilities' temporary blocks go back, the advancements are there, and `mobosses:boss_killed` reads its
 * conditions.
 */
class FightGameTests {
  @GameTest(maxTicks = 60)
  fun aSoloKillPaysTheWholeShare(helper: GameTestHelper) {
    val player = survivor(helper)
    val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(2, 2, 1))
    val catalog = mod.catalog
    val boss = mod.bosses.make(zombie, catalog.types.getValue(zombie.type), catalog.classes.byKey("mythic")!!, 30)
    val experience = player.totalExperience
    val sources = helper.level.damageSources()
    zombie.hurtServer(helper.level, sources.playerAttack(player), 5f)
    helper.assertTrue((boss.damage[player.uuid] ?: 0.0) in 4.9..5.0, "the hit didn't count as the player's: ${boss.damage}")
    // A fall after the hit is the player's too: alone, they get all of it.
    val before = zombie.health
    val share = boss.damage[player.uuid] ?: 0.0
    zombie.hurtServer(helper.level, sources.fall(), 12f)
    val fell = (before - zombie.health).toDouble()
    helper.assertTrue(fell > 0.0 && kotlin.math.abs((boss.damage[player.uuid] ?: 0.0) - share - fell) < 0.01, "the fall's $fell didn't count as the player's: ${boss.damage}")
    zombie.hurtServer(helper.level, sources.playerAttack(player), 100_000f)
    helper.assertTrue(zombie.isDeadOrDying, "the boss didn't die")
    helper.assertTrue(mod.bosses.get(zombie) == null, "a dead boss stays live")
    helper.assertTrue(player.totalExperience > experience, "the player got no experience")
    helper.assertTrue(player.chat.any { it.contains("100%") }, "the kill note doesn't say 100%, though the player fought alone: ${player.chat}")
    val loot = helper.level.getEntitiesOfClass(ItemEntity::class.java, zombie.boundingBox.inflate(4.0))
    helper.assertTrue(loot.any { owner(helper, it) == player.uuid }, "no loot for the player alone")
    helper.succeed()
  }

  /** Who may pick an item up: the game keeps it as the item's `Owner` and shows it nowhere else. */
  private fun owner(helper: GameTestHelper, item: ItemEntity): UUID? {
    val output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.level.registryAccess())
    item.saveWithoutId(output)
    return output.buildResult().read("Owner", UUIDUtil.CODEC).orElse(null)
  }

  @GameTest(maxTicks = 40)
  fun temporaryBlocksGoBack(helper: GameTestHelper) {
    val pos = helper.absolutePos(BlockPos(3, 2, 3))
    helper.assertTrue(mod.blockChanges.setFor(helper.level, pos, Blocks.COBWEB.defaultBlockState(), 10), "the guards refused a web outside any claim")
    helper.assertTrue(helper.level.getBlockState(pos).`is`(Blocks.COBWEB), "no web")
    helper.runAfterDelay(15) {
      helper.assertTrue(helper.level.getBlockState(pos).isAir, "the web is still there")
      helper.succeed()
    }
  }

  @GameTest
  fun theAdvancementsAreThere(helper: GameTestHelper) {
    val missing = Achievement.entries.filter { helper.level.server.advancements.get(Achievements.id(it)) == null }
    helper.assertTrue(missing.isEmpty(), "missing advancements: ${missing.map { it.id }}")
    helper.succeed()
  }

  @GameTest
  fun bossKilledReadsItsConditions(helper: GameTestHelper) {
    val json = JsonParser.parseString("""{ "type": "zombie", "class": { "min": "epic" }, "level": { "min": 50 } }""")
    val instance = BossKilled.Instance.CODEC.parse(JsonOps.INSTANCE, json).orThrow
    helper.assertTrue(instance.matches("zombie", "mythic", 60), "a level 60 Mythic zombie doesn't match")
    helper.assertFalse(instance.matches("zombie", "rare", 60), "a Rare zombie matches a minimum of Epic")
    helper.assertFalse(instance.matches("skeleton", "mythic", 60), "a skeleton matches a zombie")
    helper.assertFalse(instance.matches("zombie", "mythic", 40), "level 40 matches a minimum of 50")
    val any = BossKilled.Instance.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).orThrow
    helper.assertTrue(any.matches("bogged", "common", 1), "no conditions match every boss")
    helper.succeed()
  }
}
