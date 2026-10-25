package ru.mairwunnx.mobosses.config

import ru.mairwunnx.mobosses.Log
import java.io.File
import java.security.MessageDigest

/**
 * The files the plugin copies into its folder keep up with new versions by themselves. The jar
 * lists every copy of them it ever shipped, since 2.0.0, in [LIST]. A file on disk equal to one of
 * those was never edited, so the plugin swaps it for this version's; a file someone changed stays.
 */
object BundledFiles {
  const val LIST = "bundled.sha256"

  /** SHA-256 of the text, with Windows line ends read as Unix ones. */
  fun hash(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.replace("\r\n", "\n").toByteArray()).joinToString("") { "%02x".format(it) }

  /** `<sha256>  <path>` lines, as `sha256sum` writes them, by path; `#` starts a comment. */
  fun parse(list: String): Map<String, Set<String>> = list.lineSequence()
    .map(String::trim)
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .map { it.split(Regex("\\s+"), limit = 2) }
    .filter { it.size == 2 }
    .groupBy({ it[1] }, { it[0].lowercase() })
    .mapValues { it.value.toSet() }

  /** True when [onDisk] is a copy of [path] an older version shipped, unchanged, and this version's [bundled] differs. */
  fun outdated(path: String, onDisk: String, bundled: String, shipped: Map<String, Set<String>>): Boolean {
    val ours = hash(onDisk)
    return ours != hash(bundled) && ours in shipped[path].orEmpty()
  }

  /**
   * The file at [path] in [folder], copied from the jar first when it isn't there yet, or again when it's an older
   * version's copy nobody changed: up to 2.7 the plugin copied files once and left them be.
   */
  fun copy(folder: File, path: String, bundled: (String) -> String, shipped: Map<String, Set<String>>, log: Log): File = folder.resolve(path).also {
    if (!it.exists()) {
      it.parentFile.mkdirs()
      it.writeText(bundled(path))
    } else if (outdated(path, it.readText(), bundled(path), shipped)) {
      it.writeText(bundled(path))
      log.info("$path: updated to this version’s, since it had no changes of yours")
    }
  }
}
