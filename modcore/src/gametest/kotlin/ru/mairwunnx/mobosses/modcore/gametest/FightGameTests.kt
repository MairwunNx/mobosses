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
import com.google.gson.GsonBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.util.zip.Deflater
import net.minecraft.advancements.Advancement as GameAdvancement
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.achievement.Achievement
import ru.mairwunnx.mobosses.achievement.Advancement
import ru.mairwunnx.mobosses.progress.BestiaryEntry
import ru.mairwunnx.mobosses.modcore.Mod
import ru.mairwunnx.mobosses.modcore.achievement.Achievements
import ru.mairwunnx.mobosses.modcore.achievement.BossKilled
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod
import ru.mairwunnx.mobosses.modcore.gametest.Players.survivor

/**
 * A fight on a real server: a player's hits make their share, a solo kill pays it all with the boss's loot for them
 * alone, abilities' temporary blocks go back, the advancements are there and count kills and Mythic types as the game
 * shows them, N of M, and `mobosses:boss_killed` reads its conditions.
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

  /** What the advancement screen shows under [achievement] for [player], like “3/10”. */
  private fun shown(helper: GameTestHelper, player: ServerPlayer, achievement: Achievement): AdvancementProgress =
    player.advancements.getOrStartProgress(helper.level.server.advancements.get(Achievements.id(achievement))!!)

  @GameTest
  fun killTiersCountEachKill(helper: GameTestHelper) {
    val player = survivor(helper)
    mod.achievements.kills(player, 3)
    val text = { it: Achievement -> shown(helper, player, it).progressText?.string }
    helper.assertTrue(text(Achievement.KILLS_10) == "3/10", "kills_10 shows ${text(Achievement.KILLS_10)}, not 3/10")
    helper.assertTrue(text(Achievement.KILLS_1000) == "3/1000", "kills_1000 shows ${text(Achievement.KILLS_1000)}, not 3/1000")
    mod.achievements.kills(player, 10)
    val tier = shown(helper, player, Achievement.KILLS_10)
    helper.assertTrue(tier.isDone && text(Achievement.KILLS_10) == "10/10", "ten kills don't complete kills_10: ${text(Achievement.KILLS_10)}")
    // At the goal one criterion stands for every kill, so the player's file keeps one line for the tier.
    helper.assertTrue(tier.completedCriteria.toList() == listOf(Advancement.CRITERION), "kills_10 keeps ${tier.completedCriteria.toList()}")
    helper.assertTrue(text(Achievement.KILLS_100) == "10/100", "kills_100 shows ${text(Achievement.KILLS_100)}, not 10/100")
    helper.succeed()
  }

  /**
   * What counting kills costs: the definitions every client gets on join, the progress a kill sends at 99 kills, when
   * three tiers move at once, and a player's advancement file at its biggest, 999 kills. The game compresses packets
   * with zlib, as [deflated] does. 2.24.0 measured 18.5 KB (3.3 KB compressed), 10.1 KB (3.5 KB), and 40 KB.
   */
  @GameTest
  fun theKillTiersStaySmall(helper: GameTestHelper) {
    val server = helper.level.server
    val definitions = RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess())
    Achievement.entries.forEach { GameAdvancement.STREAM_CODEC.encode(definitions, server.advancements.get(Achievements.id(it))!!.value()) }
    val tiers = Achievement.KILLS.map { it.second }
    val player = survivor(helper)
    mod.achievements.kills(player, 99)
    val kill = Unpooled.buffer().also { buffer -> tiers.forEach { AdvancementProgress.STREAM_CODEC.encode(buffer, shown(helper, player, it)) } }
    mod.achievements.kills(player, 999)
    val file = tiers.sumOf { gson.toJson(AdvancementProgress.CODEC.encodeStart(JsonOps.INSTANCE, shown(helper, player, it)).orThrow).toByteArray().size }
    Mod.log.info("Kill tiers: definitions ${definitions.readableBytes()} B (${deflated(definitions)} B compressed), a kill's progress ${kill.readableBytes()} B (${deflated(kill)} B), the file at 999 kills $file B")
    helper.assertTrue(deflated(definitions) < 8_000 && deflated(kill) < 8_000 && file < 64_000, "the kill tiers grew past what a join, a kill, or a file should hold")
    helper.succeed()
  }

  private val gson = GsonBuilder().setPrettyPrinting().create()

  private fun deflated(buffer: ByteBuf): Int {
    val data = ByteArray(buffer.readableBytes()).also { buffer.getBytes(buffer.readerIndex(), it) }
    val deflater = Deflater().apply { setInput(data); finish() }
    return deflater.deflate(ByteArray(data.size + 64)).also { deflater.end() }
  }

  @GameTest
  fun anOldTierStaysComplete(helper: GameTestHelper) {
    val player = survivor(helper)
    // Before tiers counted kills, a tier had one criterion, which a player's file keeps.
    player.advancements.award(helper.level.server.advancements.get(Achievements.id(Achievement.KILLS_100))!!, Advancement.CRITERION)
    mod.achievements.kills(player, 2)
    val tier = shown(helper, player, Achievement.KILLS_100)
    helper.assertTrue(tier.isDone && tier.progressText?.string == "100/100", "an old kills_100 shows ${tier.progressText?.string}")
    helper.succeed()
  }

  @GameTest
  fun theMythicCollectionCountsTypes(helper: GameTestHelper) {
    val player = survivor(helper)
    val types = mod.catalog.types.values.count { it.spec.enabled }
    mod.achievements.bestiary(player, listOf(BestiaryEntry("zombie", "mythic", 1, 30), BestiaryEntry("skeleton", "rare", 1, 30)))
    val text = shown(helper, player, Achievement.MYTHIC_COLLECTOR).progressText?.string
    helper.assertTrue(text == "1/$types", "mythic_collector shows $text, not 1/$types")
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
