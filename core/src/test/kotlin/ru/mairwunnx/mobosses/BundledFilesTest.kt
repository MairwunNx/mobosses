package ru.mairwunnx.mobosses

import ru.mairwunnx.mobosses.config.BundledFiles
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.lang.Languages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundledFilesTest {
  private fun resource(path: String) = javaClass.classLoader.getResource(path)!!.readText()
  private val shipped = BundledFiles.parse(resource(BundledFiles.LIST))
  private val paths = Configs.FILES + Languages.BUNDLED.map { "lang/$it.yml" }

  @Test fun `this version's copies are listed`() {
    val missing = paths.filter { BundledFiles.hash(resource(it)) !in shipped[it].orEmpty() }
      .map { "${BundledFiles.hash(resource(it))}  $it" }
    assertEquals(emptyList(), missing, "add these lines to ${BundledFiles.LIST}")
  }

  @Test fun `an older copy nobody changed gets replaced`() {
    val list = BundledFiles.parse("# a comment\n${BundledFiles.hash("old: 1\n")}  loottable.yml\n")
    assertTrue(BundledFiles.outdated("loottable.yml", "old: 1\n", "new: 2\n", list))
    assertTrue(BundledFiles.outdated("loottable.yml", "old: 1\r\n", "new: 2\n", list), "Windows line ends count as the same file")
    assertFalse(BundledFiles.outdated("loottable.yml", "old: 1\n# mine\n", "new: 2\n", list), "an edited file stays")
    assertFalse(BundledFiles.outdated("bosses.yml", "old: 1\n", "new: 2\n", list), "a copy of another file doesn't count")
    assertFalse(BundledFiles.outdated("loottable.yml", "old: 1\n", "old: 1\n", list), "the current copy stays")
  }

  @Test fun `2_0_0's loot table is known`() {
    // The copy Moonlands kept from its first start on 2.1.0: 2.7.0 still dropped its stacks of cobblestone.
    assertTrue("6dded5af912ccdcaed849aa2661dd9903d98a307a9eab0d98f7a0f6f2151b512" in shipped.getValue("loottable.yml"))
  }
}
