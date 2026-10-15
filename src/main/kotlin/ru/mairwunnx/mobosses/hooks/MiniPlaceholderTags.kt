package ru.mairwunnx.mobosses.hooks

import io.github.miniplaceholders.api.Expansion
import io.github.miniplaceholders.api.MiniPlaceholders
import io.github.miniplaceholders.api.resolver.AudienceTagResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.mairwunnx.mobosses.MoBossesPlugin

/**
 * MiniPlaceholders both ways, when it's there. Messages resolve other plugins' tags, like
 * `<luckperms_prefix>`, for the player who reads them. And `<mobosses_level>`, `<mobosses_xp>`, and
 * `<mobosses_kills>` work in TAB, scoreboards, and other plugins. Those may be asked from any thread,
 * so they read only the progress cache.
 */
object MiniPlaceholderTags {
  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")

  /** Other plugins' global and per-player tags; nothing without MiniPlaceholders. */
  fun resolver(): TagResolver =
    // Only touch MiniPlaceholders' classes when it's there; they aren't on the classpath otherwise.
    if (available) MiniPlaceholders.audienceGlobalPlaceholders() else TagResolver.empty()

  /** Registers `<mobosses_…>`. Call only when [available]. Returns a handle that unregisters it. */
  fun register(plugin: MoBossesPlugin): AutoCloseable {
    fun tag(value: (Player) -> Any) = AudienceTagResolver<Player> { player, _, _ -> Tag.selfClosingInserting(Component.text(value(player).toString())) }
    val expansion = Expansion.builder("mobosses")
      .author("MairwunNx")
      .version(plugin.pluginMeta.version)
      .audiencePlaceholder(Player::class.java, "level", tag { plugin.progression.level(it.uniqueId) })
      .audiencePlaceholder(Player::class.java, "xp", tag { plugin.progression.of(it.uniqueId).experience })
      .audiencePlaceholder(Player::class.java, "kills", tag { plugin.progression.of(it.uniqueId).kills })
      .build()
    expansion.register()
    return AutoCloseable { expansion.unregister() }
  }
}
