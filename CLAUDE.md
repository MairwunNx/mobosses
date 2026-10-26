# Mo’Bosses: notes for AI agents

A Paper 26.3 plugin, and Fabric and NeoForge mods for dedicated servers and singleplayer and LAN worlds, that turn ordinary mobs into bosses of six classes, with abilities, minions, boss bars, loot, rewards, and players’ own Mo’Bosses levels, each player reading it in their own language, picked by IP. `docs/mods.md` describes the mods as built. README.md shows it off: what it gives, how to start, and links; README.RU.md is its Russian copy. The manual for server owners and developers is the GitHub wiki (`https://github.com/MairwunNx/mobosses.wiki.git`), in English only. Moonlands, the owner’s server (MairwunNx/minecraft), runs it since 2.1.0.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, the config files, and the English language file are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `lang/ru.yml` follow Russian typography; `lang/de.yml` uses „du“. Change both READMEs and MODRINTH.md together; details (commands, permissions, config keys, formulas) go to the wiki page that owns them, not the READMEs.
- The game balance (formulas, classes, abilities, loot) is the owner’s. Fix what’s plainly broken; ask before changing anything else.

## How it fits together

| Module | What it holds |
|---|---|
| `core/` | What doesn't touch a game platform: `Log`, `algebra/`, `config/` but `Ids`, `lang/` but `PlayerLanguages` and `Texts`, `progress/` (`Progression`, `Store`, `Tables`), `combat/` (`Shares`, `Cheese`, and the rules of loot, rewards, and spawner fatigue), `spawn/Hunt` (the survival bonus and the full moon), `achievement/` (`Achievement` and its `Advancement` JSON), `boss/Codec`, and every file the plugin copies (configs, `lang/`, `bundled.sha256`). No `org.bukkit`, no `net.minecraft`; Adventure only `compileOnly`, since Paper ships it and the mods carry it. Its tests: the formulas, the difficulty, the anti-cheese watch, the shared rules, the bundled and language files (`ResourcesTest`), 1.x files, the resolver, the language choices, the store, and `./gradlew balance` |
| `paper/` | The plugin: everything that calls Bukkit or Paper, and `paper-plugin.yml`. Its shaded jar is the only one released |
| `modcore/` | The mods' game side, compiled against NeoForm as in WorldAPI and Allay: one package per Paper package under `ru.mairwunnx.mobosses.modcore`, each file the twin of Paper's, `Hooks` where the mixins (`src/main/java/…/mixin/`) land, `MoBosses` for one server, `Mod` starting and stopping it, `Loader` and `ClientLoader` that each mod fills, `net/` (the messages between a player's game and the server), and `client/` (the settings screen and the loot pages for JEI and REI), which only the loaders' client entries load. Its game tests live in `src/gametest` and run on both loaders |
| `fabric/`, `neoforge/` | Thin adapters: the entry, the loader's events to `Mod`, `OnFabric` or `OnNeoForge` (mod list, permissions, the channel, Mo’Bosses' own files), the client entry in `src/client` (settings button, payload handlers, REI's annotation on NeoForge), and the test mods in `src/gametest`. Each shades core, modcore, Kotlin, Adventure, and the libraries under `ru.mairwunnx.mobosses.libs`, and carries the SQLite driver as a jar in the jar |

The packages stay the same in every module, so a class moves between `core` and `paper` without a change to its imports. A class goes to `core` once nothing in it, and nothing it uses, needs Bukkit; the compiler says when it does. The paths below are packages under `ru.mairwunnx.mobosses`, in `core` or `paper`.

| Where | What it does |
|---|---|
| `MoBossesPlugin.kt` | Startup, `reload()`, and shutdown. Reload reads every file again and keeps the plugin, its command, and its bosses running |
| `MoBossesCommand.kt` | `/mobosses reload`, `spawn`, `killall`, `language`, with suggestions |
| `config/` | One model per file (`Settings`, `AbilitySettings`, `Classes`, `BossSpec`, `MinionSettings`, `RewardSettings`, `LootTable`), read by `Configs` through kaml, not strict. `Yamls.decode` drops a value that doesn’t fit, with a warning, so it takes its default; `Ids` resolves game ids against the registries, 1.x enum names included; `BundledFiles` swaps a server’s untouched older copies for the jar’s |
| `algebra/` | The math, pure: `Curves` (the shared growth curve), `Stats` (health, damage, armor, vanilla experience), `Leveling` (player levels, boss levels, relevance), `Rolls` (minions, cooldowns, weighted picks) |
| `boss/` | `Catalog` (configs with ids resolved), `Bosses` (live bosses: make, adopt, forget), `BossFactory` and `Equipment` (stats and gear), `Boss` (the fight state, kept on the entity through `Keys` and `Codec`), `Minions`, `NameTags` (names per language), `BossBars`, `Visuals` |
| `spawn/` | `SpawnListener` (events), `NaturalSpawns` (the roll and the level), `SpawnLimiter` (cooldowns per player, the cap on bosses nearby) |
| `combat/` | `DamageListener` (shares, bars, abilities on hit), `DeathListener` (experience, kills, loot, rewards, the kill message), `Shares`, `Loot`, `Rewards` |
| `menu/` | `Stats` (cards, the board, boss counts, from the database off the server thread), `Menu` (the `/mobosses` inventory), `LanguagePage` (the language picker), `MenuListener` |
| `ability/` | `Ability` and its context `Cast`; `Abilities` picks one on a hit; `kinds/` holds one object per shared ability and `signature/` one per boss type's own (`Signature` reads `signature` in abilities.yml); `Targets` decides who may be hit and where the ground is; `Arrows` shoots for the arrow abilities |
| `progress/` | Players’ levels and boss statistics: `Progression` (a cache filled at login, in `core`; Paper's `ProgressionListener` feeds it logins and quits) and `Store` (Exposed over SQLite, MySQL/MariaDB, or PostgreSQL) on a thread of its own; `Tables` holds `player_progress` (1.x’s, plus `name`) and `boss_stats` |
| `lang/` | `Message`, `Languages`, `LanguageFiles`, `LocaleResolver`, `GeoIp`, `PlayerLanguages` (who reads what: a player’s own choice first), `LanguageChoices` (those choices, in `player-languages.yml`), `Texts` (rendering, boss names, classes, minions) |
| `protect/` | `Guard` and `Guards`, the GriefPrevention and WorldGuard adapters and flags, `BlockLog` and `CoreProtectLog`, `BlockLogListener`, and `BlockChanges` with `TemporaryBlocks`, the only way abilities touch blocks |
| `hooks/` | PlaceholderAPI (the `<papi:…>` tag and `%mobosses_…%`), MiniPlaceholders (`MiniPlaceholderTags`), Plan (`PlanData`), WorldAPI, and Allay. `BossesExpansion` speaks WorldAPI's expansion API, and `WorldApiLink`, which holds none of its types, is what the rest of the plugin calls. `AllayBosses` gives Allay, the AI helper, four tools (`classes`, `bosses`, `hunt`, `nearby`) and a few lines of prompt; `AllayLink` is its side for the plugin. Plan and MiniPlaceholders ask from other threads, so they read only the progress cache and counts the server thread keeps |

## Rules that keep it working

- Bukkit calls stay on the server thread; the database, GeoIP downloads, and nothing else run off it. PlaceholderAPI may ask from any thread, so the expansion reads only the progress cache.
- A boss is whatever carries `mobosses:type`, `mobosses:class`, and `mobosses:level`. Fight state that must outlive a restart (damage shares, cooldowns, enrage) lives on the boss; cooldowns that must outlive a relog (rewards, spawns) live on the player. Never keep it only in memory, and never force-load chunks for a boss; 1.x did, and leaked loaded chunks.
- Names over mobs are `TextDisplay` passengers, not saved, invisible by default, and shown per player by `NameTags`. A plain teleport keeps passengers since 1.21.10 (checked on 26.3); `NameTags` puts back any label a mob sheds.
- Ability entities (meteors, arrows) are `isPersistent = false`, so a restart never brings one back.
- A new ability is an object in `ability/kinds/` in `Abilities.PICKED`, with its keys in a section of its own in `AbilitySettings` and `abilities.yml` (the flat 1.x keys filled the JVM's limit on constructor parameters; 2.3's `obelisk`, `chain`, `doubles`, `phase_two` show how), a threshold in `AbilityThresholds` and `classes.yml`, and a message in every language.
- Abilities change blocks only through `BlockChanges.set` or `setFor`: that asks the guards, logs to CoreProtect before the change, in the same tick, and restores temporary blocks on shutdown.
- Abilities hit players, through `Targets`, which checks the game mode and `Guard.canHurt`, and the pets and golems that hit the boss in the last 15 seconds (`Targets.isAggressor`, marked by `DamageListener`); area abilities reach both through `Cast.victims`. Nothing else: no villagers, other pets, or minions.
- A boss is never weaker than its vanilla mob: `BossFactory` keeps every stat at least the mob’s own, and `Equipment` puts on only stronger pieces of the same kind. Keep both when changing stats or gear.
- Optional plugins (PlaceholderAPI, Plan, MiniPlaceholders, GriefPrevention, WorldGuard, CoreProtect, WorldAPI, Allay) are `compileOnly` and `join-classpath` in `paper-plugin.yml`; their classes are touched only after checking the plugin is enabled. WorldGuard flags register in `onLoad`, the only time WorldGuard takes them.
- WorldAPI's data keeps its paths, JSON, and privacy: the Moonlands site reads `leaderboards/kills` and `level`, and minecraft-mcp and Grafana read the rest. Add fields; never rename or remove one. Only `bosses` is private, since it carries coordinates; events put a boss's place under `location`, which WorldAPI strips for anonymous clients. `BossesExpansion` is the only class that touches `com.mairwunnx.worldapi.expansion`, and it loads only when WorldAPI is enabled; a WorldAPI before 3.4.0 has no such classes, which `WorldApiLink.connect` catches.
- Allay's tools only read (`READ`, so Allay offers them to everyone, in `/askx` too), run on the server thread, and answer from the database through `ToolResult.later`, never by blocking. `hunt` and `nearby` speak of the asker alone and stay out of public questions; `classes` and `bosses` are the server's settings and may go in them. Never a tool that gives, spawns, or changes anything without the owner's word. `AllayBosses` is the only class that touches `chat.allay.expansion`, and it loads only when Allay is enabled; `requiresApi` is the API version it compiled against (1.3.0 needs Allay 6.17.0 or newer, for `Asker.language`).
- `core` never imports `org.bukkit`, `io.papermc`, or `net.minecraft`, so the mods can share it. A model or formula a platform needs goes there; whatever reads or changes the world stays in `paper` or goes to `modcore`.
- A change to what bosses do goes into `paper` and its twin in `modcore`, and its rules into `core`. modcore never asks which loader runs it; what differs goes behind `Loader` or `ClientLoader`.
- On the mods, Mo’Bosses reads the files it copies from its own jar (`Loader.resource`), never through the class loader, which on Fabric hands out another mod's `config.yml`. Its data on entities is the `mobosses` compound in the entity's custom data, and its times are the server's ticks (`MoBosses.now`).
- The mods add no items, blocks, or entities, so any game joins a server with them. Their one registry entry, the `mobosses:boss_killed` trigger, sits in a registry neither loader syncs. The channel is optional: the server sends only to a game that has it.
- Client classes live in `modcore/client` and the loaders' `src/client`, and nothing a dedicated server runs touches them.
- Game ids stay strings in the config models and resolve in `Catalog`; a bad one logs a warning and drops out, never failing the file.
- A new message gets a `Message` entry and a line in every file in `core/src/main/resources/lang/`; a new boss type, minion, class, or loot `id` needs its name in every language too. `ResourcesTest` fails otherwise.
- A new key in `config.yml`, `abilities.yml`, `classes.yml`, or `rewards.yml` gets the same default in its model; `ResourcesTest` checks they match. 1.x files must keep loading (`core/src/test/resources/v1/`).
- A change to a file the plugin copies to its folder (`Configs.FILES` and `lang/`) adds the new copy’s hash to `core/src/main/resources/bundled.sha256`; `BundledFilesTest` prints the line. Never remove a line: it’s how a server’s untouched older copy gets replaced on start. The loot table keeps junk blocks out; `ResourcesTest` lists them.
- The table `player_progress` keeps 1.x’s name, columns, and index. Progress changes go through `Store.add`, which reads and writes in one transaction.
- Player-provided text goes into messages through `Placeholder.unparsed`. PlaceholderAPI goes through the `<papi:…>` tag only, never `%…%` replacement over whole texts.

## Building and testing

- `./gradlew build` runs the tests of every module and builds `paper/build/libs/mobosses-paper-<version>.jar`, shaded and relocated under `ru.mairwunnx.mobosses.libs`, and the mods, `fabric/build/libs/mobosses-fabric-<version>.jar` and `neoforge/build/libs/mobosses-neoforge-<version>.jar` (the `-slim` jars beside them don't load). JDK 25; Gradle provisions it through the foojay resolver.
- There is no MockBukkit for Paper 26.3; tests cover the pure parts: the formulas (the curves against numbers from the 1.12.0 jar, the 2.1 progression on its own), the bundled files, 1.x files, the language files, the resolver, the codec, and the SQLite store with a 1.x schema. The rest runs on a real Paper; see “Checking a change”.
- `./gradlew balance` (core's) prints the curves from the real formulas; `--args=<folder>` reads a server’s `config.yml` and `classes.yml`. Run it after changing a formula or its defaults, and say in the changelog what moved.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar, started and stopped by the script, which fails on any error about the plugin:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin paper/build/libs/mobosses-paper-dev.jar --modrinth placeholderapi --modrinth plan --jar https://cdn.modrinth.com/data/HQyibRsN/versions/N2WfJ0ll/MiniPlaceholders-Paper-3.2.0.jar --modrinth griefprevention --modrinth worldedit --modrinth worldguard --jar https://github.com/allay-ai/minecraft/releases/download/v6.18.2/allay-spigot-6.18.2.jar --jar https://github.com/MairwunNx/minecraft-worldapi/releases/download/v4.2.0/worldapi-paper-4.2.0.jar --command "forceload add 0 0" --command "mobosses spawn zombie 30 epic 0 -60 4 world" --command "mobosses reload" --command "mobosses killall" --expect "protection: GriefPrevention, WorldGuard" --expect "Bosses removed: 1" --expect "Plan found" --expect "MiniPlaceholders found" --expect "Expansion Mo’Bosses .* is on: 4 tools" --expect "WorldAPI found" --ignore "Plan\] Failed to enable geolocation"
   ```
   Allay and WorldAPI come from their GitHub releases; raise their versions as they move.
   The console must give coordinates to `spawn`; a flat world’s ground is at y -60. Pass `--dir` to keep the folder and run it again: bosses spawned in the first run must come back in the second. Spawn them in a chunk `forceload add` keeps loaded: an unloaded boss stays in the world, but `killall` only sees loaded ones.
3. CoreProtect 24.1 lists the game only up to 26.2 and refuses to start on 26.3. Until a release supports 26.3, build its `master` with `-Dproject.branch=development` to test the logging, as 2.0.0 was tested.
4. Natural spawns, fights, abilities, boss bars, names over mobs, and loot need a game client: a player has to stand near the mobs and hit them. Say so when a change touches them. So do Allay's tools: they run only when a player asks Allay, which needs its backend.
5. The mods, after a change to `core`, `modcore`, `fabric`, or `neoforge`: the game tests on a real server, a singleplayer world in a game window, and a dedicated server each:
   ```sh
   ./gradlew :fabric:runGameTest :neoforge:runGameTestServer
   caffeinate -d ./gradlew :fabric:runClientGameTest :neoforge:runClientGameTest
   python3 -I .claude/scripts/fabric_smoke.py --game 26.3 --mod fabric/build/libs/mobosses-fabric-dev.jar --modrinth fabric-api --command "forceload add 0 0" --command "mobosses spawn zombie 30 epic 0 -60 4 overworld" --command "mobosses killall" --expect "Bosses removed: 1" --expect "Mo’Bosses dev for Fabric is on a dedicated server"
   python3 -I .claude/scripts/neoforge_smoke.py --game 26.3 --mod neoforge/build/libs/mobosses-neoforge-dev.jar --command "forceload add 0 0" --command "mobosses spawn zombie 30 epic 0 -60 4 overworld" --command "mobosses killall" --expect "Bosses removed: 1" --expect "Mo’Bosses dev for NeoForge is on a dedicated server"
   ```
   The client tests open a game window; on macOS a sleeping display stalls it before the menu, which `caffeinate -d` prevents. They save screenshots of the settings screen and the loot pages under `fabric/build/run/clientGameTest/screenshots/` and `neoforge/run/clientgametest/screenshots/`. The dedicated servers' worlds are flat at y -60, as Paper's check.
   A singleplayer or LAN world needs a game window: `./gradlew :fabric:runClient` or `:neoforge:runClient`.

## Autopilot

`.github/workflows/autopilot.yml` runs Claude every Monday at 03:00 UTC: `/update-deps`, then `/update-minecraft`, a fix for whatever they break, a push to `master`, and the “Release” workflow with every store when what the project ships changed. The release's `deploy` job then starts Moonlands' `ship-plugin.yml` with the `FAMILY_ACTIONS_TOKEN` secret, and Claude puts the jar on the server there. The run itself needs the `CLAUDE_CODE_OAUTH_TOKEN` secret. “Autopilot” in `~/c/personal/minecraft/CLAUDE.md` explains the family's runs. A session with the owner still releases only when the owner asks.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice: `compileOnly` and `testImplementation`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) in `paper/build.gradle.kts`, and `api-version` in `paper/src/main/resources/paper-plugin.yml`. The mods: `neoFormVersion` in `modcore/build.gradle.kts`, `minecraft`, `loader`, and `fabricApi` in `fabric/build.gradle.kts`, `minecraft` and `neoforgeVersion` in `neoforge/build.gradle.kts`.
- PlaceholderAPI: `compileOnly("me.clip:placeholderapi:…")`, from `repo.extendedclip.com`.
- Plan: `compileOnly("com.djrapitops:plan-api:…")`, from `repo.playeranalytics.net`; `PlanData` checks Plan’s capabilities.
- MiniPlaceholders: `compileOnly("io.github.miniplaceholders:miniplaceholders-api:…")` from Maven Central; its Paper jar on Modrinth lags a game version, so the check passes its URL with `--jar`.
- GriefPrevention: `compileOnly("com.github.GriefPrevention:GriefPrevention:…")` from JitPack, the version Moonlands runs.
- WorldGuard: `compileOnly("com.sk89q.worldguard:worldguard-bukkit:…")` from `maven.enginehub.org`.
- CoreProtect: `compileOnly("net.coreprotect:coreprotect:…")` from `maven.playpro.com`; the code needs API 10 or newer.
- WorldAPI: `compileOnly("com.mairwunnx:worldapi-expansion:…")` from Maven Central, the expansion API. Raise it only when Mo’Bosses needs something newer: WorldAPI refuses an expansion built against a newer minor than its own. It's in `paper`, `modcore`, `fabric`, and `neoforge`.
- Allay: `compileOnly("chat.allay:allay-expansion:…")` from Maven Central, the expansion API, in the same four modules. The same rule: raise it only when Mo’Bosses needs something newer, since Allay refuses an expansion that needs a newer minor than its own.
- The mods' optional mods, all `compileOnly` and asked only when loaded: Text Placeholder API and Common Protection API (`eu.pb4`, from `maven.nucleoid.xyz`), Flan and Open Parties and Claims (Modrinth's Maven) in `modcore`; JEI's common API (`maven.blamejared.com`), REI's API and Cloth's `basic-math` (`maven.shedaniel.me`) in `modcore`; Mod Menu (Modrinth's Maven) in `fabric`'s client; REI's NeoForge jar, for its annotation, in `neoforge`'s client. The client tests run JEI on Fabric and REI with Cloth Config and Architectury on NeoForge, by Modrinth version id in `fabric/build.gradle.kts` and `neoforge/build.gradle.kts`; raise them with the game.
- Adventure: `adventureVersion` in `core`, `modcore`, `fabric`, and `neoforge`, the version Paper ships.
- The SQLite driver comes from Paper (`compileOnly`, and `testImplementation` in `core` for the store test), and rides in each mod's jar (`sqlite` in `fabric` and `neoforge`); the MariaDB and PostgreSQL drivers are shaded.
- Java: `jvmToolchain` in `build.gradle.kts`, the foojay resolver in `settings.gradle.kts`, and CI’s `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins (Loom and ModDevGradle among them) in the root `plugins` block; libraries in each module's `dependencies`.

## Moonlands

To put a release on Moonlands, point the Mo’Bosses line in `minecraft/extras/jars.txt` (MairwunNx/minecraft) at the release jar, `mobosses-paper-X.Y.Z.jar` since 2.10.0, push, and deploy as its CLAUDE.md says, which also says when a restart may happen without asking. Moonlands keeps its own `config.yml` in `minecraft/config/plugins/MoBosses/`.

## Releasing

1. Add a `## X.Y.Z` section to CHANGELOG.md, written for server owners, and push it. After a change that reaches what the project ships, add it to CHANGELOG.md in the same push: under the topmost “## X.Y.Z” section while that version isn’t released (`gh release view vX.Y.Z` fails), or under a new section with the next version: patch for fixes, minor for features, major for breaking changes. A release then needs only the workflow, and the workflow runs only when the owner asks.
2. Run the “Release” workflow with the new version: `gh workflow run release.yml -R MairwunNx/mobosses -f version=X.Y.Z -f modrinth=true -f hangar=true -f curseforge=true`. Its `changelog` field overrides the CHANGELOG.md section; one of the two must be there. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `mobosses-paper-X.Y.Z.jar` to a GitHub release, with `mobosses-paper.jar` and `mobosses.jar` (the name up to 2.9.0) beside it. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Never delete or re-tag a published release. If GitHub succeeded and Modrinth didn’t, fix the build, finish the upload with `./gradlew :paper:modrinth -PreleaseVersion=X.Y.Z -PchangelogFile=… --no-configuration-cache` locally, and fix the workflow for next time.

## Modrinth

The project is `mobosses` (id `bRPqbgIV`), MIT, created 2026-10-15 and submitted for review with 2.0.0. The “Release” workflow has a `modrinth` switch, off by default: on, it runs `./gradlew :paper:modrinth`, which uploads the jar as a version and syncs MODRINTH.md as the description. MODRINTH.md is for server owners and has the README's shape: the pitch, “What you get”, “Get started”, links into the wiki, and “Related projects”; no build, stack, AI, license, or Moonlands banner. A change to what the plugin does goes in the wiki, and in README.md, README.RU.md, and MODRINTH.md when it changes the pitch. It uses the `MODRINTH_TOKEN` secret; locally the same token is in `~/.secrets/cloud.env`. The icon is `media/icon.png`. Never put the token in the repo.

## Hangar

The project is `MairwunNx/MoBosses` on hangar.papermc.io, MIT, created 2026-10-15 through the API; the owner set its avatar on the site. The “Release” workflow has a `hangar` switch, off by default: on, it runs `./gradlew :paper:publishPluginPublicationToHangar` with the `HANGAR_TOKEN` secret. The jar is over Hangar's 15 MB limit (Exposed brings `kotlin-reflect`), so Hangar gets a link to the GitHub release jar, as WorldAPI does; the GitHub release must exist first, which the workflow's order makes sure of. 2.1.0 went up this way from a local run after the workflow hit the limit. Hangar doesn't sync its page from MODRINTH.md, so edit it on the site after a big change.

## CurseForge

The plugin is project `1766137` (Bukkit Plugins, on dev.bukkit.org) and the mods project `1766190` (Mods, on minecraft.curseforge.com), MIT, created by the owner on authors.curseforge.com on 2026-10-25: CurseForge has no API to create a project. The “Release” workflow's `curseforge` switch, off by default, runs `scripts/curseforge.py` for each jar after the build, with Fabric API as required projects of the Fabric file; it takes the game version from the build files and looks up CurseForge's ids by name. The step may fail without failing the release, so the deploy to Moonlands still runs: when CurseForge doesn't know a new game version yet, upload the jars later with the same script, as the step does. Settings, the icon, and the page are edited on the site; the mods’ page is MODRINTH.md without the Paper parts, and each page links to the other. The token is the `CURSEFORGE_TOKEN` secret, also in `~/.secrets/cloud.env`; never put it in the repo.

## Platforms

How the family ships a project to more than one platform. A project that builds only for Paper keeps its jar's present name until it gains a second platform, so links and Moonlands' `jars.txt` keep working.

- **Modules.** One Gradle module per platform, `paper/`, `fabric/`, `neoforge/`, next to a `core/` module with what they share: the rules as plain Kotlin without game imports, data, and assets. A project with both mods adds `modcore/`, the game without a loader, compiled against NeoForm and packed into both mods' jars, so `fabric/` and `neoforge/` stay thin adapters (Allay and Red Bull do; their game tests live in `modcore/src/gametest` and run on both loaders). Every module builds from one version, and one release ships them all.
- **Jars.** `<name>-paper-X.Y.Z.jar`, `<name>-fabric-X.Y.Z.jar`, and `<name>-neoforge-X.Y.Z.jar`, each attached again without the version, so `releases/latest/download/<name>-<platform>.jar` always fetches the newest.
- **Modrinth.** One project for every platform. Each platform is its own version, numbered `X.Y.Z+paper`, `X.Y.Z+fabric`, or `X.Y.Z+neoforge`, with its own loaders and dependencies; a mod's version says it runs on the client and the server.
- **Hangar.** Paper only, numbered `X.Y.Z`.
- **CurseForge.** Two projects, since one holds a single class: Paper's jar in a Bukkit Plugins project, numbered `X.Y.Z`, and both mods as files of one Mods project, “X.Y.Z for Fabric” and “X.Y.Z for NeoForge”. A Paper-only project has the plugin project alone.
- **Moonlands** runs the Paper jar only.

Mo’Bosses has `core`, `paper`, `modcore`, `fabric`, and `neoforge` since 2.10.0. On the owner's word its Paper jar took its platform's name then, ahead of the mods: `mobosses-paper-X.Y.Z.jar`, with `mobosses.jar` attached too for old links. The mods play since 2.11.0, and `mobosses.mods=true` in `gradle.properties` ships all three: Modrinth's `X.Y.Z+paper`, `+fabric`, and `+neoforge` (the mods through `scripts/modrinth_mods.py`, on the client or the server), and the release attaches every jar.

## Commits

Short one-line imperative English messages, one logical change per commit: “Keep bosses out of GriefPrevention claims”.
