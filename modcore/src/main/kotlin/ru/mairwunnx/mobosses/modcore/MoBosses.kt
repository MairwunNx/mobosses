package ru.mairwunnx.mobosses.modcore

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import ru.mairwunnx.mobosses.Log
import ru.mairwunnx.mobosses.config.BundledFiles
import ru.mairwunnx.mobosses.config.Configs
import ru.mairwunnx.mobosses.config.Settings
import ru.mairwunnx.mobosses.lang.GeoIp
import ru.mairwunnx.mobosses.lang.LanguageFiles
import ru.mairwunnx.mobosses.lang.Languages
import ru.mairwunnx.mobosses.lang.LocaleResolver
import ru.mairwunnx.mobosses.modcore.ability.Abilities
import ru.mairwunnx.mobosses.modcore.ability.AbilityEntities
import ru.mairwunnx.mobosses.modcore.ability.ArrowHits
import ru.mairwunnx.mobosses.modcore.ability.kinds.Chain
import ru.mairwunnx.mobosses.modcore.ability.kinds.Obelisk
import ru.mairwunnx.mobosses.modcore.achievement.Achievements
import ru.mairwunnx.mobosses.modcore.boss.BossBars
import ru.mairwunnx.mobosses.modcore.boss.Bosses
import ru.mairwunnx.mobosses.modcore.boss.Catalog
import ru.mairwunnx.mobosses.modcore.boss.Decoys
import ru.mairwunnx.mobosses.modcore.boss.Despawns
import ru.mairwunnx.mobosses.modcore.boss.Minions
import ru.mairwunnx.mobosses.modcore.boss.NameTags
import ru.mairwunnx.mobosses.modcore.boss.Visuals
import ru.mairwunnx.mobosses.modcore.combat.AntiCheese
import ru.mairwunnx.mobosses.modcore.combat.Damage
import ru.mairwunnx.mobosses.modcore.combat.DeathPenalty
import ru.mairwunnx.mobosses.modcore.combat.Deaths
import ru.mairwunnx.mobosses.modcore.combat.GroupHealth
import ru.mairwunnx.mobosses.modcore.combat.Loot
import ru.mairwunnx.mobosses.modcore.combat.Rewards
import ru.mairwunnx.mobosses.modcore.combat.SpawnerFatigue
import ru.mairwunnx.mobosses.modcore.config.Ids
import ru.mairwunnx.mobosses.modcore.hooks.AllayLink
import ru.mairwunnx.mobosses.modcore.hooks.Placeholders
import ru.mairwunnx.mobosses.modcore.hooks.WorldApiLink
import ru.mairwunnx.mobosses.modcore.lang.PlayerLanguages
import ru.mairwunnx.mobosses.modcore.lang.Texts
import ru.mairwunnx.mobosses.modcore.menu.Stats
import ru.mairwunnx.mobosses.modcore.protect.BlockChanges
import ru.mairwunnx.mobosses.modcore.protect.Claims
import ru.mairwunnx.mobosses.modcore.protect.Guards
import ru.mairwunnx.mobosses.modcore.spawn.FullMoon
import ru.mairwunnx.mobosses.modcore.spawn.Spawns
import ru.mairwunnx.mobosses.modcore.spawn.SurvivalBonus
import ru.mairwunnx.mobosses.progress.Hunter
import ru.mairwunnx.mobosses.progress.Progression
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mo’Bosses on one server the game runs, the twin of Paper's `MoBossesPlugin`: made when the server starts and
 * dropped when it stops, so leaving a singleplayer world frees everything. The configs live in the game's
 * `config/mobosses/`, the same for every world; the database and the temporary blocks in the world's own
 * `mobosses/`, so each world keeps its own hunters and moves with its save.
 */
class MoBosses(val server: MinecraftServer, val loader: Loader) {
  @Volatile lateinit var catalog: Catalog; private set
  @Volatile lateinit var languages: Languages; private set
  @Volatile lateinit var resolver: LocaleResolver; private set
  lateinit var guards: Guards; private set

  /** `config/mobosses/`: the configs, the language files, the players' languages, and the GeoIP database. */
  val folder: File = loader.config.resolve("mobosses").toFile()

  /** The world's `mobosses/`: its database and temporary blocks. */
  val data: File = server.getWorldPath(LevelResource.ROOT).resolve("mobosses").toFile()

