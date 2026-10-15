# Mo’Bosses

🏆 **Mo’Bosses** turns ordinary mobs on Paper servers into bosses. A zombie that spawns near you may come out as a level 40 Epic boss: more health, harder hits, armor, a glow in its class color, a health bar, and abilities like lightning, meteor showers, and minions. Beat it, and you get experience, a piece of loot, and a few potion effects. Every kill raises your own Mo’Bosses level, and the bosses that find you grow with it.

[Русская версия](https://github.com/MairwunNx/mobosses/blob/master/README.RU.md)

## What you get

- 🐲 **25 kinds of bosses**, from zombies and skeletons to blazes, breezes, ravagers, and guardians.
- ⚔️ **Six classes**, from Common to Mythic. Each multiplies a boss’s health, damage, experience, armor, and toughness, and decides which abilities it gets and when. Rarer classes come less often.
- 📈 **Levels that grow with you.** Each player has a Mo’Bosses level, from the experience and kills they’ve gathered. A boss takes the average level of the players near it, now and then a little lower, so weaker bosses keep turning up. Bosses far below your level count for little, so farming them levels nobody up.
- 💪 **Never weaker than vanilla:** a level 1 boss has at least its mob’s health, damage, and armor, and its gear only gets better.
- 🧮 **Scaling by curves.** Health, damage, and armor climb fast at first and level off toward a ceiling, so a level 100 boss is hard without being absurd. Class differences shrink at high levels.
- ✨ **Thirteen abilities:** lightning, minions, regeneration, a ring of fire, a ring of cobwebs, a teleport behind you, a meteor shower, an arc of arrows, arrow rain, a shockwave, a frost nova, a grapple that pulls archers off their pillars, and enrage at low health. Each class unlocks them at its own levels, with cooldowns.
- 🧟 **Minions** that a boss summons, up to a dozen at a time, and that may turn into weaker bosses themselves.
- 🗡️ **Gear by level:** a level 5 zombie swings a wooden sword; a level 60 one may carry enchanted netherite.
- 🎁 **Loot tables by level and class:** over 500 entries, 199 with their own names and lore, and random durability that favors better classes. Each player who dealt enough damage gets their own drop, which only they can pick up.
- 💪 **Rewards:** a few potion effects and absorption hearts after a kill, stronger for rarer classes.
- 📊 **Boss bars** with the boss’s health, shown to the players who hit it.
- 🌍 **Every player reads their own language,** even the names over bosses’ heads: English, Russian, Spanish, Portuguese, and German come bundled, picked by the player’s IP or game language.
- 🛡️ **Protected areas stay protected:** GriefPrevention claims and WorldGuard regions keep bosses and their abilities out.
- 🔎 **CoreProtect logging:** whatever bosses do to blocks shows up under `#mobosses`, ready to roll back.
- 🧩 **PlaceholderAPI**, if you have it: `%mobosses_level%`, `%mobosses_xp%`, and `%mobosses_kills%` for scoreboards and chat, and `<papi:…>` tags in messages.
- 🧩 **MiniPlaceholders**, if you have it: its tags work in messages, and `<mobosses_level>`, `<mobosses_xp>`, `<mobosses_kills>` work in TAB and elsewhere.
- 📊 **Plan**, if you have it: each player’s level, experience, and kills on their page, and the bosses alive on the server’s.
- 💾 **SQLite, MySQL, MariaDB, or PostgreSQL** for players’ levels.
- 🧯 **No piles of bosses:** a cooldown per player and a cap on bosses nearby keep mob farms and spawner rooms from turning into boss factories.
- ⚙️ **`/mobosses reload`** applies changes without a restart, and a fight survives restarts and relogs: bosses keep their level, cooldowns, and who hit them.

## My server

I play on Moonlands, a hard survival server I run for friends. It stays pure vanilla, so you won’t meet Mo’Bosses there, but you’re welcome to drop by.

[![Play on Moonlands: pure vanilla, hard mobs, AI, claims. minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/mobosses/master/media/og-adv-image.png)](https://minecraft.mairwunnx.com)

## Requirements

- Paper 26.3 or a fork of it, like Purpur
- Java 25

## Install

1. Download the jar from [Releases](https://github.com/MairwunNx/mobosses/releases).
2. Put it in your server’s `plugins/` folder and restart the server.
3. Look through the files in `plugins/MoBosses/`: the defaults play well, but spawn chances and classes are yours to tune.
4. Run `/mobosses reload` after a change.

## How it plays

When a mob spawns, Mo’Bosses looks up its type in `bosses.yml`. If a player is within `spawn.spawn_radius`, the mob becomes a boss with the type’s `spawn_chance`, times the multipliers for the world, the time of day, and the weather. The boss picks a class by weight, takes the average level of the players within `spawn.group_radius`, and gets its health, damage, gear, and name.

After a boss spawns for a group, its players wait about a minute for the next one, and no boss spawns while three are already close. Both live in `spawn` in `config.yml`.

Hitting a boss shows you its health bar. Each hit may set off an ability: a random one the boss has reached the level for, once the cooldowns have passed. Abilities hurt players in survival and adventure only; the boss’s own minions, pets, and villagers stay out of it.

When the boss dies, each player’s share of its health decides their part of the vanilla experience and the Mo’Bosses experience. The kill counts toward your level when you dealt 15% of the boss’s health and it wasn’t 20 or more levels below you; bosses below your level give you less experience. Players who dealt more than about a quarter of it (`loot.min_damage_share`) also get a piece of loot and the rewards.

Bosses keep their type, class, level, cooldowns, and who hit them how hard on the entity itself. They survive restarts and chunk unloads, and pick up their glow, bar, names, and abilities again when their chunk loads. The rewards and spawn cooldowns live on each player, so a relog doesn’t reset them. Temporary blocks, like cobwebs, go back when they expire, when the server stops, or, after a crash, on the next start.

## Languages

The plugin tries the sources in `locales.sources`, in order:

1. **`ip`:** the country of the player’s IP address. `locales.countries` maps languages to country codes: Russia, Belarus, and Kazakhstan get Russian; Spain and Latin America get Spanish; Brazil and Portugal get Portuguese; Germany, Austria, and Switzerland get German.
2. **`client`:** the language set in the player’s game, when a file in `lang/` matches it.

When nothing matches, the player gets `locales.default`. The country lookup needs no account and sends no address anywhere: on the first start, the plugin downloads DB-IP’s free country database (4 MB) into `plugins/MoBosses/geoip/` and refreshes it every 30 days. Players who connect through a proxy or a relay show up with its address, so they get the relay’s country.

Each player’s language covers chat messages, ability warnings, kill and loot announcements, boss bars (one bar per language among the boss’s viewers), the names over bosses and minions, and the names and lore of loot, in the language of the player who got it. The game shows one name tag to everyone, so the names are text displays riding the mob, one per language among the players near it, each shown only to its readers. Death messages, which the game writes the same for everyone, use `locales.default`; so does the plain name tag with `name_tags.per_player: false`.

`lang/<language>.yml` holds the messages and the names of classes, bosses, minions, and loot items. A key missing from a file comes from the default language, then from English; an empty text turns a message off. To add a language, copy `lang/en.yml` to, say, `lang/it.yml`, translate it, add `it: [ IT, SM, VA ]` to `locales.countries`, and run `/mobosses reload`.

## Config files

Every file lives in `plugins/MoBosses/`, and every key has a default, so you can delete the ones you don’t change. A value that doesn’t fit, like a word where a number goes, costs only itself: the log names it, and it takes its default. Game ids take the namespaced form (`minecraft:diamond_sword`) or the old enum names (`DIAMOND_SWORD`); a bad one logs a warning and drops out.

| File | What it holds |
|---|---|
| `config.yml` | Languages, spawning, player levels, the stat formulas, glow and particles, boss bars, loot durability, announcements, the database, protection, and CoreProtect |
| `classes.yml` | The six classes: their multipliers, colors, weights, and the level each ability starts at |
| `bosses.yml` | Which mobs become bosses, their spawn chance, gear by level, particles, and sounds |
| `abilities.yml` | Each ability’s switch, cooldown, and numbers by level |
| `minions.yml` | What each boss type summons |
| `loottable.yml` | Loot by class and level, with amounts, weights, enchantments, and names |
| `rewards.yml` | Potion effects and absorption hearts by class, and how long a player waits for the next ones |
| `lang/*.yml` | Messages and names, one file per language |

## Commands and permissions

| Command | Permission | What it does |
|---|---|---|
| `/mobosses reload` | `mobosses.reload` | Reloads every config and language file; bosses keep going |
| `/mobosses spawn <entity> <level> <class> [x y z [world]]` | `mobosses.spawn` | Spawns a boss two blocks ahead of you, or at the coordinates, which the console must give |
| `/mobosses killall` | `mobosses.killall` | Removes every boss in loaded chunks, without loot or experience |

`mobosses.admin` gives all three; operators have it by default. The command also answers to `/mb` and `/moboss`, and suggests every argument.

## Protected areas

Mo’Bosses works with each protection plugin only when that plugin runs; with none, everything is allowed. Each rule has its own switch under `protection` in `config.yml`, and all are on by default.

| Plugin | What it stops |
|---|---|
| [GriefPrevention](https://modrinth.com/plugin/griefprevention) | No boss spawns naturally inside a claim, and no ability breaks, places, or ignites blocks there. Bosses may still walk in and fight, as vanilla mobs do. |
| [WorldGuard](https://modrinth.com/plugin/worldguard) | No boss spawns naturally in a region with owners or members, a player’s plot, unless the `mobosses-spawn` flag allows it there. Elsewhere bosses spawn where `mobosses-spawn` and WorldGuard’s `mob-spawning` allow it, so a fresh setup keeps them everywhere but in plots. Abilities change blocks only where `mobosses-abilities` allows it and `build` lets non-members build. Ability damage to players follows `mob-damage`. |

Set the flags as any other: `/rg flag spawn mobosses-spawn deny`. `/mobosses spawn` ignores protection on purpose, since an admin asked for the boss; set `protection.admin_spawn_bypass: false` to make it ask too.

## CoreProtect

With [CoreProtect](https://modrinth.com/plugin/coreprotect) installed, every block an ability breaks, places, ignites, or changes goes into its log under `#mobosses`, in the same tick, before the change happens. Blocks bosses and minions blow up or pick up go there too. To look up and undo a boss’s damage:

```
/co lookup u:#mobosses t:1h r:50
/co rollback u:#mobosses t:1h r:50
```

CoreProtect’s API takes block changes only, so the loot bosses drop stays out of its log; bosses never open containers. Rename the user with `coreprotect.user`, or turn logging off with `coreprotect.enabled: false`.

## PlaceholderAPI

With [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) installed:

| Placeholder | What it shows |
|---|---|
| `%mobosses_level%` | The player’s Mo’Bosses level |
| `%mobosses_xp%` | Their Mo’Bosses experience |
| `%mobosses_kills%` | Bosses they helped kill |

In messages, write `<papi:name>` to show `%name%` for the player who reads it. Mo’Bosses doesn’t look for `%…%` in your text, so a percent sign stays a percent sign.

## MiniPlaceholders

Every message is [MiniMessage](https://docs.advntr.dev/minimessage/format). With [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) installed, messages take its tags as they are, for the player who reads them: `<luckperms_prefix>`. Mo’Bosses adds three tags of its own, each about the player they’re shown for:

| Tag | What it shows |
|---|---|
| `<mobosses_level>` | The player’s Mo’Bosses level |
| `<mobosses_xp>` | Their Mo’Bosses experience |
| `<mobosses_kills>` | Bosses they helped kill |

## Plan

With [Plan](https://modrinth.com/plugin/plan) installed, Mo’Bosses adds its section to Plan’s pages. A player’s “Plugins” tab shows their level, experience, and kills; the level is also a column in the players table. The server’s “Plugins Overview” shows the bosses alive. Plan keeps the last values while a player is away.

## Databases

Players’ levels live in `bosses.db`, an SQLite file, out of the box. Set `database.type` to `mysql` (for MySQL or MariaDB) or `postgres` and fill in its section to share levels between servers; the plugin creates its table on the first start. A database that can’t be reached doesn’t stop the plugin: levels then last until each player leaves. Queries never run on the server thread.

The MariaDB and PostgreSQL drivers come inside the jar, about 2 MB together, so the plugin works on servers without internet access; Paper ships SQLite’s.

## Upgrading from 1.x

Your 1.x files and your database load as they are. What changed:

- `messages.yml` is no longer read. Messages live in `lang/`, one file per language; their switches moved to `broadcast` in `config.yml`, and the kill sound to `kill_sound`.
- `language: ru` in `config.yml` still sets the default language, until you set `locales.default`.
- A `name` in `bosses.yml` or `minions.yml`, or a `display_name` in `classes.yml`, shows to everyone as before. Remove it to use the names in `lang/`, each player in their own language. Loot with `title` and `lore` works the same; the bundled loot uses `id` instead.
- `boss_bar.style` takes `NOTCHED_20` and the like; `SEGMENTED_20` still works.
- An old `bosses.yml` lacks the bosses 2.1 added, and an old `classes.yml` the levels for its new abilities, which stay off until you add them. Copy them from the bundled files, which the jar holds and `plugins/MoBosses/` gets when a file is missing.

The [changelog](https://github.com/MairwunNx/mobosses/blob/master/CHANGELOG.md) lists what 2.0 fixed.

## Build

You need JDK 25; Gradle downloads it when it’s missing.

```shell
git clone https://github.com/MairwunNx/mobosses
cd mobosses
./gradlew build
```

The jar lands in `build/libs/`. Releases come from the “Release” workflow on GitHub, which takes the version as input. `./gradlew balance` prints the stat curves, experience, and player levels from the real formulas, for the bundled config or, with `--args=path/to/plugins/MoBosses`, for yours.

## Stack

- Kotlin and Gradle Kotlin DSL
- Paper API 26.3 and Adventure MiniMessage
- kotlinx-serialization with kaml for the configs
- Exposed and HikariCP over SQLite, MySQL, MariaDB, or PostgreSQL
- MaxMind DB reader for the country database

## AI involvement

AI wrote parts of the documentation and the code: the configuration models and serializers at first, then the 2.0 rewrite with languages by IP, protection plugins, and CoreProtect, and the translations. The formulas, classes, and balance are mine. AI also writes the commit messages from the changes.

## Related projects

[Allay Server](https://github.com/allay-ai/server)—🛠️⚡ the backend for Allay (formerly MCBuddy), an AI helper for Minecraft, built on OpenRouter.

[Allay](https://github.com/allay-ai/minecraft)—🪽 brings Allay into the game: ask the AI anything with `/ask`, right in the chat. 🎮

## From the same author

[Mo’Joins](https://github.com/MairwunNx/mojoins)—🎉 greets players in their own language: join and quit lines, a welcome for newcomers, sounds, and particles.

[Mo’Afks](https://github.com/MairwunNx/moafks)—🛡️ a safe AFK mode: type `/afk` or stand idle, and nothing hurts you, mobs leave you alone, and you don’t keep the night from passing.

## License

MIT, see [LICENSE](https://github.com/MairwunNx/mobosses/blob/master/LICENSE).

## Credits

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.

> 🫡 Made by Pavel Erokhin (Павел Ерохин), aka mairwunnx.
