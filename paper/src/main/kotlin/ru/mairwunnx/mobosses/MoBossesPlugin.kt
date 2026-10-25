package ru.mairwunnx.mobosses

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.mobosses.ability.Abilities
import ru.mairwunnx.mobosses.ability.AbilityEntities
import ru.mairwunnx.mobosses.ability.ArrowListener
import ru.mairwunnx.mobosses.ability.kinds.Chain
import ru.mairwunnx.mobosses.ability.kinds.Obelisk
import ru.mairwunnx.mobosses.ability.kinds.MeteorListener
import ru.mairwunnx.mobosses.boss.BossBars
import ru.mairwunnx.mobosses.boss.Bosses
import ru.mairwunnx.mobosses.boss.Catalog
import ru.mairwunnx.mobosses.boss.Decoys
import ru.mairwunnx.mobosses.hooks.WorldApiLink
import ru.mairwunnx.mobosses.hooks.AllayLink
import ru.mairwunnx.mobosses.achievement.Achievements
import ru.mairwunnx.mobosses.boss.Despawns
import ru.mairwunnx.mobosses.boss.Keys
import ru.mairwunnx.mobosses.boss.Minions
import ru.mairwunnx.mobosses.boss.NameTags
import ru.mairwunnx.mobosses.boss.Visuals
import ru.mairwunnx.mobosses.combat.AntiCheese
import ru.mairwunnx.mobosses.combat.DamageListener
import ru.mairwunnx.mobosses.combat.DeathListener
import ru.mairwunnx.mobosses.combat.Loot
import ru.mairwunnx.mobosses.combat.Rewards
import ru.mairwunnx.mobosses.config.BundledFiles
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.Ids
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.hooks.Expansion
import ru.mairwunnx.mobosses.hooks.MiniPlaceholderTags
import ru.mairwunnx.mobosses.hooks.PlanData
import ru.mairwunnx.mobosses.hooks.Placeholders
import ru.mairwunnx.mobosses.lang.GeoIp
import ru.mairwunnx.mobosses.lang.LanguageFiles
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.lang.PlayerLanguages
import ru.mairwunnx.mobosses.lang.Texts
import ru.mairwunnx.mobosses.menu.MenuListener
import ru.mairwunnx.mobosses.menu.Stats
import ru.mairwunnx.mobosses.progress.Progression
import ru.mairwunnx.mobosses.progress.ProgressionListener
import ru.mairwunnx.mobosses.protect.BlockChanges
import ru.mairwunnx.mobosses.protect.BlockLog
import ru.mairwunnx.mobosses.protect.BlockLogListener
import ru.mairwunnx.mobosses.protect.Guards
import ru.mairwunnx.mobosses.protect.TemporaryBlockListener
import ru.mairwunnx.mobosses.protect.WorldGuardFlags
import ru.mairwunnx.mobosses.spawn.SpawnListener
import ru.mairwunnx.mobosses.spawn.SurvivalBonus
import ru.mairwunnx.mobosses.spawn.FullMoon
import ru.mairwunnx.mobosses.combat.GroupHealth
import ru.mairwunnx.mobosses.combat.SpawnerFatigue
import ru.mairwunnx.mobosses.progress.DeathPenalty
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
  val playerLanguages = PlayerLanguages(this)
  val texts = Texts(this)
  val progression = Progression(log, { catalog.settings }, { server.onlinePlayers.map { it.uniqueId } }) { server.scheduler.runTask(this, it) }
  val bosses = Bosses(this)
  val minions = Minions(this)
  val nameTags = NameTags(this)
  val bars = BossBars(this)
  val visuals = Visuals(this)
  val abilities = Abilities(this)
  val loot = Loot(this)
  val rewards = Rewards(this)
  val spawns = SpawnListener(this)
  val despawns = Despawns(this)
  val decoys = Decoys(this)
  val achievements = Achievements(this)
  val worldApi = WorldApiLink(this)
  val allay = AllayLink(this)
  val survival = SurvivalBonus(this)
  val fullMoon = FullMoon(this)
  val groupHealth = GroupHealth(this)
  val fatigue = SpawnerFatigue(this)
  val antiCheese = AntiCheese(this)
  val blockChanges = BlockChanges(this)
  val menuStats = Stats(this)

  private val tasks = mutableListOf<BukkitTask>()
  private var geoIpTask: BukkitTask? = null
  private var expansion: Expansion? = null
  private var miniPlaceholders: AutoCloseable? = null

  override fun onLoad() {
    // WorldGuard takes new flags only while plugins load.
    if (server.pluginManager.getPlugin("WorldGuard") != null) WorldGuardFlags.register(log)
  }

  override fun onEnable() {
    if (!reload()) log.warn("Started with errors in the config files; fix them and run /mobosses reload")
    listOf(playerLanguages, ProgressionListener(progression), spawns, survival, DeathPenalty(this), achievements, DamageListener(this), DeathListener(this), antiCheese, MeteorListener(this), ArrowListener(this), AbilityEntities(this), BlockLogListener(this), TemporaryBlockListener(this), MenuListener())
      .forEach { server.pluginManager.registerEvents(it, this) }
    registerCommand("mobosses", "Your Mo’Bosses level and the best hunters; for admins, reload, spawn, or remove bosses", listOf("mb", "moboss"), MoBossesCommand(this))
    tasks += server.scheduler.runTaskTimer(this, Runnable { visuals.tick(); bars.tick(); antiCheese.tick(); PlanData.bossesAlive = bosses.all.size }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { nameTags.tick(); spawns.natural.tick() }, 10L, 10L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { despawns.tick() }, 100L, 100L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { survival.tick(); fullMoon.tick() }, 1200L, 1200L)

    // What the last run left: cobwebs after a crash, and bosses and minions in chunks already loaded.
    blockChanges.temporary.recover()
    server.worlds.forEach { world -> world.entities.forEach { entity -> bosses.adopt(entity)?.let(bosses::vanillaName) ?: minions.adopt(entity) } }
    if (Placeholders.available) {
      expansion = Expansion(this).also { it.register() }
      log.info("PlaceholderAPI found: %mobosses_level%, %mobosses_xp%, and %mobosses_kills% work, and so do <papi:…> tags in messages")
    }
    if (PlanData.available) PlanData.register(this)
    worldApi.connect()
    allay.connect()
    if (MiniPlaceholderTags.available) {
      miniPlaceholders = MiniPlaceholderTags.register(this)
      log.info("MiniPlaceholders found: its tags work in messages, and <mobosses_level>, <mobosses_xp>, <mobosses_kills> work elsewhere")
    }
    log.info("Mo’Bosses ${pluginMeta.version} is on: ${catalog.types.size} boss types, ${languages.codes.sorted().joinToString()}, default ${resolver.default}; protection: ${guards.name}")
  }

  override fun onDisable() {
    tasks.forEach(BukkitTask::cancel)
    geoIpTask?.cancel()
    expansion?.unregister()
    worldApi.disconnect()
    allay.disconnect()
    miniPlaceholders?.close()
    // Cobwebs and the like go back now, or they'd stay in the world for good.
    blockChanges.temporary.restoreAll()
    Obelisk.removeAll()
    Chain.clear()
    decoys.removeAll()
    nameTags.clearAll()
    bars.clear()
    bosses.forgetAll()
    progression.close()
    geoIp.close()
  }

  /** Reads every config and language file again, keeping the plugin and its bosses running. False when a file had errors. */
  fun reload(): Boolean {
    (Configs.FILES + Languages.BUNDLED.map { "lang/$it.yml" }).forEach(::file)
    val configs = Configs.fromFolder(dataFolder, ::bundled, log::warn, log::error)
    var ok = configs.ok
    val texts = LanguageFiles.load(dataFolder.resolve("lang"), ::bundled, log) { ok = false }
    languages = Languages(texts, LocaleResolver.normalize(configs.defaultLanguage))
    resolver = LocaleResolver(configs.settings.locales, texts.keys, configs.defaultLanguage)
    if (LocaleResolver.normalize(configs.defaultLanguage) !in texts) log.warn("locales.default: no lang/${configs.defaultLanguage}.yml, using ${resolver.default}")

    catalog = Catalog(configs, Ids(log), log)
    guards = Guards.load(configs.settings.protection, log)
    blockLog = BlockLog.load(configs.settings.coreprotect, log)
    setUpGeoIp(configs.settings)
    progression.open(configs.settings.database, dataFolder)

    // Titles, labels, and names may read differently now.
    bars.clear()
    nameTags.clearAll()
    bosses.all.forEach(bosses::vanillaName)
    // Loads any the server doesn't have yet, as when they were just switched on.
    achievements.load()
    return ok
  }

  private fun setUpGeoIp(settings: Settings) {
    geoIpTask?.cancel()
    val config = settings.geoip
    if (!config.enabled || Settings.Source.IP !in settings.locales.sources) return geoIp.close()
    geoIp.open()
    // Checks twice a day; downloads only when the file on disk has aged past refresh_days.
    geoIpTask = server.scheduler.runTaskTimerAsynchronously(this, Runnable { if (geoIp.isStale(config)) geoIp.refresh(config) }, 0L, 20L * 60 * 60 * 12)
  }

  private fun bundled(path: String): String = getResource(path)!!.use { it.readBytes().toString(Charsets.UTF_8) }

  private val shipped by lazy { BundledFiles.parse(bundled(BundledFiles.LIST)) }

  private fun file(path: String): File = BundledFiles.copy(dataFolder, path, ::bundled, shipped, log)
}