  val debug get() = ::catalog.isInitialized && catalog.settings.debug

  private val logger = LoggerFactory.getLogger("Mo’Bosses")
  val log = Log(object : Log.Sink {
    override fun info(message: String) = logger.info(message)
    override fun warn(message: String) = logger.warn(message)
    override fun error(message: String, cause: Throwable?) = logger.error(message, cause)
  }) { debug }

  val scheduler = Scheduler()
  val geoIp by lazy { GeoIp(folder.resolve("geoip"), log) }
  val playerLanguages = PlayerLanguages(this)
  val texts = Texts(this)
  val progression = Progression(log, { catalog.settings }, { server.playerList.players.map { it.uuid } }) { server.execute(it) }
  val bosses = Bosses(this)
  val minions = Minions(this)
  val nameTags = NameTags(this)
  val bars = BossBars(this)
  val visuals = Visuals(this)
  val abilities = Abilities(this)
  val loot = Loot(this)
  val rewards = Rewards(this)
  val spawns = Spawns(this)
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
  val damage = Damage(this)
  val deaths = Deaths(this)
  val deathPenalty = DeathPenalty(this)
  val blockChanges = BlockChanges(this)
  val menuStats = Stats(this)
  val arrowHits = ArrowHits(this)
  val abilityEntities = AbilityEntities(this)

  /** Names of the players seen this session, for those who left before a boss they fought fell. */
  private val names = ConcurrentHashMap<UUID, String>()
  private var geoIpTask: Scheduler.Task? = null

  /** The server's clock in milliseconds: its ticks, which stand still while a singleplayer game pauses and go on after a restart. */
  fun now(): Long = server.overworld().gameTime * 50L

  fun enable() {
    if (!reload()) log.warn("Started with errors in the config files; fix them and run /mobosses reload")
    scheduler.every(20) { visuals.tick(); bars.tick(); antiCheese.tick() }
    scheduler.every(10) { nameTags.tick(); spawns.natural.tick() }
    scheduler.every(100) { despawns.tick() }
    scheduler.every(1200) { survival.tick(); fullMoon.tick() }

    // What the last run left: cobwebs after a crash, and bosses and minions in chunks already loaded.
    blockChanges.temporary.recover()
    server.allLevels.forEach { level -> level.allEntities.forEach { entity -> bosses.adopt(entity)?.let(bosses::vanillaName) ?: minions.adopt(entity) } }
    server.playerList.players.forEach { names[it.uuid] = it.plainTextName }
    worldApi.connect()
    allay.connect()
    if (Placeholders.register()) log.info("Text Placeholder API found: %mobosses:level%, %mobosses:xp%, and %mobosses:kills% work, and so do <papi:…> tags in messages")
    val side = if (server.isDedicatedServer) "a dedicated server" else "a singleplayer world"
    log.info("Mo’Bosses ${loader.version} for ${loader.name} is on $side: ${catalog.types.size} boss types, ${languages.codes.sorted().joinToString()}, default ${resolver.default}; protection: ${guards.name}")
  }

  fun disable() {
    geoIpTask?.cancel()
    worldApi.disconnect()
    allay.disconnect()
    // Cobwebs and the like go back now, or they'd stay in the world for good.
    if (::catalog.isInitialized) blockChanges.temporary.restoreAll()
    Obelisk.removeAll()
    Chain.clear()
    decoys.removeAll()
    nameTags.clearAll()
    bars.clear()
    bosses.forgetAll()
    scheduler.stop()
    progression.close()
    geoIp.close()
  }

  fun tick() = scheduler.tick()

  /** A player joined: their country and progress load off the server thread, as Paper's login does. */
  fun joined(player: ServerPlayer) {
    names[player.uuid] = player.plainTextName
    val address = PlayerLanguages.address(player)
    Thread({
      playerLanguages.login(player.uuid, address)
      progression.login(player.uuid, player.plainTextName)
      server.execute { if (!player.hasDisconnected()) achievements.joined(player) }
    }, "Mo’Bosses login").start()
  }

  fun left(player: ServerPlayer) {
    progression.quit(player.uuid)
    playerLanguages.quit(player.uuid)
    bars.hide(player)
  }

