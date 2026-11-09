package ru.mairwunnx.mobosses.modcore.gametest

import chat.allay.expansion.AllayApi
import chat.allay.expansion.AllayExpansion
import chat.allay.expansion.AllayExpansions
import chat.allay.expansion.AllayTool
import chat.allay.expansion.Args
import chat.allay.expansion.Asker
import chat.allay.expansion.ToolCall
import chat.allay.expansion.ToolException
import com.google.gson.GsonBuilder
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.LevelBasedPermissionSet
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod
import ru.mairwunnx.mobosses.modcore.hooks.AllayBosses
import java.util.Optional
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Mo’Bosses' tools as Allay runs them, without Allay: a host of our own takes the expansion through the API's own
 * classes, and each tool runs for a real player, its answer turned into the plain values Allay writes as JSON. The
 * answers keep under Allay's 8000 characters, `nearby` names no other fighter, and `spawn` refuses a player without
 * `mobosses.spawn` and spawns for an operator.
 */
@Suppress("DEPRECATION")
class AllayGameTests {
  /** Allay's side of the API, as far as a test needs it. */
  private class Host : AllayExpansions.Host {
    var expansion: AllayExpansion? = null
    override fun register(expansion: AllayExpansion) { this.expansion = expansion }
    override fun unregister(id: String) { expansion = null }
    override fun apiVersion(): String = AllayApi.VERSION
  }

  private class Ask(private val player: ServerPlayer, private val language: String) : Asker {
    override fun uuid(): UUID = player.uuid
    override fun name(): String = player.plainTextName
    override fun locale() = language
    override fun language() = language
    override fun isOperator() = false
    override fun hasPermission(permission: String, byDefault: Boolean) = byDefault
    override fun dimension() = "overworld"
    override fun x() = player.blockX
    override fun y() = player.blockY
    override fun z() = player.blockZ
    override fun <T> unwrap(type: Class<T>): T? = if (type.isInstance(player)) type.cast(player) else null
  }

  private class Run(private val asker: Asker, private val args: Map<String, Any?>) : ToolCall {
    override fun asker() = asker
    override fun args() = Args(args)
    override fun mode() = ToolCall.Mode.ASK
    override fun isPublic() = false
    override fun nameOf(player: UUID, name: String) = name
    override fun canSee(player: UUID) = true
  }

  private val json = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

  private fun <T> withAllay(block: (AllayExpansion) -> T): T {
    val host = Host()
    AllayExpansions.bind(host)
    AllayBosses.mod = mod
    AllayBosses.register()
    return try {
      block(host.expansion ?: error("Allay didn't get Mo’Bosses"))
    } finally {
      AllayExpansions.unregister(AllayBosses.id())
      AllayExpansions.bind(null)
    }
  }

  private fun tool(expansion: AllayExpansion, name: String): AllayTool = expansion.tools().first { it.name() == name }

  /** A tool's answer as Allay writes it, and its JSON. */
  private fun run(expansion: AllayExpansion, name: String, player: ServerPlayer, args: Map<String, Any?> = emptyMap(), language: String = "en_us"): Pair<Map<String, Any?>, String> {
    val result = tool(expansion, name).run(Run(Ask(player, language), args))
    // A later answer reads the database on its own thread, which a busy CI runner can hold up for seconds.
    val data = result.pending()?.toCompletableFuture()?.get(30, TimeUnit.SECONDS)?.data() ?: result.data()
    return data to json.toJson(data)
  }

  /** Whether [block] is refused with a reason the model reads. */
  private fun refused(block: () -> Unit): Boolean = try {
    block()
    false
  } catch (_: ToolException) {
    true
  }

  @Suppress("UNCHECKED_CAST")
  private fun list(value: Any?) = value as List<Map<String, Any?>>

