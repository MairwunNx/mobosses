package ru.mairwunnx.mobosses.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import ru.mairwunnx.mobosses.MoBossesPlugin
import ru.mairwunnx.mobosses.boss.AdminSpawns
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.boss.SpawnPlan
import ru.mairwunnx.mobosses.boss.SpawnSpots
import ru.mairwunnx.mobosses.config.BossClass
import ru.mairwunnx.mobosses.lang.Message
import kotlin.math.abs

/**
 * The last step: where. Each cell is a chunk, north at the top, and the admin's head in the middle is the chunk they
 * stand in. Their head spawns the boss right on them; another chunk, at its safest spot at their height.
 */
class SpawnPlacePage private constructor(
  private val plugin: MoBossesPlugin, private val type: Catalog.BossType, private val clazz: BossClass, private val level: Int,
) : Page {
  private lateinit var inventory: Inventory

  override fun getInventory() = inventory

  override fun click(player: Player, slot: Int, click: ClickType) {
    when (slot) {
      SpawnMenu.BACK -> SpawnLevelPage.open(plugin, player, type, clazz, level)
      SpawnMenu.CLOSE -> player.closeInventory()
      else -> {
        val (dx, dz) = SpawnPlan.offset(slot) ?: return
        val here = player.location
        val at = if (dx == 0 && dz == 0) here.clone()
          else SpawnSpots.inChunk(here.world, here.chunk.x + dx, here.chunk.z + dz, here.blockY)
            ?: return plugin.texts.send(player, Message.SPAWN_MENU_NO_ROOM).also { sound(player) }
        player.closeInventory()
        when (val result = plugin.adminSpawns.spawn(type, clazz, level, at)) {
          is AdminSpawns.Result.Spawned -> plugin.texts.send(player, Message.SPAWNED, *plugin.texts.boss(plugin.texts.languageOf(player), result.boss))
          is AdminSpawns.Result.Failed -> plugin.texts.send(player, Message.SPAWN_FAILED, Placeholder.unparsed("reason", result.reason))
        }
      }
    }
    sound(player)
  }

  companion object {
    fun open(plugin: MoBossesPlugin, player: Player, type: Catalog.BossType, clazz: BossClass, level: Int) {
      val code = plugin.texts.languageOf(player)
      val languages = plugin.languages
      val page = SpawnPlacePage(plugin, type, clazz, level)
      val inventory = Bukkit.createInventory(page, 54, languages.render(code, Message.SPAWN_MENU_PLACE_TITLE, *SpawnMenu.chosen(plugin, code, type, clazz, level)) ?: Component.text("Mo’Bosses"))
      page.inventory = inventory
      val chunk = player.location.chunk
      for (slot in 0 until SpawnPlan.COLUMNS * SpawnPlan.ROWS) {
        val (dx, dz) = SpawnPlan.offset(slot) ?: continue
        if (dx == 0 && dz == 0) {
          inventory.setItem(slot, ItemStack(Material.PLAYER_HEAD).apply {
            editMeta(SkullMeta::class.java) { it.owningPlayer = player }
            Page.name(this, languages.lines(code, Message.SPAWN_MENU_HERE).firstOrNull(), languages.lines(code, Message.SPAWN_MENU_HERE).drop(1))
          })
          continue
        }
        val direction = listOfNotNull(
          if (dz < 0) languages.render(code, Message.SPAWN_MENU_NORTH, Placeholder.unparsed("count", abs(dz).toString())) else null,
          if (dz > 0) languages.render(code, Message.SPAWN_MENU_SOUTH, Placeholder.unparsed("count", dz.toString())) else null,
          if (dx > 0) languages.render(code, Message.SPAWN_MENU_EAST, Placeholder.unparsed("count", dx.toString())) else null,
          if (dx < 0) languages.render(code, Message.SPAWN_MENU_WEST, Placeholder.unparsed("count", abs(dx).toString())) else null,
        ).reduce { a, b -> a.append(Component.text(", ")).append(b) }
        inventory.setItem(slot, Page.item(ground(player.world), languages.lines(code, Message.SPAWN_MENU_CHUNK, Placeholder.component("direction", direction),
          Placeholder.unparsed("x", (chunk.x + dx).toString()), Placeholder.unparsed("z", (chunk.z + dz).toString()))))
      }
      SpawnMenu.footer(plugin, player, inventory)
      player.openInventory(inventory)
    }

    private fun ground(world: World) = when (world.environment) {
      World.Environment.NETHER -> Material.NETHERRACK
      World.Environment.THE_END -> Material.END_STONE
      else -> Material.GRASS_BLOCK
    }
  }
}
