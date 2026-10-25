# Mo’Bosses on Fabric and NeoForge

The plan for Mo’Bosses as a mod, drafted on 2026-10-25 for the owner to approve before any code. On a dedicated Fabric or NeoForge server the mod plays as the Paper plugin does: the same configs, the same `player_progress`, the same bosses. In singleplayer and on LAN it plays the world you host. The players need no mod of their own, except for the loot pages in EMI, JEI, and REI and the settings screen, which only the client draws.

## What the owner picked

| Feature | Where | Players need the mod |
|---|---|---|
| The port itself: bosses, abilities, minions, loot, rewards, levels, menu, achievements, languages | mods | no |
| Bosses from other mods' mobs | mods | no (the mob's own mod is on every client anyway) |
| Other mods' weapons, armor, and enchantments in gear and loot | mods | no |
| The `mobosses:boss_killed` trigger, for a server's own advancements in data packs | mods | no |
| Difficulty: peaceful makes no bosses, the rest scale bosses by a modifier | Paper and mods | no |
| Anti-cheese: no boats, no portals, no trapping or out-of-reach farming | Paper and mods | no |
| Boss loot in EMI, JEI, and REI | mods | yes, the viewer and Mo’Bosses |
| Settings in game: Mod Menu on Fabric, the mod list's button on NeoForge | mods | yes, the one who edits |

Left for later, by the owner's word: client HUD and boss bar, ability warnings, the bestiary screen, battle music, loot through loot tables, Brain AI for piglins and their kin, lairs, trophies. The mods add no items, blocks, or entities, so a game with Mo’Bosses joins any server and any game joins a server with it, as Allay decided.

## How the code splits

The family's way, as WorldAPI and Allay did it: what decides goes to `core` with tests, and what touches the game is written twice, once against Bukkit in `paper` and once against the game in `modcore`, each file next to its twin with the same name. Fabric and NeoForge differ only behind `Loader`; modcore never asks which loader it runs on.

A shared facade over both games (entities, particles, sounds, blocks) would write each ability once, but would also rewrite the whole plugin behind it and hide each game's details behind the lowest common one. The family chose against it twice; so does this plan.

### What moves to `core` first

Each move keeps Paper playing as before, with the Paper check from CLAUDE.md after each step:

- **The clock.** `Clock`, which every timer reads instead of `System.currentTimeMillis()`: cooldowns, despawns, rewards' wait, spawner fatigue, aggressors, the avenger window. Paper keeps wall time. The mods count the overworld's game time, which the save keeps and which stops while a singleplayer game is paused, so leaving for the menu for ten minutes no longer sends a boss away.
- **The decisions.** Cooldown maps (`Abilities`), the despawn rule, `SpawnLimiter`'s counts, the loot pick (weights, classes below, pity, durability, amount), rewards' tiers and durations, the bestiary and the cards' numbers, `Targets`' order of aimed targets. Each as a pure function over plain values, tested.
- **Difficulty** and **anti-cheese** decisions (below), new, tested, used by both platforms.

### `paper` package by package, as the mods see it

