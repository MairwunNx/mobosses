package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.lang.LanguageChoices
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageChoicesTest {
  private val codes = listOf("en", "ru", "es", "pt", "pt_br", "de")

  @Test fun `a typed code finds its language, the full one first`() {
    assertEquals("ru", LanguageChoices.match("RU", codes))
    assertEquals("pt_br", LanguageChoices.match("pt-BR", codes))
    assertEquals("es", LanguageChoices.match("es_MX", codes))
    assertNull(LanguageChoices.match("it", codes))
  }

  @Test fun `choices survive a reload, and auto drops one`() {
    val file = Files.createTempDirectory("mobosses").resolve("player-languages.yml").toFile()
    val alex = UUID.randomUUID()
    val steve = UUID.randomUUID()
    LanguageChoices(file).apply { load(); set(alex, "pt_BR"); set(steve, "de") }

    val again = LanguageChoices(file).apply { load() }
    assertEquals("pt_br", again.of(alex))
    assertEquals("de", again.of(steve))

    again.set(steve, null)
    val third = LanguageChoices(file).apply { load() }
    assertNull(third.of(steve))
    assertEquals("pt_br", third.of(alex))
  }

  @Test fun `a broken line costs only itself`() {
    val file = Files.createTempFile("mobosses", ".yml").toFile()
    val alex = UUID.randomUUID()
    file.writeText("# comment\nnot a uuid: ru\n$alex: \"ru\"\n")
    val choices = LanguageChoices(file).apply { load() }
    assertEquals("ru", choices.of(alex))
  }

  @Test fun `bundled languages show their own names`() {
    assertEquals("Русский", LanguageChoices.nativeName("ru"))
    assertEquals("it", LanguageChoices.nativeName("it"))
  }
}
