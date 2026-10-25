package ru.mairwunnx.mobosses.modcore.hooks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.mobosses.modcore.MoBosses

/**
 * `<papi:name>` in a message, as on Paper: a tag for one placeholder, never `%…%` replaced over a whole text. The mods
 * have no placeholder mod yet, so the tag renders empty, as Paper's does without PlaceholderAPI.
 */
object Placeholders {
  @Suppress("UNUSED_PARAMETER")
  fun resolver(mod: MoBosses, player: ServerPlayer?): TagResolver = TagResolver.resolver("papi") { _, _ -> Tag.selfClosingInserting(Component.empty()) }
}
