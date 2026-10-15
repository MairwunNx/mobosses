# Mo’Bosses: notes for AI agents

A Paper 26.3 plugin that turns ordinary mobs into bosses of six classes, with abilities, minions, boss bars, loot, rewards, and players’ own Mo’Bosses levels, each player reading it in their own language, picked by IP. README.md explains it for players and server owners; README.RU.md is its Russian copy. Moonlands, the owner’s server (MairwunNx/minecraft), runs it since 2.1.0.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, the config files, and the English language file are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `lang/ru.yml` follow Russian typography; `lang/de.yml` uses „du“. Change both READMEs and MODRINTH.md together.
- The game balance (formulas, classes, abilities, loot) is the owner’s. Fix what’s plainly broken; ask before changing anything else.

## How it fits together

| Where | What it does |
|---|---|
| `MoBossesPlugin.kt` | Startup, `reload()`, and shutdown. Reload reads every file again and keeps the plugin, its command, and its bosses running |
| `MoBossesCommand.kt` | `/mobosses reload`, `spawn`, `killall`, with suggestions |
| `config/` | One model per file (`Settings`, `AbilitySettings`, `Classes`, `BossSpec`, `MinionSettings`, `RewardSettings`, `LootTable`), read by `Configs` through kaml, not strict. `Yamls.decode` drops a value that doesn’t fit, with a warning, so it takes its default; `Ids` resolves game ids against the registries, 1.x enum names included |
| `algebra/` | The math, pure: `Curves` (the shared growth curve), `Stats` (health, damage, armor, vanilla experience), `Leveling` (player levels, boss levels, relevance), `Rolls` (minions, cooldowns, weighted picks) |
| `boss/` | `Catalog` (configs with ids resolved), `Bosses` (live bosses: make, adopt, forget), `BossFactory` and `Equipment` (stats and gear), `Boss` (the fight state, kept on the entity through `Keys` and `Codec`), `Minions`, `NameTags` (names per language), `BossBars`, `Visuals` |
| `spawn/` | `SpawnListener` (events), `NaturalSpawns` (the roll and the level), `SpawnLimiter` (cooldowns per player, the cap on bosses nearby) |
| `combat/` | `DamageListener` (shares, bars, abilities on hit), `DeathListener` (experience, kills, loot, rewards, the kill message), `Shares`, `Loot`, `Rewards` |
| `menu/` | `Stats` (cards, the board, boss counts, from the database off the server thread), `Menu` (the `/mobosses` inventory), `MenuListener` |
| `ability/` | `Ability` and its context `Cast`; `Abilities` picks one on a hit; `kinds/` holds one object per shared ability and `signature/` one per boss type's own (`Signature` reads `signature` in abilities.yml); `Targets` decides who may be hit and where the ground is; `Arrows` shoots for the arrow abilities |
| `progress/` | Players’ levels and boss statistics: `Progression` (a cache filled at login) and `Store` (Exposed over SQLite, MySQL/MariaDB, or PostgreSQL) on a thread of its own; `Tables` holds `player_progress` (1.x’s, plus `name`) and `boss_stats` |
| `lang/` | `Message`, `Languages`, `LanguageFiles`, `LocaleResolver`, `GeoIp`, `PlayerLanguages` (who reads what), `Texts` (rendering, boss names, classes, minions) |
| `protect/` | `Guard` and `Guards`, the GriefPrevention and WorldGuard adapters and flags, `BlockLog` and `CoreProtectLog`, `BlockLogListener`, and `BlockChanges` with `TemporaryBlocks`, the only way abilities touch blocks |
| `hooks/` | PlaceholderAPI (the `<papi:…>` tag and `%mobosses_…%`), MiniPlaceholders (`MiniPlaceholderTags`), and Plan (`PlanData`). Plan and MiniPlaceholders ask from other threads, so they read only the progress cache and counts the server thread keeps |

## Rules that keep it working

