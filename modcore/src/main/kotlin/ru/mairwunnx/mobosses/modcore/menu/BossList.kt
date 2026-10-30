package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.modcore.boss.Boss
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * For admins, as on Paper: the bosses in loaded chunks, nearest first, then other worlds, 45 to a page. A click shows
 * a boss's details in the chat, Shift and a click takes the admin to it, and a right click asks once and then removes
 * it, without loot.
 */
class BossList private constructor(mod: MoBosses, player: ServerPlayer, private val page: Int, private val pages: Int, private val slots: List<UUID>) : Page(mod, player, 6) {
  /** The slot whose boss a right click would remove now. */
  private var armed: Int? = null

  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (slot) {
      PREVIOUS -> if (page > 0) open(mod, player, page - 1) else return
      NEXT -> if (page < pages - 1) open(mod, player, page + 1) else return
      BACK -> Menu.open(mod, player)
      CLOSE -> close()
      in slots.indices -> {
        val boss = mod.bosses.get(slots[slot])?.takeIf { it.entity.isAlive }
          ?: return mod.texts.send(player, Message.BOSS_LIST_GONE).also { sound(); open(mod, player, page) }
        val code = mod.texts.languageOf(player)
        when {
          right && armed == slot -> {
            mod.bosses.remove(boss)
            mod.texts.send(player, Message.BOSS_LIST_REMOVED, *mod.texts.boss(code, boss))
            open(mod, player, page)
          }
          right -> {
            armed = slot
            set(slot, item(Items.BARRIER, mod.languages.lines(code, Message.BOSS_LIST_CONFIRM)))
          }
          shift -> {
            close()
            val entity = boss.entity
            player.teleportTo(entity.level() as ServerLevel, entity.x, entity.y, entity.z, emptySet(), player.yRot, player.xRot, true)
            mod.texts.send(player, Message.BOSS_LIST_TELEPORTED, *mod.texts.boss(code, boss))
          }
          else -> mod.texts.send(player, Message.BOSS_LIST_INFO, *details(mod, player, boss))
        }
      }
      else -> return
    }
    sound()
  }

  companion object {
    private const val PER_PAGE = 45
    private const val PREVIOUS = 45
    private const val BACK = 47
    private const val INDEX = 49
    private const val CLOSE = 51
    private const val NEXT = 53

    fun open(mod: MoBosses, player: ServerPlayer, page: Int) {
      if (!Perms.check(player.createCommandSourceStack(), Permissions.BOSSES)) return mod.texts.send(player, Message.NO_PERMISSION)
      val here = player.level()
      // Nearest first in the admin's world; other worlds after, by name.
      val bosses = mod.bosses.all.filter { it.entity.isAlive }.sortedWith(
        compareBy<Boss> { it.entity.level() != here }.thenBy { if (it.entity.level() == here) it.entity.distanceToSqr(player) else 0.0 }
          .thenBy { it.entity.level().dimension().identifier().toString() },
      )
      val pages = ((bosses.size + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)
      val current = page.coerceIn(0, pages - 1)
      val shown = bosses.drop(current * PER_PAGE).take(PER_PAGE)
      val code = mod.texts.languageOf(player)
      val languages = mod.languages
      val counts = arrayOf(Placeholder.unparsed("page", (current + 1).toString()), Placeholder.unparsed("pages", pages.toString()), Placeholder.unparsed("count", bosses.size.toString()))

      val list = BossList(mod, player, current, pages, shown.map { it.uuid })
      with(list) {
        shown.forEachIndexed { slot, boss ->
          val name = languages.render(code, Message.BOSS_TAG, *mod.texts.boss(code, boss))
          set(slot, item(egg(boss.entity.type), listOfNotNull(name) + languages.lines(code, Message.BOSS_LIST_LORE, *details(mod, player, boss))))
        }
        if (bosses.isEmpty()) set(22, item(Items.STRUCTURE_VOID, languages.lines(code, Message.BOSS_LIST_EMPTY)))
        if (current > 0) set(PREVIOUS, item(Items.ARROW, languages.lines(code, Message.MENU_PREVIOUS)))
        if (current < pages - 1) set(NEXT, item(Items.ARROW, languages.lines(code, Message.MENU_NEXT)))
        set(BACK, item(Items.BOOK, languages.lines(code, Message.MENU_BACK)))
        set(INDEX, item(Items.PAPER, languages.lines(code, Message.BOSS_LIST_TITLE, *counts)))
        set(CLOSE, item(Items.OAK_DOOR, languages.lines(code, Message.MENU_CLOSE)))
        open(languages.render(code, Message.BOSS_LIST_TITLE, *counts) ?: Component.text("Mo’Bosses"))
      }
    }

    /** `<health>`, `<max>`, `<world>`, `<x>`, `<y>`, `<z>`, `<distance>`, `<fighters>`, `<phase>`, `<spawner>`, and the boss's own tags. */
    private fun details(mod: MoBosses, player: ServerPlayer, boss: Boss): Array<TagResolver> {
      val code = mod.texts.languageOf(player)
      val entity = boss.entity
      val distance = if (entity.level() == player.level()) sqrt(entity.distanceToSqr(player)).roundToInt().toString() else "—"
      return arrayOf(
        *mod.texts.boss(code, boss),
        Placeholder.unparsed("health", entity.health.roundToInt().toString()),
        Placeholder.unparsed("max", entity.maxHealth.roundToInt().toString()),
        Placeholder.unparsed("world", mod.worldApi.world(entity)),
        Placeholder.unparsed("x", entity.blockX.toString()), Placeholder.unparsed("y", entity.blockY.toString()), Placeholder.unparsed("z", entity.blockZ.toString()),
        Placeholder.unparsed("distance", distance),
        Placeholder.unparsed("fighters", boss.damage.size.toString()),
        Placeholder.component("phase", mod.languages.render(code, if (boss.phaseTwo) Message.WORD_YES else Message.WORD_NO) ?: Component.empty()),
        Placeholder.component("spawner", mod.languages.render(code, if (boss.fromSpawner) Message.WORD_YES else Message.WORD_NO) ?: Component.empty()),
      )
    }
  }
}
