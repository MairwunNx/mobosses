package ru.mairwunnx.mobosses.config

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
}
