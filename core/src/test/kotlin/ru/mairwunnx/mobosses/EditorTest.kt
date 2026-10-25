package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.config.AbilitySettings
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.config.edit.EditableFile
import ru.mairwunnx.mobosses.config.edit.Editor
import ru.mairwunnx.mobosses.config.edit.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The settings screen's editor: one value on its own line, comments kept, nothing it can't read back. */
class EditorTest {
  private fun bundled(name: String) = Settings::class.java.classLoader.getResource(name)!!.readText()
  private val config = EditableFile.named("config.yml")!!
  private val abilities = EditableFile.named("abilities.yml")!!

  @Test fun `every file lists its values with their notes`() {
    EditableFile.ALL.forEach { file -> assertTrue(file.fields(bundled(file.name)).size > 20, file.name) }
    val normal = config.fields(bundled("config.yml")).single { it.key == "difficulty.modifiers.normal" }
    assertEquals(Field.Kind.DOUBLE, normal.kind)
    assertEquals("0.8777777773", normal.value)
    val flag = config.fields(bundled("config.yml")).single { it.key == "anti_cheese.vehicles" }
    assertTrue("boats" in flag.note, flag.note)
  }

  @Test fun `a value changes on its own line and every other line stays`() {
    val text = bundled("config.yml")
    val done = assertIs<Editor.Result.Done>(Editor.set(config, text, listOf("anti_cheese", "step_out", "stuck_seconds"), "9"))
    val before = text.lines()
    val after = done.text.lines()
    assertEquals(before.size, after.size)
    val changed = before.indices.filter { before[it] != after[it] }
    assertEquals(1, changed.size)
    assertEquals("    stuck_seconds: 9", after[changed.single()])
    assertEquals(9, Settings.parse(done.text).antiCheese.stepOut.stuckSeconds)
  }

  @Test fun `a comment on the line stays where it was`() {
    val done = assertIs<Editor.Result.Done>(Editor.set(config, bundled("config.yml"), listOf("anti_cheese", "vehicles"), "false"))
    assertTrue(done.text.lines().any { it.startsWith("  vehicles: false") && it.endsWith("# Bosses never get into boats or minecarts.") })
  }

  @Test fun `a flow map keeps its one line`() {
    val text = bundled("abilities.yml")
    val done = assertIs<Editor.Result.Done>(Editor.set(abilities, text, listOf("signature", "swap", "cooldown"), "20000"))
    assertEquals(text.lines().size, done.text.lines().size)
    assertTrue(done.text.lines().any { it.startsWith("  swap: { enabled: true, cooldown: 20000, cooldown_bias: 1.3") && it.contains("# trades places") })
    assertEquals(20000L, AbilitySettings.parse(done.text).signatures.getValue("swap").cooldown)
  }

  @Test fun `a key the file lacks comes in where it belongs`() {
    val text = "enabled: true\nspawn:\n  enabled: true\n\ndebug: false\n"
    val done = assertIs<Editor.Result.Done>(Editor.set(config, text, listOf("spawn", "max_nearby"), "5"))
    assertEquals(5, Settings.parse(done.text).spawn.maxNearby)
    val deep = assertIs<Editor.Result.Done>(Editor.set(config, text, listOf("difficulty", "modifiers", "easy"), "0.5"))
    assertEquals(0.5, Settings.parse(deep.text).difficulty.modifiers.easy)
    assertTrue(Settings.parse(deep.text).debug.not())
  }

  @Test fun `the wrong kind or an unknown key is refused`() {
    val text = bundled("config.yml")
    assertIs<Editor.Result.Refused>(Editor.set(config, text, listOf("debug"), "maybe"))
    assertIs<Editor.Result.Refused>(Editor.set(config, text, listOf("spawn", "max_nearby"), "three"))
    assertIs<Editor.Result.Refused>(Editor.set(config, text, listOf("no", "such"), "1"))
  }

  @Test fun `strings come back quoted and whole`() {
    val done = assertIs<Editor.Result.Done>(Editor.set(config, bundled("config.yml"), listOf("locales", "default"), "ru"))
    assertEquals("ru", Settings.parse(done.text).locales.default)
  }
}
