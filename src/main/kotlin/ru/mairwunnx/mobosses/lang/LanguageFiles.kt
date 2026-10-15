package ru.mairwunnx.mobosses.lang

import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.Yamls
import java.io.File

/** The language files: the bundled ones, overlaid key by key with those in `lang/`, plus any extra languages found there. */
object LanguageFiles {
  /** False through [onError] when a file in `lang/` can't be read; it's skipped and the bundled texts stay. */
  fun load(folder: File, bundled: (String) -> String, log: Log, onError: () -> Unit): Map<String, Map<String, String>> {
    val ours = Languages.BUNDLED.associateWith { code -> Yamls.flatten(Yamls.parse(bundled("lang/$code.yml"))) }
    val theirs = folder.listFiles { f -> f.isFile && f.extension == "yml" }.orEmpty().associate { f ->
      LocaleResolver.normalize(f.nameWithoutExtension) to runCatching { Yamls.flatten(Yamls.parse(f.readText())) }
        .onFailure { onError(); log.error("lang/${f.name} has an error, skipping it: ${it.message?.substringBefore('\n')}") }
        .getOrDefault(emptyMap())
    }
    return (ours.keys + theirs.keys).associateWith { ours[it].orEmpty() + theirs[it].orEmpty() }
  }
}
