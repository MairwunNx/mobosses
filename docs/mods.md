# Mo’Bosses on Fabric and NeoForge

How Mo’Bosses runs as a mod, as built on 2026-10-25. On a dedicated Fabric or NeoForge server it plays as the Paper plugin does: the same configs, the same `player_progress`, the same bosses. In a singleplayer or LAN world it plays the world you host. Players need no mod of their own; with it, they get the settings screen and boss loot in JEI, REI, and EMI.

## What the mods have that Paper has too

Everything the plugin does: bosses and classes, natural spawns on sight, every ability and signature, minions, names over mobs in each player's language, boss bars, glow and aura, loot, rewards, levels, the death penalty, the survival bonus, full moons, spawner fatigue, group health, despawns, the menu, the bestiary, achievements, `/mobosses` and its permissions, difficulty grading, anti-cheese, and WorldAPI and Allay.

## What only the mods have

| What | Players need the mod |
|---|---|
| Bosses from other mods' mobs, by id in `bosses.yml`, like `alexsmobs:grizzly_bear` | no |
| Other mods' weapons, armor, and enchantments in gear and loot, ranked by their attributes | no |
| `mobosses:boss_killed`, a trigger for a server's own advancements | no |
| Claims of Flan, Open Parties and Claims, and the mods behind Common Protection API | no |
| Text Placeholder API: `<papi:…>` tags in messages and `%mobosses:level%`, `%mobosses:xp%`, `%mobosses:kills%` | no |
| The settings screen: Mod Menu on Fabric, the mod list on NeoForge, the server's files from inside a world | yes, the one who edits |
| Boss loot as pages in JEI and REI, and in EMI through JEI's plugin | yes, with the viewer |

The mods add no items, blocks, or entities. `mobosses:boss_killed` is the one registry entry, a trigger type, which neither loader syncs to clients, so a vanilla game joins a server with Mo’Bosses.

## How the code splits

The family's way: what decides goes to `core` with tests, and what touches the game is written twice, once against Bukkit in `paper` and once against the game in `modcore`, each file next to its twin with the same name. Fabric and NeoForge differ only behind `Loader` and `ClientLoader`; modcore never asks which loader it runs on.