| Paper | In modcore |
|---|---|
| `MoBossesPlugin` | `Mod`: everything for a server built at `SERVER_STARTING`, closed at `SERVER_STOPPED`, so a second world in the same game starts clean. Ticks from the server's tick event, the same periods |
| `MoBossesCommand` | `/mobosses` (aliases `mb`, `moboss`) on Brigadier with the same subcommands and suggestions. Permissions as Allay's `Perms`: fabric-permissions-api on Fabric, PermissionAPI on NeoForge, else operator level 2 as `paper-plugin.yml` gives them; the owner of a singleplayer world may reload and spawn without cheats |
| `config/Ids` | `Ids` against the game's registries (`BuiltInRegistries`, and the server's `registryAccess()` for enchantments, which data packs add), 1.x enum names included |
| `boss/Keys`, `Boss` | The same keys and the same `Codec` strings in a `mobosses` compound on the entity, written and read by one mixin into `Entity`'s save and load. One code for both loaders; attachments would need two. A player's own keys survive death by `ServerPlayer.restoreFrom`. Bosses from a Paper world's `BukkitValues` are read too, so a world moved from Paper keeps its bosses |
| `Catalog`, `BossFactory`, `Equipment` | The same, over `EntityType`, `Item`, `Holder<Enchantment>`. `Equipment` already ranks pieces by their attribute modifiers, so other mods' gear ranks the same way |
| `Bosses`, `SpawnListener` | Entity load and unload events adopt and forget bosses; a mixin into `Mob.finalizeSpawn` hands every spawn with its `EntitySpawnReason` to `NaturalSpawns` |
| `NameTags` | Text displays sent by packet only to the players who read that language, as Allay's `Ghosts`, riding the mob on the client. Nothing in the world, nothing to shed, nothing to clear after a crash |
| `BossBars` | `ServerBossEvent`, one per language among a boss's viewers |
| `Visuals` | The glow through a scoreboard team per class, the aura through `ServerLevel.sendParticles` |
| `Minions`, `Decoys`, `Despawns` | The same over the game's mobs; decoys stay unsaved |
| `combat/` | Damage and death through mixins at `LivingEntity.actuallyHurt` and `die`, where both loaders' events differ; loot, rewards, group health, spawner fatigue as on Paper |
| `ability/` | Every ability and signature, one file per Paper file, with its numbers from `core`. Ability entities never save (`Entity.shouldBeSaved` is false for them) |
| `achievement/` | The same advancements, loaded as a built-in data pack generated in `achievements.language` at server start. Mo’Bosses' own advancements use the new trigger instead of being granted by hand |
| `menu/` | `ChestMenu` windows that cancel every click, as Allay's `menus/` |
| `progress/` | `Store` as it is, from `core`; `Progression` caches at login as on Paper |
| `lang/` | `PlayerLanguages` from the client's language (`clientInformation().language()`) and GeoIP. A local address skips GeoIP, so singleplayer and LAN follow the client's language |
| `protect/` | `Guard`s for Flan, Open Parties and Claims, and Patbox's Common Protection API, as Allay's `Claims`; `BlockChanges` and `TemporaryBlocks` as on Paper |
| `hooks/` | WorldAPI and Allay through their expansion APIs, which both bring as mods; Text Placeholder API both ways (`%mobosses:level%` and the `<papi:…>` tag), as Allay's `Placeholders` |

Deferred integrations: Ledger for the block log (Fabric only, and nothing on NeoForge), Plan (Fabric only), MiniPlaceholders, FTB Chunks. Without a block log, abilities' blocks still go through the guards and come back on shutdown.

## Where files live

| What | Where | Why |
|---|---|---|
| `config.yml`, the other configs, `lang/`, `player-languages.yml`, `geoip/` | `config/mobosses/` of the game | One setup for every world; a player's language is theirs, not a world's |
| `bosses.db` and temporary blocks | `<save>/mobosses/` | Levels belong to a world: a creative test world shouldn't carry your level 100 into survival. It moves, copies, and goes with the save |

A dedicated mod server keeps the database in `world/mobosses/` too, and `database` in `config.yml` still points it at MySQL, MariaDB, or PostgreSQL. A singleplayer world always uses SQLite in its save.

## Difficulty

The game's difficulty scales new bosses, on Paper (each world's own) and on the mods alike. A boss keeps the modifier it was made with, written on it as `mobosses:difficulty`; bosses made before carry none and count as hard, which is how they were made. A change of difficulty applies to the next boss.

- **Peaceful**: no mob rolls into a boss.
- **Easy, normal, hard, hardcore**: one modifier each, `m`. Hard is 1.0, since the balance was built for it; hardcore is 1.2333333339 (a world's or server's `hardcore` flag).
- Each part of a boss takes `1 + (m − 1) × weight`, so one number per difficulty moves every part, each by its own weight. Cooldowns divide by it instead.
- The floor stays: a boss is never weaker than its vanilla mob, on any difficulty.

```yaml
difficulty:
  enabled: true
  modifiers: { easy: 0.7333333337, normal: 0.8777777773, hard: 1.0, hardcore: 1.2333333339 }
  weights:
    health: 1.0
    damage: 1.0          # melee, and ranged through formula.ranged_damage
    armor: 0.5           # armor and toughness
    cooldowns: 0.5       # an ability's cooldown divides by it
    minions: 0.5         # how many a summon brings
    spawn_chance: 0.5
    experience: 1.0      # the vanilla experience a boss drops
```

Mo’Bosses experience follows a boss's health already, so it needs no weight of its own. Loot, rewards, and levels stay as they are. The difficulty itself always comes from the server or the world; Mo’Bosses has no difficulty of its own. Grading by it is on by default, and `enabled: false` makes every difficulty but peaceful play as hard. The modifiers and weights above are the owner's balance, and class odds stay out of it. `./gradlew balance` prints the curves for each difficulty.

Moonlands plays on hard, so nothing changes there. A Paper server on easy or normal gets weaker bosses after the update; the changelog says so and points at `enabled: false`.

## Anti-cheese

On Paper and the mods alike, each under its own switch in `anti_cheese`:

