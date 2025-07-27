package ru.mairwunnx.mobosses.framework

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemStack

fun platformItemTextFrom(stack: ItemStack): Component {
  val meta = stack.itemMeta
  val legacyName = meta?.displayName?.ifEmpty { "'загадочная хуйня'" } ?: "'загадочная хуйня'"

  return if (legacyName.isNotEmpty()) {
    LegacyComponentSerializer.legacySection().deserialize(legacyName)
  } else {
    val pretty = stack.type.name.ifEmpty { "'загадочная хуйня'" }
      .lowercase().replace('_', ' ')
      .replaceFirstChar(Char::titlecase)
    Component.text(pretty, NamedTextColor.WHITE)
  }
}