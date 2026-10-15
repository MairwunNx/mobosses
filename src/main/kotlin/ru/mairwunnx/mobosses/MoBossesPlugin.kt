package ru.mairwunnx.mobosses

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.ability.Abilities
import ru.mairwunnx.mobosses.boss.BossBars
import ru.mairwunnx.mobosses.boss.Bosses
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.boss.CombatListener
import ru.mairwunnx.mobosses.boss.Keys
import ru.mairwunnx.mobosses.boss.Loot
import ru.mairwunnx.mobosses.boss.Rewards
import ru.mairwunnx.mobosses.boss.SpawnListener
import ru.mairwunnx.mobosses.boss.Visuals
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.Ids
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.config.Yamls
import ru.mairwunnx.mobosses.hooks.Expansion
import ru.mairwunnx.mobosses.hooks.Placeholders
import ru.mairwunnx.mobosses.lang.GeoIp
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.lang.Texts
import ru.mairwunnx.mobosses.progress.Progression
import ru.mairwunnx.mobosses.progress.Store
import ru.mairwunnx.mobosses.protect.BlockChanges
import ru.mairwunnx.mobosses.protect.BlockLog
import ru.mairwunnx.mobosses.protect.Guards
import ru.mairwunnx.mobosses.protect.WorldGuardFlags
import java.io.File

class MoBossesPlugin : JavaPlugin() {
  @Volatile lateinit var catalog: Catalog; private set
  @Volatile lateinit var languages: Languages; private set
  @Volatile lateinit var resolver: LocaleResolver; private set
  lateinit var guards: Guards; private set
  lateinit var blockLog: BlockLog; private set

  val log = Log(logger) { ::catalog.isInitialized && catalog.settings.debug }
  val keys by lazy { Keys(this) }
  val geoIp by lazy { GeoIp(dataFolder.resolve("geoip"), log) }
  val texts = Texts(this)
  val progression = Progression(this)
  val bosses = Bosses(this)
  val bars = BossBars(this)
  val visuals = Visuals(this)
  val abilities = Abilities(this)
  val loot = Loot(this)
  val rewards = Rewards(this)
  val spawns = SpawnListener(this)
  val blockChanges = BlockChanges(this)

  private var databaseSettings: Settings.Database? = null
  private var ticker: BukkitTask? = null
  private var geoIpTask: BukkitTask? = null
  private var expansion: Expansion? = null

  override fun onLoad() {
    // WorldGuard takes new flags only while plugins load.
    if (server.pluginManager.getPlugin("WorldGuard") != null) WorldGuardFlags.register(log)
  }

  override fun onEnable() {
    if (!reload()) log.warn("Started with errors in the config files; fix them and run /mobosses reload")
    listOf(texts, progression, spawns, CombatListener(this)).forEach { server.pluginManager.registerEvents(it, this) }
    registerCommand("mobosses", "Reload Mo’Bosses, spawn a boss, or remove every boss", listOf("mb", "moboss"), MoBossesCommand(this))
    ticker = server.scheduler.runTaskTimer(this, Runnable {
      visuals.tick()
      bars.tick()
    }, 20L, 20L)

    // Bosses from before a restart, in chunks that are already loaded.
    server.worlds.forEach { world -> world.livingEntities.forEach { bosses.adopt(it)?.let(bosses::nameTag) } }
    if (Placeholders.available) {
      expansion = Expansion(this).also { it.register() }
      log.info("PlaceholderAPI found: %mobosses_level%, %mobosses_xp%, and %mobosses_kills% work, and so do <papi:…> tags in messages")
    }
    log.info("Mo’Bosses ${pluginMeta.version} is on: ${catalog.types.size} boss types, ${languages.codes.sorted().joinToString()}, default ${resolver.default}; protection: ${guards.name}")
  }

  override fun onDisable() {
    ticker?.cancel()
    geoIpTask?.cancel()
    expansion?.unregister()
    // Cobwebs and the like go back now, or they'd stay in the world for good.
    blockChanges.restoreAll()
    bars.clear()
    bosses.forgetAll()
    progression.store?.close()
    progression.store = null
    geoIp.close()
  }

  /** Reads every config and language file again, keeping the plugin and its bosses running. False when a file had errors. */
  fun reload(): Boolean {
    Configs.FILES.forEach(::file)
    val configs = Configs.fromFolder(dataFolder, ::bundled, log::warn, log::error)
    var ok = configs.ok

    val texts = loadLanguages { ok = false }
    languages = Languages(texts, LocaleResolver.normalize(configs.defaultLanguage))
    resolver = LocaleResolver(configs.settings.locales, texts.keys, configs.defaultLanguage)
    if (LocaleResolver.normalize(configs.defaultLanguage) !in texts) log.warn("locales.default: no lang/${configs.defaultLanguage}.yml, using ${resolver.default}")

    catalog = Catalog(configs, Ids(log), log)
    guards = Guards.load(configs.settings.protection, log)
    blockLog = BlockLog.load(configs.settings.coreprotect, log)

    setUpGeoIp(configs.settings)
    if (configs.settings.database != databaseSettings) openDatabase(configs.settings.database)

    // Titles and name tags may read differently now.
    bars.clear()
    bosses.all.forEach(bosses::nameTag)
    return ok
  }

  /** The bundled languages, overlaid key by key with the files in `lang/`, plus any extra languages found there. */
  private fun loadLanguages(onError: () -> Unit): Map<String, Map<String, String>> {
    Languages.BUNDLED.forEach { file("lang/$it.yml") }
    val bundled = Languages.BUNDLED.associateWith { code -> Yamls.flatten(Yamls.parse(bundled("lang/$code.yml"))) }
    val custom = dataFolder.resolve("lang").listFiles { f -> f.isFile && f.extension == "yml" }.orEmpty().associate { f ->
      LocaleResolver.normalize(f.nameWithoutExtension) to runCatching { Yamls.flatten(Yamls.parse(f.readText())) }
        .onFailure { onError(); log.error("lang/${f.name} has an error, skipping it: ${it.message?.substringBefore('\n')}") }
        .getOrDefault(emptyMap())
    }
    return (bundled.keys + custom.keys).associateWith { bundled[it].orEmpty() + custom[it].orEmpty() }
  }

  /** Opens the database off the server thread: a remote one can take a while to answer. */
  private fun openDatabase(settings: Settings.Database) {
    databaseSettings = settings
    val old = progression.store
    progression.store = null
    Thread({
      old?.close()
      try {
        val store = Store(settings, dataFolder)
        progression.store = store
        log.info("Progress is stored in ${store.description}")
        progression.loadOnline()
      } catch (e: Exception) {
        log.error("Can't open the database; players' progress lasts only until they leave: ${e.message}")
      }
    }, "Mo’Bosses database").start()
  }

  private fun setUpGeoIp(settings: Settings) {
    geoIpTask?.cancel()
    val config = settings.geoip
    if (!config.enabled || Settings.Source.IP !in settings.locales.sources) {
      geoIp.close()
      return
    }
    geoIp.open()
    // Checks twice a day; downloads only when the file on disk has aged past refresh_days.
    geoIpTask = server.scheduler.runTaskTimerAsynchronously(this, Runnable { if (geoIp.isStale(config)) geoIp.refresh(config) }, 0L, 20L * 60 * 60 * 12)
  }

  private fun bundled(path: String): String = getResource(path)!!.use { it.readBytes().toString(Charsets.UTF_8) }

  /** A file in the data folder, copied from the jar first when it isn't there yet. */
  private fun file(path: String): File {
    val file = dataFolder.resolve(path)
    if (!file.exists()) saveResource(path, false)
    return file
  }
}
