package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.Level
import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.boss.AdminSpawns
import ru.mairwunnx.mobosses.modcore.boss.Catalog
import ru.mairwunnx.mobosses.modcore.boss.SpawnSpots
import kotlin.math.abs

/**
 * The last step, as on Paper: where. Each cell is a chunk, north at the top, and the admin's head in the middle is the
 * chunk they stand in. Their head spawns the boss right on them; another chunk, at its safest spot at their height.
 */
class SpawnPlacePage private constructor(
  mod: MoBosses, player: ServerPlayer, private val type: Catalog.BossType, private val clazz: BossClass, private val level: Int,
) : Page(mod, player, 6) {
  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      SpawnMenu.BACK -> SpawnLevelPage.open(mod, player, type, clazz, level)
      SpawnMenu.CLOSE -> close()
      else -> {
        val (dx, dz) = SpawnPlan.offset(slot) ?: return
        val world = player.level() as ServerLevel
        val chunk = player.chunkPosition()
        val at = if (dx == 0 && dz == 0) player.position()
          else SpawnSpots.inChunk(world, chunk.x + dx, chunk.z + dz, player.blockY)
            ?: return mod.texts.send(player, Message.SPAWN_MENU_NO_ROOM).also { sound() }
        close()
        when (val result = mod.adminSpawns.spawn(type, clazz, level, world, at)) {
          is AdminSpawns.Result.Spawned -> mod.texts.send(player, Message.SPAWNED, *mod.texts.boss(mod.texts.languageOf(player), result.boss))
          is AdminSpawns.Result.Failed -> mod.texts.send(player, Message.SPAWN_FAILED, Placeholder.unparsed("reason", result.reason))
        }
      }
    }
    sound()
  }

  companion object {
    fun open(mod: MoBosses, player: ServerPlayer, type: Catalog.BossType, clazz: BossClass, level: Int) {
      val code = mod.texts.languageOf(player)
      val languages = mod.languages
      val chunk = player.chunkPosition()
      val ground = when (player.level().dimension()) {
        Level.NETHER -> Items.NETHERRACK
        Level.END -> Items.END_STONE
        else -> Items.GRASS_BLOCK
      }
      with(SpawnPlacePage(mod, player, type, clazz, level)) {
        for (slot in 0 until SpawnPlan.COLUMNS * SpawnPlan.ROWS) {
          val (dx, dz) = SpawnPlan.offset(slot) ?: continue
          if (dx == 0 && dz == 0) {
            val here = languages.lines(code, Message.SPAWN_MENU_HERE)
            set(slot, name(ItemStack(Items.PLAYER_HEAD).apply { set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.gameProfile)) }, here.firstOrNull(), here.drop(1)))
            continue
          }
          val direction = listOfNotNull(
            if (dz < 0) languages.render(code, Message.SPAWN_MENU_NORTH, Placeholder.unparsed("count", abs(dz).toString())) else null,
            if (dz > 0) languages.render(code, Message.SPAWN_MENU_SOUTH, Placeholder.unparsed("count", dz.toString())) else null,
            if (dx > 0) languages.render(code, Message.SPAWN_MENU_EAST, Placeholder.unparsed("count", dx.toString())) else null,
            if (dx < 0) languages.render(code, Message.SPAWN_MENU_WEST, Placeholder.unparsed("count", abs(dx).toString())) else null,
          ).reduce { a, b -> a.append(Component.text(", ")).append(b) }
          set(slot, item(ground, languages.lines(code, Message.SPAWN_MENU_CHUNK, Placeholder.component("direction", direction),
            Placeholder.unparsed("x", (chunk.x + dx).toString()), Placeholder.unparsed("z", (chunk.z + dz).toString()))))
        }
        SpawnMenu.footer(this)
        open(languages.render(code, Message.SPAWN_MENU_PLACE_TITLE, *SpawnMenu.chosen(mod, code, type, clazz, level)) ?: Component.text("Mo’Bosses"))
      }
    }
  }
}
