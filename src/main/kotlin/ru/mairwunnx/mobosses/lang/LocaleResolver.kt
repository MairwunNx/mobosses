package ru.mairwunnx.mobosses.lang

import ru.mairwunnx.mobosses.config.Settings
import java.util.Locale

/** Picks a player's language from their country and their game's language setting. */
class LocaleResolver(private val settings: Settings.Locales, private val available: Set<String>, defaultCode: String = settings.default) {
  private val byCountry: Map<String, String> = buildMap {
    settings.countries.forEach { (code, countries) -> countries.forEach { putIfAbsent(it.uppercase(), normalize(code)) } }
  }

  val default: String = normalize(defaultCode).takeIf { it in available } ?: Languages.FALLBACK

  fun resolve(country: String?, client: Locale?): String {
    for (source in settings.sources) {
      val code = when (source) {
        Settings.Source.IP -> country?.let { byCountry[it.uppercase()] }
        Settings.Source.CLIENT -> client?.let(::fromClient)
      }
      if (code != null && code in available) return code
    }
    return default
  }

  /** `pt_BR` matches a `pt_br` file first, then `pt`. */
  private fun fromClient(locale: Locale): String? {
    val full = normalize(locale.toString())
    val language = normalize(locale.language)
    return when {
      full in available -> full
      language in available -> language
      else -> null
    }
  }

  companion object {
    fun normalize(code: String) = code.trim().lowercase().replace('-', '_')
  }
}
