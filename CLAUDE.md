# Mo’Bosses: notes for AI agents

A Paper 26.3 plugin that turns ordinary mobs into bosses of six classes, with abilities, minions, boss bars, loot, rewards, and players’ own Mo’Bosses levels, each player reading it in their own language, picked by IP. README.md explains it for players and server owners; README.RU.md is its Russian copy. Moonlands, the owner’s server (MairwunNx/minecraft), doesn’t run it: Moonlands stays vanilla.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, the config files, and the English language file are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `lang/ru.yml` follow Russian typography; `lang/de.yml` uses „du“. Change both READMEs and MODRINTH.md together.
- The game balance (formulas, classes, abilities, loot) is the owner’s. Fix what’s plainly broken; ask before changing anything else.

## How it fits together

| Where | What it does |
|---|---|
| `MoBossesPlugin.kt` | Startup, `reload()`, and shutdown. Reload reads every file again and keeps the plugin, its command, and its bosses running |
| `config/` | The config files through kaml, not strict. `Yamls.decode` drops a value that doesn’t fit, with a warning, so it takes its default; `Ids` resolves game ids against the registries, 1.x enum names included |
| `boss/Catalog.kt` | The configs with every id resolved, built once per load |
| `boss/Bosses.kt` | Live bosses. `make` scales a mob and writes its type, class, and level to its persistent data; `adopt` takes it back when its chunk loads; `forget` lets go of its bar, glow, and aura |
| `boss/SpawnListener.kt` | Natural spawns become bosses by chance near a player; minions may become weaker bosses; chunk loads and unloads |
| `boss/CombatListener.kt` | Damage shares, deaths (experience, loot, rewards, the kill message), meteors that mustn’t land as blocks, and CoreProtect entries for blocks bosses and minions change |
| `boss/BossBars.kt`, `boss/Visuals.kt` | Bars, one per language per boss; the glow and particles while a player is near |
| `boss/Loot.kt`, `boss/Rewards.kt` | One item per qualifying player, in their language; potion effects with a cooldown |
| `ability/` | `Abilities` picks an ability on a hit; `Casts` casts it; `Targets` decides who may be hit and where the ground is |
| `algebra/Formulas.kt` | The math, pure, as in 1.12.0 |
| `progress/` | Players’ levels: a cache filled at login, and `Store` (Exposed over SQLite, MySQL/MariaDB, or PostgreSQL) on a thread of its own |
| `lang/` | `Languages`, `LocaleResolver`, `GeoIp`, and `Texts`, which knows each player’s language and renders boss names, classes, and minions |
| `protect/` | `Guard` and its adapters (GriefPrevention, WorldGuard), the WorldGuard flags, the CoreProtect log, and `BlockChanges`, the only way abilities touch blocks |
| `hooks/` | PlaceholderAPI: the `<papi:…>` tag and the `%mobosses_…%` expansion |
| `MoBossesCommand.kt` | `/mobosses reload`, `spawn`, `killall`, with suggestions |

## Rules that keep it working

- Bukkit calls stay on the server thread; the database, GeoIP downloads, and nothing else run off it. PlaceholderAPI may ask from any thread, so the expansion reads only the progress cache.
- A boss is whatever carries `mobosses:type`, `mobosses:class`, and `mobosses:level`. Never keep a boss only in memory, and never force-load chunks for one; 1.x did, and leaked loaded chunks.
- Abilities change blocks only through `BlockChanges.set` or `setFor`: that asks the guards, logs to CoreProtect before the change, in the same tick, and restores temporary blocks on shutdown.
- Abilities hit players only, through `Targets`, which checks the game mode and `Guard.canHurt`.
- Optional plugins (PlaceholderAPI, GriefPrevention, WorldGuard, CoreProtect) are `compileOnly` and `join-classpath` in `paper-plugin.yml`; their classes are touched only after checking the plugin is enabled. WorldGuard flags register in `onLoad`, the only time WorldGuard takes them.
- Game ids stay strings in the config models and resolve in `Catalog`; a bad one logs a warning and drops out, never failing the file.
- A new message gets a `Message` entry and a line in every file in `src/main/resources/lang/`; a new boss type, minion, class, or loot `id` needs its name in every language too. `ResourcesTest` fails otherwise.
- A new key in `config.yml`, `abilities.yml`, `classes.yml`, or `rewards.yml` gets the same default in its model; `ResourcesTest` checks they match. 1.x files must keep loading (`src/test/resources/v1/`).
- The table `player_progress` keeps 1.x’s name, columns, and index. Progress changes go through `Store.add`, which reads and writes in one transaction.
- Player-provided text goes into messages through `Placeholder.unparsed`. PlaceholderAPI goes through the `<papi:…>` tag only, never `%…%` replacement over whole texts.