| Cheese | What a boss does | Paper | Mods |
|---|---|---|---|
| A boat or a minecart | Never gets in; one already in gets out as it turns | `EntityMountEvent` | mixin into `Entity.startRiding` |
| A portal | Stays in its dimension | `EntityPortalEvent` | mixin into `Entity.canUsePortal` |
| A pillar, a wall, a 1×1 shelter | Hit by a player it can't reach for `unreachable_seconds`: steps out next to them, as an enderman does | `Pathfinder.findPath(…).canReachFinalPoint()` | `PathNavigation.createPath(…).canReach()` |
| A hole, a trapdoor, a bubble column, honey | Hurt while it hasn't moved a block in `stuck_seconds`: the same step out | position samples | position samples |

The step out goes only to a free spot within 16 blocks of the player, never into a claim that forbids bosses. `unreachable_seconds` is 8 and `stuck_seconds` 6: long enough that a boss climbing a hill or a player ducking behind a tree doesn't count, short enough that a trap earns a hit or two at most. A boss steps out at most once in 20 seconds (`step_cooldown_seconds`), so a player who keeps building can't chain it into a dance. Environmental damage already earns nothing, since shares count only the players' and their pets' damage.

## Bosses from other mods

`bosses.yml` takes any living, spawnable mob by its id, like `alexsmobs:grizzly_bear`, with the same keys as a vanilla type. Such a type is off unless the owner turns it on. It gets the shared abilities, no signature, and its own kind as minions unless `minions.yml` says otherwise. Its name over the mob and in messages falls back to the game's own name for it, which every client translates. Loot and gear take other mods' items and enchantments by id; a bad id drops out with a warning, as now.

## The `mobosses:boss_killed` trigger

A criterion trigger for advancements, met by every player who got a share of a boss when it died:

```json
"criteria": { "slayer": { "trigger": "mobosses:boss_killed", "conditions": {
  "type": "zombie", "class": { "min": "epic" }, "level": { "min": 50 } } } }
```

`type` takes a boss type, or a list; `class` a class, or `min` and `max` by rank; `level` a number, or `min` and `max`. Every condition is optional, and `player` takes the game's usual entity predicate. Trigger types aren't sent to clients, so a vanilla client plays such advancements. Paper has no such API, so the trigger is the mods' alone.

## EMI, JEI, and REI

A “Boss loot” page for each loot entry: the item, its class and boss level range, the boss types, and its odds. The client gets the loot table from the server by an optional payload when it joins, in the player's language, so a vanilla client never sees the channel, and a modded client on a server without Mo’Bosses has no page. In singleplayer the page reads the table of the world you play. Each viewer is `compileOnly`, touched only when loaded.

## Settings in game

A screen from Mod Menu on Fabric and from NeoForge's mod list. It lists every key of `config.yml`, `classes.yml`, `abilities.yml`, and `rewards.yml`, built from the models' serializers, so a new key shows up without new screen code. It edits the files in place, line by line, so the comments stay, and runs `reload()`. In singleplayer it edits `config/mobosses/`. On a dedicated server an operator with the mod edits the server's files by payload, within these limits:

- Only `mobosses.config`, which nobody has by default but operators of level 4, the level that may stop the server. The server checks it on every message, never trusting the client.
- The client sends a file's name from the four above and a key's path with its new value as text; never a path on disk or a whole file. The server checks the key exists in the model, decodes the value as the model would, and refuses anything else.
- Each change goes into the server's log with who made it, the old value, and the new one, and to WorldAPI's events when it runs.
- A few changes a second at most per player; the server reloads once they stop.

## Tests

- `core`: the clock, difficulty, anti-cheese decisions, the loot pick, the config line editor, and the moved decisions, as unit tests.
- `modcore/src/gametest`, on both loaders as WorldAPI's: a boss's stats and gear; its data surviving a save and a load; shares, loot, and rewards on death; difficulty and peaceful; a boat, a portal, and a trapped boss; the trigger; the commands and permissions.
- `runClientGameTest` on both loaders: a singleplayer world where a boss spawns, names read in the client's language, and the settings screen opens.
- The Paper check from CLAUDE.md after each step that touches `core` or `paper`.

## Order of work

1. Move the clock and the decisions to `core`; Paper plays as before.
2. Difficulty, in `core` and on Paper.
3. Anti-cheese, in `core` and on Paper.
4. modcore: the lifecycle, data on entities, the catalog, making and adopting bosses, spawns.
5. Combat, names, bars, visuals, despawns, loot, rewards, progress.
6. Abilities and minions.
7. The command, permissions, the menu, achievements and the trigger, languages.
8. Claims, WorldAPI, Allay, Text Placeholder API.
9. Bosses, gear, and enchantments from other mods.
10. The settings screen.
11. EMI, JEI, and REI.
12. `mobosses.mods=true`, CHANGELOG, both READMEs, MODRINTH.md, and the wiki's mods page; a release when the owner asks.