  @GameTest
  fun allayTakesTheToolsItemsAndPrompt(helper: GameTestHelper) = withAllay { expansion ->
    val tools = expansion.tools().associateBy { it.name() }
    helper.assertTrue(tools.keys == setOf("classes", "bosses", "hunt", "nearby", "loot", "abilities", "hunters", "spawn"), "eight tools: ${tools.keys}")
    helper.assertTrue(tools.values.filter { it.name() != "spawn" }.all { it.access() == AllayTool.Access.READ }, "every tool but spawn only reads")
    helper.assertTrue(tools.getValue("spawn").access() == AllayTool.Access.OPERATOR && tools.getValue("spawn").permission() == "mobosses.spawn" && !tools.getValue("spawn").inPublic(), "spawn is an operator's, behind mobosses.spawn")
    helper.assertTrue(listOf("classes", "bosses", "loot", "abilities", "hunters").all { tools.getValue(it).inPublic() } && listOf("hunt", "nearby").none { tools.getValue(it).inPublic() }, "only the asker's own tools stay out of public questions")
    val spawnSchema = tools.getValue("spawn").parameters().toJson()
    helper.assertTrue(Regex("\"required\"\\s*:\\s*\\[\\s*\"type\"").containsMatchIn(spawnSchema) && "\"required\"" !in tools.getValue("loot").parameters().toJson(), "type is required for spawn alone: $spawnSchema")
    val items = expansion.items()
    helper.assertTrue(items.size in 100..400, "${items.size} named drops")
    helper.assertTrue(items.all { it.id().startsWith("mobosses:") && it.base() != null && "en_us" in it.names() && "ru_ru" in it.names() && it.description().length <= 300 }, "each drop has an id, a base, English and Russian names, and a short description")
    val blade = items.first { it.id() == "mobosses:apprentice_stone_blade" }
    helper.assertTrue(blade.base() == "stone_sword" && blade.names()["en_us"] == "Apprentice’s Stone Blade", "the blade is a stone sword: ${blade.base()}, ${blade.names()["en_us"]}")
    helper.assertTrue(expansion.promptExpansion().length <= 2000, "the prompt fits in 2000 characters")
    helper.succeed()
  }

