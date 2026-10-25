package ru.mairwunnx.mobosses.modcore.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import ru.mairwunnx.mobosses.Permissions
import ru.mairwunnx.mobosses.lang.LanguageChoices
import ru.mairwunnx.mobosses.lang.Message
import ru.mairwunnx.mobosses.modcore.MoBosses
import ru.mairwunnx.mobosses.modcore.Perms
import ru.mairwunnx.mobosses.progress.BossCount
import ru.mairwunnx.mobosses.progress.Hunter

/**
 * The `/mobosses` menu, as on Paper: your card, the best hunters, the bosses by class, the bestiary, your language, a
 * button to start over, and, for admins, the bosses in the world and reload and remove-all buttons. It opens once the
 * database has answered, in the player's language.
 */
class Menu private constructor(mod: MoBosses, player: ServerPlayer, private val actions: Map<Int, Action>) : Page(mod, player, 3) {
  enum class Action { RESET, BESTIARY, LANGUAGE, BOSS_LIST, RELOAD, KILLALL, CLOSE }

  /** The reset button asks once more: the first click only turns it into a warning. */
  private var resetArmed = false
  private var confirmReset: ItemStack? = null

  override fun click(slot: Int, right: Boolean, shift: Boolean) {
    when (actions[slot]) {
      Action.RESET -> if (!resetArmed) {
        resetArmed = true
        set(RESET, confirmReset)
      } else {
        close()
        command("mobosses reset confirm")
      }
      Action.BESTIARY -> Bestiary.open(mod, player)
      Action.LANGUAGE -> LanguagePage.open(mod, player)
      Action.BOSS_LIST -> BossList.open(mod, player, 0)
      Action.RELOAD -> {
        close()
        command("mobosses reload")
      }
      Action.KILLALL -> {
        close()
        command("mobosses killall")
      }
      Action.CLOSE -> close()
      null -> {}
    }
  }

  companion object {
    private const val RESET = 18
    private const val YOU = 11
    private const val TOP = 13
    private const val BOSSES = 15
    private const val BESTIARY = 20
    private const val LANGUAGE = 21
    private const val BOSS_LIST = 22
    private const val RELOAD = 24
    private const val KILLALL = 25
    private const val CLOSE = 26

    fun open(mod: MoBosses, player: ServerPlayer) {
      val stats = mod.menuStats
      stats.card(player.uuid, player.plainTextName).thenCombine(stats.top()) { card, top -> card to top }.thenCombine(stats.counts()) { (card, top), counts -> Triple(card, top, counts) }
        .whenComplete { data, error ->
          mod.server.execute {
            if (player.hasDisconnected()) return@execute
            if (error != null) return@execute mod.log.warn("Can't open the menu for ${player.plainTextName}: ${error.message}")
            val (card, top, counts) = data
            build(mod, player, card, top, counts)
          }
        }
    }

    private fun build(mod: MoBosses, player: ServerPlayer, card: Stats.Card, top: List<Hunter>, counts: List<BossCount>) {
      val languages = mod.languages
      val code = mod.texts.languageOf(player)
      val source = player.createCommandSourceStack()
      val canReset = Perms.check(source, Permissions.RESET)
      val actions = buildMap {
        if (canReset) put(RESET, Action.RESET)
        put(BESTIARY, Action.BESTIARY)
        put(LANGUAGE, Action.LANGUAGE)
        if (Perms.check(source, Permissions.BOSSES)) put(BOSS_LIST, Action.BOSS_LIST)
        if (Perms.check(source, Permissions.RELOAD)) put(RELOAD, Action.RELOAD)
        if (Perms.check(source, Permissions.KILLALL)) put(KILLALL, Action.KILLALL)
        put(CLOSE, Action.CLOSE)
      }
      val menu = Menu(mod, player, actions)
      if (canReset) menu.confirmReset = menu.item(Items.TNT, languages.lines(code, Message.MENU_RESET_CONFIRM))

      val resolvers = mod.menuStats.resolvers(code, card)
      menu.set(YOU, menu.name(ItemStack(Items.PLAYER_HEAD).apply { set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.gameProfile)) },
        languages.render(code, Message.MENU_YOU, *resolvers), languages.lines(code, Message.INFO, *resolvers).drop(1)))
      val lines = top.mapIndexed { i, hunter ->
        languages.render(code, Message.TOP_LINE, Placeholder.unparsed("rank", (i + 1).toString()), Placeholder.unparsed("player", mod.nameOf(hunter)),
          Placeholder.unparsed("level", mod.menuStats.level(hunter.progress).toString()), Placeholder.unparsed("kills", hunter.progress.kills.toString()))
      }.filterNotNull().ifEmpty { listOfNotNull(languages.render(code, Message.TOP_EMPTY)) }
      menu.set(TOP, menu.name(ItemStack(Items.GOLDEN_HELMET), languages.render(code, Message.MENU_TOP), lines))
      val byClass = counts.groupBy { it.clazz }
      val classLines = mod.catalog.classes.all.mapNotNull { clazz ->
        val rows = byClass[clazz.key].orEmpty()
        languages.render(code, Message.MENU_BOSSES_LINE, Placeholder.component("class", mod.texts.className(code, clazz)),
          Placeholder.unparsed("spawned", rows.sumOf { it.spawned }.toString()), Placeholder.unparsed("killed", rows.sumOf { it.killed }.toString()))
      }
      menu.set(BOSSES, menu.name(ItemStack(Items.ZOMBIE_HEAD), languages.render(code, Message.MENU_BOSSES, Placeholder.unparsed("alive", mod.bosses.all.size.toString())), classLines))
      if (canReset) menu.set(RESET, menu.item(Items.RECOVERY_COMPASS, languages.lines(code, Message.MENU_RESET)))
      menu.set(BESTIARY, menu.item(Items.WRITABLE_BOOK, languages.lines(code, Message.MENU_BESTIARY)))
      menu.set(LANGUAGE, menu.item(Items.GLOBE_BANNER_PATTERN, languages.lines(code, Message.MENU_LANGUAGE, Placeholder.unparsed("language", LanguageChoices.nativeName(code)))))
      if (Action.BOSS_LIST in actions.values) menu.set(BOSS_LIST, menu.item(Items.SPYGLASS, languages.lines(code, Message.MENU_BOSS_LIST)))
      if (Action.RELOAD in actions.values) menu.set(RELOAD, menu.name(ItemStack(Items.REPEATER), languages.render(code, Message.MENU_RELOAD), emptyList()))
      if (Action.KILLALL in actions.values) menu.set(KILLALL, menu.name(ItemStack(Items.BARRIER), languages.render(code, Message.MENU_KILLALL), emptyList()))
      menu.set(CLOSE, menu.name(ItemStack(Items.OAK_DOOR), languages.render(code, Message.MENU_CLOSE), emptyList()))
      menu.open(languages.render(code, Message.MENU_TITLE) ?: Component.text("Mo’Bosses"))
    }
  }
}