| Module | What it holds for the mods |
|---|---|
| `core` | The rules both platforms share: formulas, difficulty, the anti-cheese watch, the loot and reward rules, a kill's payout and its note, spawner fatigue, the hunt, `Progression`, achievements and their advancement JSON, permissions and their defaults, and the settings screen's editor (`config/edit/`) |
| `modcore` | The game side, one package per Paper package (`boss/`, `spawn/`, `combat/`, `ability/`, `menu/`, `achievement/`, `protect/`, `hooks/`, `lang/`), `Hooks` where the mixins land, `MoBosses` for one server, the messages between a player's game and the server (`net/`), and the client side (`client/`), which only the loaders' client entries load |
| `fabric`, `neoforge` | The entry, events to `Mod`, `OnFabric` and `OnNeoForge` (the mod list, permissions, the channel, Mo’Bosses' own files), the client entry with the settings button and the payload handlers, and the test mods |

### Moments the game hands in

Each mixin calls `Hooks`, which passes the moment to the server that runs now: a mob entering the world after `finalizeSpawn` told why (`LevelEntityMixin`, `SpawnReasonMixin`), entities coming and going with chunks (`TrackingMixin`), a hit once its health is gone (`HurtMixin`, `PlayerHurtMixin`), a death before its drops (`DeathMixin`, `PlayerDeathMixin`), boats, portals, a broken temporary block, ability shots, splashes, fangs, obelisks, and ability entities kept out of saves. The loaders hand in what they have events for: the server's start and stop, its ticks, players joining and leaving, the command tree, and the messages.

### Data on entities

Mo’Bosses' data sits in a compound under `mobosses` in the entity's own custom data, which the game saves with it and `/data` shows; a mixin carries a player's over to the entity they respawn as. The keys are Paper's without their namespace. Times in it are the server's clock: its ticks, which stand still while a singleplayer game pauses, so leaving for the menu sends no boss away.

### Names, bars, menus, advancements

- **Names over mobs** are text displays only their viewers' games know: sent by packet, riding the mob there, one per language among the players near it. Nothing goes into the world.
- **Boss bars** are the game's `ServerBossEvent`, one per language.
- **Menus** are chest windows whose items are only pictures, as Allay's.
- **Advancements** join the game's list each time it builds one (`AdvancementsMixin`), in `achievements.language`, and Mo’Bosses grants them as on Paper.

### Where files live

| What | Where | Why |
|---|---|---|
| `config.yml`, the other configs, `lang/`, `player-languages.yml`, `geoip/` | `config/mobosses/` of the game | One setup for every world; a player's language is theirs, not a world's |
| `bosses.db` and `temporary-blocks.txt` | `<save>/mobosses/` | Levels belong to a world, and move, copy, and go with its save |

A dedicated server keeps the database in `world/mobosses/`, and `database` in `config.yml` may still point it at MySQL, MariaDB, or PostgreSQL. A singleplayer world always uses SQLite in its save. The files Mo’Bosses copies come from its own jar through `Loader.resource`, never the class loader, which on Fabric hands out whichever mod's `config.yml` it finds first.

### Languages

A player reads their own choice, else the language their country speaks, else their game's. A singleplayer or LAN game's players come from local addresses no country has, so their game's language decides, and GeoIP doesn't run there at all.

### Permissions

`mobosses.*` as on Paper, through fabric-permissions-api on Fabric and PermissionAPI on NeoForge when a permissions mod answers, else by `Permissions.byDefault`: everyone gets `use` and `reset`, operators the rest, and `mobosses.config` only owners (level 4). The owner of a singleplayer world may reload and change the settings without cheats; spawning and removing bosses ask for cheats.

### The settings screen

It lists every value of `config.yml`, `classes.yml`, `abilities.yml`, and `rewards.yml` from the models' serializers, with the file's own comment and the default on hover. Done changes each value on its own line, so every comment stays (`core/config/edit/`), and only when Mo’Bosses reads the file back without falling back for it. From the main menu it edits `config/mobosses/`; in a world it asks the server over Mo’Bosses' channel, which checks `mobosses.config` on every message, takes a file and keys only, one batch a second, logs who changed what, and reloads. In singleplayer that's the world's own server.

### Loot pages

The server sends a player's game the loot table once they join and after each reload, each entry as it drops, named in their language, with lines for the lowest class, the boss levels, the boss types, and its odds. JEI and REI show them as a category of their own; EMI reads JEI's plugin. A game without the mod, or a viewer, never asks.

## Integrations

| Paper | The mods |
|---|---|
| GriefPrevention, WorldGuard | Flan, Open Parties and Claims, Common Protection API (`protection.claims`) |
| PlaceholderAPI, MiniPlaceholders | Text Placeholder API |
| CoreProtect | none yet: Ledger is Fabric only, and NeoForge has no common block log |
| Plan | none yet |
| WorldAPI, Allay | the same, through their expansion APIs |

## Tests

- `core`: the rules, the editor, the resources, as unit tests.
- `modcore/src/gametest`, on both loaders (`./gradlew :fabric:runGameTest`, `:neoforge:runGameTestServer`): a boss's data and stats, difficulty and peaceful, a save and a load, boats and portals, `/mobosses spawn`, natural spawns waiting to be seen, a solo kill's payout and loot, temporary blocks, the advancements, the trigger's conditions, and every ability cast once by a Mythic boss.
- A singleplayer world in a game window (`./gradlew :fabric:runClientGameTest`, `:neoforge:runClientGameTest`): Mo’Bosses on the game's own server, its files where they belong, the client's language, the owner's rights, a spawn, the settings screen changing the server's file, and the loot pages in JEI (Fabric) and REI (NeoForge). On macOS keep the display awake (`caffeinate -d`), or the window stalls before the menu.

What only a person at the game can judge, how a fight feels, the abilities' look, and the screens' layout at other sizes, goes to the owner.
