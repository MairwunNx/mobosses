package ru.mairwunnx.mobosses

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.LevelProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelProgressTest {
  private val languages = Languages(
    mapOf("en" to mapOf("info_next" to "To level <next> <progress>/<needed> · <percent>%", "info_top" to "The top level")),
    "en",
  )

  private fun line(experience: Long) =
    LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize("<next_level>", LevelProgress.resolver(languages, "en", Settings.LevelCurve(), experience)))

  @Test fun `a card shows the way to the next level`() {
    // Level 20 starts at 14,545 and level 21 at 16,143.
    assertEquals("To level 21 799/1\u00A0598 · 50%", line(15_344))
    assertEquals("The top level", line(Long.MAX_VALUE / 2))
  }

  @Test fun `numbers come in thousands`() {
    assertEquals("1\u00A0047\u00A0042", LevelProgress.grouped(1_047_042))
    assertEquals("80", LevelProgress.grouped(80))
  }
}
