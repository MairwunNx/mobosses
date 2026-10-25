package ru.mairwunnx.mobosses.config

import kotlinx.serialization.Serializable

/** `minions.yml`: what each boss type summons. */
@Serializable
data class MinionSettings(val minions: List<Minion> = emptyList()) {
  @Serializable
  data class Minion(
    val summoner: String = "",
    val entity: String = "",
    /** 1.x's name for the minion, shown to everyone. Leave it out to use `minions.<summoner>` from the language files. */
    val name: String? = null,
  )

  companion object {
    fun parse(text: String, warn: (String) -> Unit = {}): MinionSettings = Yamls.decode(serializer(), text, warn)
  }
}