  @GameTest
  fun everyAnswerFitsAllaysLimit(helper: GameTestHelper) = withAllay { expansion ->
    val player = Players.survivor(helper)
    val asks = listOf(
      "classes" to emptyMap(), "bosses" to emptyMap(), "bosses" to mapOf("type" to "zombie"), "abilities" to emptyMap(), "abilities" to mapOf("ability" to "chain"),
      "loot" to emptyMap(), "loot" to mapOf("class" to "mythic", "level" to 100L, "type" to "evoker"), "loot" to mapOf("item" to "sword"), "hunters" to emptyMap(), "hunt" to emptyMap(),
    )
    asks.forEach { (name, args) ->
      listOf("en_us", "ru_ru").forEach { language ->
        val (_, text) = run(expansion, name, player, args, language)
        helper.assertTrue(text.length < 8000, "$name $args in $language: ${text.length} characters")
      }
    }
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  @GameTest
  fun theToolsAnswerWhatTheyPromise(helper: GameTestHelper) = withAllay { expansion ->
    val player = Players.survivor(helper)
    val (bosses, _) = run(expansion, "bosses", player, mapOf("type" to "minecraft:zombie"))
    helper.assertTrue(list(bosses["bosses"]).single()["id"] == "minecraft:zombie" && (bosses["boss"] as Map<*, *>)["id"] == "minecraft:zombie", "bosses with a type answers that one in full")
    val (all, _) = run(expansion, "bosses", player)
    helper.assertTrue(list(all["bosses"]).size > 10 && all["boss"] == null, "bosses without a type lists them all")

    val (abilities, _) = run(expansion, "abilities", player)
    helper.assertTrue(list(abilities["abilities"]).any { it["key"] == "lightning" && (it["from_level"] as List<*>).size == 6 } && list(abilities["signatures"]).any { it["key"] == "pounce" && "spider" in (it["boss_types"] as List<*>) }, "abilities lists the shared and the signature moves")
    val (chain, _) = run(expansion, "abilities", player, mapOf("ability" to "chain"))
    val detail = chain["ability"] as Map<*, *>
    helper.assertTrue((detail["tip"] as String).isNotBlank() && detail["cooldown_seconds"] == 18.0 && detail["radius"] == 24.0, "one ability comes in full: $detail")
    helper.assertTrue(refused { run(expansion, "abilities", player, mapOf("ability" to "nothing")) }, "an unknown ability is refused")

    val (loot, _) = run(expansion, "loot", player, mapOf("class" to "epic", "level" to 30L))
    val drops = list(loot["drops"])
    helper.assertTrue(drops.isNotEmpty() && drops.size <= 20 && drops.zipWithNext().all { (a, b) -> (a["chance_percent"] as Double) >= (b["chance_percent"] as Double) }, "loot comes likeliest first")
    helper.assertTrue(loot["items_per_player"] is Number && loot["loot_share_percent"] is Double, "loot says how many items and the share they take")
    val (found, _) = run(expansion, "loot", player, mapOf("item" to "Apprentice"), "en_us")
    helper.assertTrue(list(found["drops"]).any { it["name"] == "Apprentice’s Stone Blade" && it["from_class"] == "rare" }, "an item is found by its name: ${found["drops"]}")
    val (russian, _) = run(expansion, "loot", player, mapOf("item" to "apprentice_stone"), "ru_ru")
    val names = list(russian["drops"]).mapNotNull { it["name"] as String? }
    helper.assertTrue(names.isNotEmpty() && "Apprentice’s Stone Blade" !in names, "names come in the asker's language: $names")

    val (hunt, _) = run(expansion, "hunt", player)
    helper.assertTrue(hunt.containsKey("survival_bonus_percent") && hunt["full_moon"] is Boolean && hunt.containsKey("full_moon_chance") && hunt.containsKey("next_boss_in_seconds"), "hunt adds the survival bonus and the full moon: ${hunt.keys}")
    val (hunters, _) = run(expansion, "hunters", player, mapOf("limit" to 3L))
    helper.assertTrue(list(hunters["hunters"]).size <= 3, "hunters keeps to the limit")
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  @GameTest
  fun nearbyNamesNoOtherFighter(helper: GameTestHelper) = withAllay { expansion ->
    val player = Players.survivor(helper, Vec3(1.5, 2.0, 1.5))
    val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(4, 2, 4))
    val boss = mod.bosses.make(zombie, mod.catalog.types.getValue(EntityTypes.ZOMBIE), mod.catalog.classes.byKey("legendary")!!, 60)
    val stranger = UUID.randomUUID()
    boss.addDamage(player.uuid, 10.0)
    boss.addDamage(stranger, 30.0)
    val (nearby, text) = run(expansion, "nearby", player)
    // Other tests' bosses stand nearby too.
    val found = list(nearby["bosses"]).single { it["level"] == 60 && it["class"] == "legendary" }
    val whole = maxOf(zombie.maxHealth.toDouble(), 40.0)
    helper.assertTrue(found["your_share_percent"] == Math.round(10.0 / whole * 10000) / 100.0, "the asker's own share: ${found["your_share_percent"]}")
    helper.assertTrue(found["fighters"] == 2 && found["group_health_bonus_percent"] is Double, "how many fight it, and what groups added")
    helper.assertTrue("infection" in (found["abilities"] as List<*>) && "lightning" in (found["abilities"] as List<*>), "a level 60 Legendary zombie has reached lightning and its signature move: ${found["abilities"]}")
    helper.assertTrue(stranger.toString() !in text, "nobody else's name")
    mod.bosses.remove(boss)
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  @GameTest
  fun spawnIsAnOperatorsAlone(helper: GameTestHelper) = withAllay { expansion ->
    val player = Players.survivor(helper, Vec3(1.5, 2.0, 1.5))
    val before = mod.bosses.all.size
    helper.assertTrue(refused { run(expansion, "spawn", player, mapOf("type" to "zombie")) }, "spawn is refused without mobosses.spawn")
    helper.assertTrue(mod.bosses.all.size == before, "a player without mobosses.spawn spawns nothing")
    val server = helper.level.server
    // The test server gives operators no level of their own: a game master's, as on a dedicated server.
    server.playerList.op(player.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.empty())
    try {
      helper.assertTrue(refused { run(expansion, "spawn", player, mapOf("type" to "nothing")) }, "an unknown type is refused")
      helper.assertTrue(refused { run(expansion, "spawn", player, mapOf("class" to "rare")) }, "spawn needs a type")
      val (spawned, _) = run(expansion, "spawn", player, mapOf("type" to "zombie", "class" to "rare", "level" to 12L, "count" to 2L))
      helper.assertTrue(list(spawned["spawned"]).size == 2 && spawned["failed"] == null, "an operator gets two bosses: $spawned")
      val made = mod.bosses.all.filter { it.level == 12 && it.clazz.key == "rare" && it.entity.distanceToSqr(player) < 16.0 }
      helper.assertTrue(made.size == 2 && made.all { it.pinned }, "pinned, as from the command")
      made.forEach(mod.bosses::remove)
    } finally {
      server.playerList.deop(player.nameAndId())
      server.playerList.remove(player)
    }
    helper.succeed()
  }
}
