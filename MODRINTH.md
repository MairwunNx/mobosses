# Mo’Bosses

🏆 **Mo’Bosses** turns ordinary mobs into bosses. A zombie that spawns near a player may come out as a level 40 Epic boss: more health, harder hits, armor, a glow in its class color, a health bar, and abilities like lightning, meteor showers, and minions. Beat it, and you get experience, a piece of loot, and a few potion effects. Every kill raises your own Mo’Bosses level, and the bosses that find you grow with it.

## What you get

- 🐲 **25 kinds of bosses**, from zombies and skeletons to blazes, breezes, ravagers, and guardians.
- ⚔️ **Six classes**, from Common to Mythic. Each multiplies a boss’s health, damage, experience, armor, and toughness, and decides which abilities it gets and when.
- 📈 **Levels that grow with your players.** A boss takes the average level of the players near it, now and then a little lower. Weak bosses count for little, so farming them levels nobody up.
- 🧮 **Scaling by curves:** stats climb fast at first and level off toward a ceiling, so a level 100 boss is hard without being absurd.
- ✨ **Thirteen abilities:** lightning, minions, regeneration, a ring of fire, a ring of cobwebs, a teleport behind you, a meteor shower, an arc of arrows, arrow rain, a shockwave, a frost nova, a grapple against pillar archers, and enrage, each unlocked by class and level.
- 🗡️ **Gear by level**, from a wooden sword to enchanted netherite.
- 🎁 **Loot tables by level and class:** over 500 entries, 199 with their own names and lore. Each player who dealt enough damage gets their own drop.
- 💪 **Rewards:** potion effects and absorption hearts after a kill.
- 📊 **Boss bars** for the players who hit the boss.
- 🌍 **Every player reads their own language,** even the names over bosses’ heads: English, Russian, Spanish, Portuguese, and German come bundled, picked by IP or game language.
- 🛡️ **GriefPrevention and WorldGuard:** no bosses or block-changing abilities in claims and protected regions.
- 🔎 **CoreProtect:** whatever bosses do to blocks is logged under `#mobosses`, ready to roll back.
- 🧩 **PlaceholderAPI:** `%mobosses_level%`, `%mobosses_xp%`, `%mobosses_kills%`, and `<papi:…>` tags in messages.
- 💾 **SQLite, MySQL, MariaDB, or PostgreSQL** for players’ levels.
- 🧯 **No piles of bosses:** a cooldown per player and a cap on bosses nearby keep mob farms from turning into boss factories.
- ⚙️ **`/mobosses reload`** applies changes without a restart, and a fight survives restarts and relogs.

## Install

1. Put the jar in your server’s `plugins/` folder and restart the server.
2. Look through the files in `plugins/MoBosses/`: the defaults play well, but spawn chances and classes are yours to tune.
3. Run `/mobosses reload` after a change.

Mo’Bosses needs Paper 26.3 or a fork of it, like Purpur, on Java 25. Configs and databases from 1.x load as they are.

## How it plays

When a mob of a type in `bosses.yml` spawns near a player, it may become a boss, with its chance times the multipliers for the world, the time of day, and the weather. Each hit on a boss may set off an ability. Abilities hurt players in survival and adventure only; pets and villagers stay out of it. When the boss dies, each player’s share of its health decides their experience, and those who dealt more than about a quarter of it get loot and rewards.

## Languages

The plugin finds each player’s country by IP and picks a language for it; when the country isn’t listed, it takes the language set in the game. Chat messages, ability warnings, announcements, boss bars, loot names, and the names over bosses and minions follow each player’s language. Only death messages, which the game writes the same for everyone, come in the default language. Add a language by copying `lang/en.yml` and listing its countries in `config.yml`.

The country lookup needs no account and sends no address anywhere: the plugin downloads a free country database (4 MB) and refreshes it every month.

## Protected areas

| Plugin | What it stops |
|---|---|
| GriefPrevention | No boss spawns naturally inside a claim, and no ability breaks, places, or ignites blocks there. Bosses may still walk in and fight, as vanilla mobs do. |
| WorldGuard | Bosses spawn only where the `mobosses-spawn` flag and `mob-spawning` allow it. Abilities change blocks only where `mobosses-abilities` allows it and `build` lets non-members build. Ability damage follows `mob-damage`. |
| CoreProtect | Every block an ability or minion changes is logged as `#mobosses`: `/co rollback u:#mobosses t:1h r:50` undoes it. |

Each rule has its own switch in `config.yml`, on by default. `/mobosses spawn` ignores protection, since an admin asked for the boss, unless `protection.admin_spawn_bypass` is off.

## Commands and permissions

| Command | What it does |
|---|---|
| `/mobosses reload` | Reloads every config and language file |
| `/mobosses spawn <entity> <level> <class> [x y z [world]]` | Spawns a boss ahead of you, or at the coordinates |
| `/mobosses killall` | Removes every boss in loaded chunks, without loot |

Each needs its own permission: `mobosses.reload`, `mobosses.spawn`, `mobosses.killall`. `mobosses.admin` gives all three; operators have it by default.

## Links

- Source code, issues, and the full docs: [GitHub](https://github.com/MairwunNx/mobosses)
- From the same author: [Mo’Joins](https://modrinth.com/plugin/mojoins), join and quit lines in each player’s language, and [Mo’Afks](https://modrinth.com/plugin/moafks), a safe AFK mode

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.