  /** Reads every config and language file again, keeping the mod and its bosses running. False when a file had errors. */
  fun reload(): Boolean {
    val shipped = BundledFiles.parse(bundled(BundledFiles.LIST))
    (Configs.FILES + Languages.BUNDLED.map { "lang/$it.yml" }).forEach { BundledFiles.copy(folder, it, ::bundled, shipped, log) }
    val configs = Configs.fromFolder(folder, ::bundled, log::warn, log::error)
    var ok = configs.ok
    val texts = LanguageFiles.load(folder.resolve("lang"), ::bundled, log) { ok = false }
    languages = Languages(texts, LocaleResolver.normalize(configs.defaultLanguage))
    resolver = LocaleResolver(configs.settings.locales, texts.keys, configs.defaultLanguage)
    if (LocaleResolver.normalize(configs.defaultLanguage) !in texts) log.warn("locales.default: no lang/${configs.defaultLanguage}.yml, using ${resolver.default}")

    catalog = Catalog(configs, Ids(server, log), log)
    guards = Guards(listOfNotNull(Claims.load(configs.settings.protection.claims, loader, log)))
    setUpGeoIp(configs.settings)
    progression.open(database(configs.settings.database), data)

    // Titles, labels, and names may read differently now.
    bars.clear()
    nameTags.clearAll()
    bosses.all.forEach(bosses::vanillaName)
    return ok
  }

  /** A singleplayer world keeps its hunters in its own save, whatever the config says about a server's database. */
  private fun database(settings: Settings.Database) = if (server.isDedicatedServer) settings else settings.copy(type = "sqlite")

  private fun setUpGeoIp(settings: Settings) {
    geoIpTask?.cancel()
    val config = settings.geoip
    // A singleplayer world's players are its owner and LAN guests, all at local addresses no country has.
    if (!config.enabled || Settings.Source.IP !in settings.locales.sources || !server.isDedicatedServer) return geoIp.close()
    geoIp.open()
    // Checks twice a day; downloads only when the file on disk has aged past refresh_days.
    geoIpTask = scheduler.every(20L * 60 * 60 * 12, 1) { Thread({ if (geoIp.isStale(config)) geoIp.refresh(config) }, "Mo’Bosses GeoIP").start() }
  }

  fun players(entity: Entity, radius: Double) = World.players(entity, radius)

  /** A player's name: online, seen this session, or as the server's name cache knows them. */
  fun nameOf(id: UUID): String? = server.playerList.getPlayer(id)?.plainTextName ?: names[id] ?: server.services().nameToIdCache().get(id).map { it.name() }.orElse(null)

  /** A hunter's name: the one saved with their last kill, or what the server knows. */
  fun nameOf(hunter: Hunter): String = hunter.name ?: runCatching { nameOf(UUID.fromString(hunter.uuid)) }.getOrNull() ?: hunter.uuid.take(8)

  /** The UUID of a player by name: online, or one the server has seen. */
  fun idOf(name: String): UUID? = server.playerList.getPlayerByName(name)?.uuid ?: server.services().nameToIdCache().get(name).map { it.id() }.orElse(null)

  companion object {
    /** A file the jar ships, from Mo’Bosses' own jar ([Loader.resource]). */
    fun bundled(path: String): String = (Mod.loader.resource(path) ?: error("$path isn't in Mo’Bosses' jar")).toString(Charsets.UTF_8)

    /**
     * The achievements' switch, language, and texts, read from the files before a server's Mo’Bosses starts: the
     * game builds its advancements first. Null when the files can't be read yet.
     */
    fun readLanguages(loader: Loader): Triple<Boolean, String, Languages>? = runCatching {
      val folder = loader.config.resolve("mobosses").toFile()
      val quiet = Log(object : Log.Sink {
        override fun info(message: String) {}
        override fun warn(message: String) {}
        override fun error(message: String, cause: Throwable?) {}
      }) { false }
      val shipped = BundledFiles.parse(bundled(BundledFiles.LIST))
      (Configs.FILES + Languages.BUNDLED.map { "lang/$it.yml" }).forEach { BundledFiles.copy(folder, it, ::bundled, shipped, quiet) }
      val configs = Configs.fromFolder(folder, ::bundled, {}, {})
      val texts = LanguageFiles.load(folder.resolve("lang"), ::bundled, quiet) {}
      val languages = Languages(texts, LocaleResolver.normalize(configs.defaultLanguage))
      Triple(configs.settings.achievements.enabled, configs.settings.achievements.language.ifBlank { languages.default }, languages)
    }.getOrNull()
  }
}