- Bukkit calls stay on the server thread; the database, GeoIP downloads, and nothing else run off it. PlaceholderAPI may ask from any thread, so the expansion reads only the progress cache.
- A boss is whatever carries `mobosses:type`, `mobosses:class`, and `mobosses:level`. Fight state that must outlive a restart (damage shares, cooldowns, enrage) lives on the boss; cooldowns that must outlive a relog (rewards, spawns) live on the player. Never keep it only in memory, and never force-load chunks for a boss; 1.x did, and leaked loaded chunks.
- Names over mobs are `TextDisplay` passengers, not saved, invisible by default, and shown per player by `NameTags`. A plain teleport keeps passengers since 1.21.10 (checked on 26.3); `NameTags` puts back any label a mob sheds.
- Ability entities (meteors, arrows) are `isPersistent = false`, so a restart never brings one back.
- A new ability is an object in `ability/kinds/` in `Abilities.PICKED`, with its keys in `AbilitySettings` and `abilities.yml`, a threshold in `AbilityThresholds` and `classes.yml`, and a message in every language.
- Abilities change blocks only through `BlockChanges.set` or `setFor`: that asks the guards, logs to CoreProtect before the change, in the same tick, and restores temporary blocks on shutdown.
- Abilities hit players, through `Targets`, which checks the game mode and `Guard.canHurt`, and the pets and golems that hit the boss in the last 15 seconds (`Targets.isAggressor`, marked by `DamageListener`); area abilities reach both through `Cast.victims`. Nothing else: no villagers, other pets, or minions.
- A boss is never weaker than its vanilla mob: `BossFactory` keeps every stat at least the mob’s own, and `Equipment` puts on only stronger pieces of the same kind. Keep both when changing stats or gear.
- Optional plugins (PlaceholderAPI, Plan, MiniPlaceholders, GriefPrevention, WorldGuard, CoreProtect) are `compileOnly` and `join-classpath` in `paper-plugin.yml`; their classes are touched only after checking the plugin is enabled. WorldGuard flags register in `onLoad`, the only time WorldGuard takes them.
- Game ids stay strings in the config models and resolve in `Catalog`; a bad one logs a warning and drops out, never failing the file.
- A new message gets a `Message` entry and a line in every file in `src/main/resources/lang/`; a new boss type, minion, class, or loot `id` needs its name in every language too. `ResourcesTest` fails otherwise.
- A new key in `config.yml`, `abilities.yml`, `classes.yml`, or `rewards.yml` gets the same default in its model; `ResourcesTest` checks they match. 1.x files must keep loading (`src/test/resources/v1/`).
- The table `player_progress` keeps 1.x’s name, columns, and index. Progress changes go through `Store.add`, which reads and writes in one transaction.
- Player-provided text goes into messages through `Placeholder.unparsed`. PlaceholderAPI goes through the `<papi:…>` tag only, never `%…%` replacement over whole texts.

## Building and testing

- `./gradlew build` runs the tests and builds `build/libs/mobosses-<version>.jar`, shaded and relocated under `ru.mairwunnx.mobosses.libs`. JDK 25; Gradle provisions it through the foojay resolver.
- There is no MockBukkit for Paper 26.3; tests cover the pure parts: the formulas (the curves against numbers from the 1.12.0 jar, the 2.1 progression on its own), the bundled files, 1.x files, the language files, the resolver, the codec, and the SQLite store with a 1.x schema. The rest runs on a real Paper; see “Checking a change”.
- `./gradlew balance` prints the curves from the real formulas; `--args=<folder>` reads a server’s `config.yml` and `classes.yml`. Run it after changing a formula or its defaults, and say in the changelog what moved.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar, started and stopped by the script, which fails on any error about the plugin:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin build/libs/mobosses-dev.jar --modrinth placeholderapi --modrinth plan --jar https://cdn.modrinth.com/data/HQyibRsN/versions/N2WfJ0ll/MiniPlaceholders-Paper-3.2.0.jar --modrinth griefprevention --modrinth worldedit --modrinth worldguard --command "forceload add 0 0" --command "mobosses spawn zombie 30 epic 0 -60 4 world" --command "mobosses reload" --command "mobosses killall" --expect "protection: GriefPrevention, WorldGuard" --expect "Bosses removed: 1" --expect "Plan found" --expect "MiniPlaceholders found" --ignore "Plan\] Failed to enable geolocation"
   ```
   The console must give coordinates to `spawn`; a flat world’s ground is at y -60. Pass `--dir` to keep the folder and run it again: bosses spawned in the first run must come back in the second. Spawn them in a chunk `forceload add` keeps loaded: an unloaded boss stays in the world, but `killall` only sees loaded ones.
3. CoreProtect 24.1 lists the game only up to 26.2 and refuses to start on 26.3. Until a release supports 26.3, build its `master` with `-Dproject.branch=development` to test the logging, as 2.0.0 was tested.
4. Natural spawns, fights, abilities, boss bars, names over mobs, and loot need a game client: a player has to stand near the mobs and hit them. Say so when a change touches them.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice: `compileOnly` and `testImplementation`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) in `build.gradle.kts`, and `api-version` in `src/main/resources/paper-plugin.yml`.
- PlaceholderAPI: `compileOnly("me.clip:placeholderapi:…")`, from `repo.extendedclip.com`.
- Plan: `compileOnly("com.djrapitops:plan-api:…")`, from `repo.playeranalytics.net`; `PlanData` checks Plan’s capabilities.
- MiniPlaceholders: `compileOnly("io.github.miniplaceholders:miniplaceholders-api:…")` from Maven Central; its Paper jar on Modrinth lags a game version, so the check passes its URL with `--jar`.
- GriefPrevention: `compileOnly("com.github.GriefPrevention:GriefPrevention:…")` from JitPack, the version Moonlands runs.
- WorldGuard: `compileOnly("com.sk89q.worldguard:worldguard-bukkit:…")` from `maven.enginehub.org`.
- CoreProtect: `compileOnly("net.coreprotect:coreprotect:…")` from `maven.playpro.com`; the code needs API 10 or newer.
- The SQLite driver comes from Paper (`compileOnly`, and `testImplementation` for the store test); the MariaDB and PostgreSQL drivers are shaded.
- Java: `jvmToolchain` in `build.gradle.kts`, the foojay resolver in `settings.gradle.kts`, and CI’s `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the `plugins` block; libraries in `dependencies`.

