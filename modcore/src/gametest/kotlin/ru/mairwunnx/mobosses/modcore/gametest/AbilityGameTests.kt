package ru.mairwunnx.mobosses.modcore.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.block.Blocks
import ru.mairwunnx.mobosses.modcore.ability.Abilities
import ru.mairwunnx.mobosses.modcore.ability.Ability
import ru.mairwunnx.mobosses.modcore.ability.Cast
import ru.mairwunnx.mobosses.modcore.ability.kinds.Enrage
import ru.mairwunnx.mobosses.modcore.ability.kinds.PhaseTwo
import ru.mairwunnx.mobosses.modcore.ability.signature.Signature
import ru.mairwunnx.mobosses.modcore.gametest.Players.mod
import ru.mairwunnx.mobosses.modcore.gametest.Players.survivor
import net.minecraft.world.phys.Vec3

/**
 * Every ability, cast once on a real server by a Mythic level 100 boss with a player in reach: each signature by a
 * boss of its own type, the rest by a zombie. A test server has no player to hit a boss, so this is where each
 * ability's code first meets the world; a crash fails the test, and the log names the ones that found nothing to do.
 */
class AbilityGameTests {
  private fun boss(helper: GameTestHelper, type: EntityType<*>): Cast {
    val entity = helper.spawn(type, BlockPos(1, 2, 1)) as LivingEntity
    (entity as? Mob)?.setNoAi(true)
    val catalog = mod.catalog
    val spec = catalog.types[type] ?: error("${BuiltInRegistries.ENTITY_TYPE.getKey(type)} isn't a boss type")
    val boss = mod.bosses.make(entity, spec, catalog.classes.byKey("mythic")!!, 100)
    return Cast(mod, boss, null)
  }

  /** One cast with a player at [at] who just hit the boss: true when it did something, null when it found nothing to do. */
  private fun attempt(helper: GameTestHelper, ability: Ability, type: EntityType<*>, at: Vec3, failed: MutableList<String>): Boolean? {
    val cast = boss(helper, type)
    val player = survivor(helper, at)
    cast.boss.addDamage(player.uuid, 1.0)
    cast.entity.health = cast.entity.maxHealth * 0.2f
    val result = runCatching { ability.usable(Cast(mod, cast.boss, player)) && ability.cast(Cast(mod, cast.boss, player)) }
    result.exceptionOrNull()?.let { failed += "${ability.key}: ${it::class.simpleName}: ${it.message}" }
    mod.bosses.remove(cast.boss)
    helper.level.server.playerList.remove(player)
    return result.getOrNull()?.takeIf { it }
  }

  @GameTest(maxTicks = 200, padding = 8)
  fun everyAbilityCasts(helper: GameTestHelper) {
    // Ground to stand on, and air above it, for meteors, rings, and copies.
    for (x in -2..10) for (z in -2..10) helper.setBlock(BlockPos(x, 1, z), Blocks.STONE)
    val failed = ArrayList<String>()
    val idle = ArrayList<String>()
    (Abilities.PICKED + Enrage).forEach { ability ->
      val types = if (ability is Signature) mod.catalog.signatureTypes[ability.key].orEmpty() else setOf(EntityTypes.ZOMBIE)
      val type = types.firstOrNull { it in mod.catalog.types } ?: return@forEach failed.add("${ability.key}: no boss type").let {}
      // Across the structure first, far enough for grapple and chain; abilities that reach only near get a player close by.
      val cast = listOf(Vec3(7.5, 2.0, 7.5), Vec3(3.5, 2.0, 2.5)).firstNotNullOfOrNull { at -> attempt(helper, ability, type, at, failed) }
      if (cast != true) idle += ability.key
    }
    runCatching { PhaseTwo.cast(boss(helper, EntityTypes.ZOMBIE)) }.exceptionOrNull()?.let { failed += "phase_two: $it" }
    helper.assertTrue(failed.isEmpty(), failed.joinToString("; "))
    mod.log.info("Abilities cast in the test: ${(Abilities.PICKED + Enrage).size - idle.size}; found nothing to do: ${idle.joinToString().ifEmpty { "none" }}")
    helper.runAfterDelay(100) {
      mod.bosses.all.toList().forEach(mod.bosses::remove)
      helper.succeed()
    }
  }
}
