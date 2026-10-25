package ru.mairwunnx.mobosses

import java.util.logging.Level
import java.util.logging.Logger

/** Mo’Bosses' log, whichever logger the platform gives it; debug lines show only with `debug: true`. */
class Log(private val sink: Sink, private val debugEnabled: () -> Boolean) {
  /** Where the lines go: Paper's logger, or the game's on the mods. */
  interface Sink {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, cause: Throwable?)
  }

  constructor(logger: Logger, debugEnabled: () -> Boolean) : this(object : Sink {
    override fun info(message: String) = logger.info(message)
    override fun warn(message: String) = logger.warning(message)
    override fun error(message: String, cause: Throwable?) = logger.log(Level.SEVERE, message, cause)
  }, debugEnabled)

  fun info(message: String) = sink.info(message)
  fun warn(message: String) = sink.warn(message)
  fun error(message: String, cause: Throwable? = null) = sink.error(message, cause)

  inline fun debug(message: () -> String) {
    if (isDebug()) info("[debug] ${message()}")
  }

  fun isDebug() = debugEnabled()
}