## Building and testing

- `./gradlew build` runs the tests and builds `build/libs/mobosses-<version>.jar`, shaded and relocated under `ru.mairwunnx.mobosses.libs`. JDK 25; Gradle provisions it through the foojay resolver.
- There is no MockBukkit for Paper 26.3; tests cover the pure parts: the formulas against numbers from the 1.12.0 jar, the bundled files, 1.x files, the language files, the resolver, and the SQLite store with a 1.x schema. The rest runs on a real Paper; see “Checking a change”.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar, started and stopped by the script, which fails on any error about the plugin:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin build/libs/mobosses-dev.jar --modrinth placeholderapi --modrinth griefprevention --modrinth worldedit --modrinth worldguard --command "forceload add 0 0" --command "mobosses spawn zombie 30 epic 0 -60 4 world" --command "mobosses reload" --command "mobosses killall" --expect "protection: GriefPrevention, WorldGuard" --expect "Bosses removed: 1"
   ```
   The console must give coordinates to `spawn`; a flat world’s ground is at y -60. Pass `--dir` to keep the folder and run it again: bosses spawned in the first run must come back in the second. Spawn them in a chunk `forceload add` keeps loaded: an unloaded boss stays in the world, but `killall` only sees loaded ones.
3. CoreProtect 24.1 lists the game only up to 26.2 and refuses to start on 26.3. Until a release supports 26.3, build its `master` with `-Dproject.branch=development` to test the logging, as 2.0.0 was tested.
4. Natural spawns, fights, abilities, boss bars, and loot need a game client: a player has to stand near the mobs and hit them. Say so when a change touches them.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice: `compileOnly` and `testImplementation`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) in `build.gradle.kts`, and `api-version` in `src/main/resources/paper-plugin.yml`.
- PlaceholderAPI: `compileOnly("me.clip:placeholderapi:…")`, from `repo.extendedclip.com`.
- GriefPrevention: `compileOnly("com.github.GriefPrevention:GriefPrevention:…")` from JitPack, the version Moonlands runs.
- WorldGuard: `compileOnly("com.sk89q.worldguard:worldguard-bukkit:…")` from `maven.enginehub.org`.
- CoreProtect: `compileOnly("net.coreprotect:coreprotect:…")` from `maven.playpro.com`; the code needs API 10 or newer.
- The SQLite driver comes from Paper (`compileOnly`, and `testImplementation` for the store test); the MariaDB and PostgreSQL drivers are shaded.
- Java: `jvmToolchain` in `build.gradle.kts`, the foojay resolver in `settings.gradle.kts`, and CI’s `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the `plugins` block; libraries in `dependencies`.

## Releasing

1. Add a `## X.Y.Z` section to CHANGELOG.md, written for server owners, and push it. After a change that reaches what the project ships, add it to CHANGELOG.md in the same push: under the topmost “## X.Y.Z” section while that version isn’t released (`gh release view vX.Y.Z` fails), or under a new section with the next version: patch for fixes, minor for features, major for breaking changes. A release then needs only the workflow, and the workflow runs only when the owner asks.
2. Run the “Release” workflow with the new version: `gh workflow run release.yml -R MairwunNx/mobosses -f version=X.Y.Z -f modrinth=true`. Its `changelog` field overrides the CHANGELOG.md section; one of the two must be there. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `mobosses-X.Y.Z.jar` to a GitHub release. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Never delete or re-tag a published release. If GitHub succeeded and Modrinth didn’t, fix the build, finish the upload with `./gradlew modrinth -PreleaseVersion=X.Y.Z -PchangelogFile=… --no-configuration-cache` locally, and fix the workflow for next time.

## Modrinth

The project is `mobosses`, MIT. The “Release” workflow has a `modrinth` switch, off by default: on, it runs `./gradlew modrinth`, which uploads the jar as a version and syncs MODRINTH.md as the description. MODRINTH.md is for server owners: no build, stack, AI, or related projects, and no Moonlands banner. A change to what the plugin does goes in README.md, README.RU.md, and MODRINTH.md. It uses the `MODRINTH_TOKEN` secret; locally the same token is in `~/.secrets/cloud.env`. The icon is `media/icon.png`. Never put the token in the repo.

## Hangar

The workflow also has a `hangar` switch for `MairwunNx/MoBosses` on hangar.papermc.io, through `./gradlew publishPluginPublicationToHangar` and the `HANGAR_TOKEN` secret. A new Hangar project takes its first version only through the website (the API answers 404 until then); later versions go through the workflow. Hangar doesn’t sync its page from MODRINTH.md, so edit it on the site after a big change.

## Commits

Short one-line imperative English messages, one logical change per commit: “Keep bosses out of GriefPrevention claims”.