## Moonlands

To put a release on Moonlands, point the Mo’Bosses line in `minecraft/extras/jars.txt` (MairwunNx/minecraft) at the release jar, push, and deploy as its CLAUDE.md says, which also says when a restart may happen without asking. Moonlands keeps its own `config.yml` in `minecraft/config/plugins/MoBosses/`.

## Releasing

1. Add a `## X.Y.Z` section to CHANGELOG.md, written for server owners, and push it. After a change that reaches what the project ships, add it to CHANGELOG.md in the same push: under the topmost “## X.Y.Z” section while that version isn’t released (`gh release view vX.Y.Z` fails), or under a new section with the next version: patch for fixes, minor for features, major for breaking changes. A release then needs only the workflow, and the workflow runs only when the owner asks.
2. Run the “Release” workflow with the new version: `gh workflow run release.yml -R MairwunNx/mobosses -f version=X.Y.Z -f modrinth=true -f hangar=true`. Its `changelog` field overrides the CHANGELOG.md section; one of the two must be there. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `mobosses-X.Y.Z.jar` to a GitHub release. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Never delete or re-tag a published release. If GitHub succeeded and Modrinth didn’t, fix the build, finish the upload with `./gradlew modrinth -PreleaseVersion=X.Y.Z -PchangelogFile=… --no-configuration-cache` locally, and fix the workflow for next time.

## Modrinth

The project is `mobosses` (id `bRPqbgIV`), MIT, created 2026-10-15 and submitted for review with 2.0.0. The “Release” workflow has a `modrinth` switch, off by default: on, it runs `./gradlew modrinth`, which uploads the jar as a version and syncs MODRINTH.md as the description. MODRINTH.md is for server owners: no build, stack, AI, or related projects, and no Moonlands banner. A change to what the plugin does goes in README.md, README.RU.md, and MODRINTH.md. It uses the `MODRINTH_TOKEN` secret; locally the same token is in `~/.secrets/cloud.env`. The icon is `media/icon.png`. Never put the token in the repo.

## Hangar

The project is `MairwunNx/MoBosses` on hangar.papermc.io, MIT, created 2026-10-15 through the API; the owner set its avatar on the site. The “Release” workflow has a `hangar` switch, off by default: on, it runs `./gradlew publishPluginPublicationToHangar` with the `HANGAR_TOKEN` secret. The jar is over Hangar's 15 MB limit (Exposed brings `kotlin-reflect`), so Hangar gets a link to the GitHub release jar, as WorldAPI does; the GitHub release must exist first, which the workflow's order makes sure of. 2.1.0 went up this way from a local run after the workflow hit the limit. Hangar doesn't sync its page from MODRINTH.md, so edit it on the site after a big change.

## Commits

Short one-line imperative English messages, one logical change per commit: “Keep bosses out of GriefPrevention claims”.
