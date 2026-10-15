# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub and Modrinth.

## 2.1.0

### New

- 🏷️ **Names in each player’s language.** The names over bosses and minions now come in each viewer’s language, through text displays only they see. `name_tags.per_player: false` brings back the game’s own name tag in the default language.
- 🐲 **Eight new bosses:** Bogged, Blaze, Breeze, Cave Spider, Piglin Brute, Hoglin, Ravager, and Guardian, each with minions. A `bosses.yml` from 1.x or 2.0 doesn’t have them; copy them from the bundled file to get them.
- ✨ **Four new abilities.** A **shockwave** that knocks players back; a **frost nova** that freezes them as powder snow does; a **grapple** that pulls the farthest player in sight, against shooting a boss from a pillar; and **enrage**: once below 30% of its health, a boss hits harder and moves faster. A `classes.yml` without their levels keeps them off.
- 🧟 **Minions have better names** in every language, and they despawn as vanilla mobs do instead of filling the world.
- 📊 **`./gradlew balance`** prints the stat curves from the real formulas and a config; it replaces the Python scripts, which kept numbers of their own.

### Fairer progression

- Levels climb slower at first: 100 kills make level 19, not 29, so a new player doesn’t meet level 30 bosses after a weekend.
- A kill counts toward your level only when you dealt 15% of the boss’s health (`progression.min_share`), and the boss wasn’t 20 or more levels below you. Bosses below your level give you less Mo’Bosses experience, and stronger ones a little more (`progression.relevance`), so farming weak bosses doesn’t level anyone up.
- The players near a spawn set the boss’s level together, by their average (`spawn.group_radius`), so one veteran doesn’t pull newcomers into level 90 fights.
- A weaker boss is weaker by 5% to 30% of the level (`variance.min_cut`, `max_cut`), no longer by up to 80 levels.
- Vanilla experience from bosses dropped about sevenfold: a level 100 Mythic zombie gives 1286 points, not 9822.
- Level 100 can be reached; the curve used to stop at 99.

### Against piles of bosses

- After a boss spawns for a group, its players wait about a minute for the next (`spawn.player_cooldown_seconds`), and no boss spawns while three are within 48 blocks (`max_nearby`, `nearby_radius`). Mob farms and spawner rooms stay farms, not boss factories.
- A boss keeps at most 12 minions alive (`summoning_max_alive`), and minions that became bosses summon no minions of their own (`summoning_by_mini_bosses`).

### Kept across relogs and restarts

- Who hit a boss and how much, its cooldowns, and its enrage live on the boss: a restart mid-fight doesn’t cost anyone their loot or give the boss a free volley.
- The rewards cooldown and the spawn cooldown live on each player, so leaving and coming back doesn’t reset them.
- Cobwebs from abilities go back even after a crash, on the next start.
- Meteors and arrows from abilities aren’t saved, so a restart never drops one later.

### Fixed

- Toughness came from the mob’s own, which is zero for nearly every mob, so the toughness formula did nothing; skeletons got no armor the same way. Both grow from `base_points` now.

## 2.0.0

A rewrite for Paper 26.3 and Java 25. Your 1.x config files and your database load as they are; “Moved” below says what to look at.

### New

- 🌍 **Each player reads their own language.** The plugin finds the player’s country by IP, with DB-IP’s free database, and picks a language for it; when the country isn’t listed, it takes the language set in the game. English, Russian, Spanish, Portuguese, and German come bundled. Messages, ability warnings, boss bars, and the names and lore of loot follow each player’s language. The name tag over a boss stays in `locales.default`, since everyone sees the same one.
- 🛡️ **GriefPrevention and WorldGuard.** No boss spawns naturally in a claim, and no ability breaks, places, or ignites blocks there. In WorldGuard, the new `mobosses-spawn` and `mobosses-abilities` flags work alongside `mob-spawning`, `build`, and `mob-damage`. Every rule has a switch under `protection`.
- 🔎 **CoreProtect.** Blocks abilities and minions change go into CoreProtect’s log as `#mobosses`: `/co rollback u:#mobosses t:1h r:50` undoes them.
- 🧩 **PlaceholderAPI.** `%mobosses_level%`, `%mobosses_xp%`, and `%mobosses_kills%`, and `<papi:name>` tags in messages.
- 💾 **Bosses survive restarts and chunk unloads.** 1.x killed every boss on shutdown and on reload; now a boss keeps its type, class, and level on the entity and comes back with its chunk.
- ⌨️ **`/mobosses spawn` works from the console** with coordinates, and every argument has suggestions.
- ⚙️ **One bad value no longer resets a whole file.** It takes its default, and the log says which one. Bad game ids log a warning instead of silently turning into stone, a zombie, or flames.
- 📦 **The jar shrank from 31 MB to 14 MB:** Paper provides Adventure and the SQLite driver.

### Fixed

- Bosses force-loaded their chunk and often never released it, so chunks stayed loaded for good. 2.0 force-loads nothing. If your 1.x server ran for a while, `/forceload query` in each world shows the chunks left over, and `/forceload remove all` releases them, unless something else of yours needs them.
- Naturally spawned bosses used the class’s health multiplier for damage; they use the damage multiplier now, as bosses from `/mobosses spawn` always did. Natural Mythic bosses hit up to 30% softer than in 1.x, Rare ones up to 19% softer.
- `/mobosses reload` restarted the whole plugin and killed every boss; now it reloads the files and leaves the bosses be.
- `/mobosses killall` gave loot and experience to whoever had hit the bosses; now it removes them quietly.
- Loot never dropped from a class whose `display_name` differed from its key, as on servers that translated the class names.
- Rare loot announcements named the player who landed the last hit, not the one who got the item.
- A hit that overkilled a boss could count for more than its whole health, and cancelled hits counted too.
- Random durability ran backwards: Mythic loot came out the most worn. Better classes now come the least worn, as the settings always said.
- Enchanted books from loot carried their enchantment on the book itself, so an anvil couldn’t use it.
- `sweeping` in `bosses.yml` was unknown to 1.x and turned into Unbreaking; it means Sweeping Edge now. Loot that named `SIGN`, `SCUTE`, `CHAIN`, or `GOLD` dropped stone; it drops oak signs, turtle scutes, iron chains, and gold ingots now.
- Phantom, Vex, and Zombie Villager bosses in `bosses.yml` never spawned; they do now.
- Meteors turned into magma blocks where they landed; they break up on impact now.
- With nobody in range, the meteor shower fell on a player anywhere in the world.
- Lightning and cobwebs landed on the surface, or on the Nether’s roof, while the fight went on underground.
- The fire ring could put fire in place of water.
- Regeneration deleted tridents in its aura, along with arrows.
- Abilities hurt the boss’s own minions, pets, villagers, armor stands, and players in creative or spectator mode. They hurt players in survival and adventure only now.
- Cobwebs stayed for good when the server stopped before they went away.
- The teleport strike could put a boss inside a wall.
- An ability that found nobody to hit still sent its message and started its cooldown.
- The rewards cooldown didn’t stop the cleansing of negative effects.
- `max_bosses_per_world` counted the bosses of every world.
- A cooldown bias of 1.0 threw an error on every hit.
- With `check_vertical_zone` on, every spawn logged a warning.
- Database queries ran on the server thread, and a failed load could overwrite a player’s progress.

### Moved

- `messages.yml` is no longer read: messages live in `lang/<language>.yml`. Its switches moved to `broadcast` in `config.yml`, and the kill sound to `kill_sound`. The color tags `<pri>`, `<ok>`, `<warn>`, `<err>`, `<boss>`, `<num>`, `<hp>`, `<xp>`, and `<item>` still work.
- `language` in `config.yml` gave way to `locales`; an old `language: ru` still sets the default language until you set `locales.default`.
- Names moved to `lang/`. A `name` in `bosses.yml` or `minions.yml`, a `display_name` in `classes.yml`, and `title` and `lore` in `loottable.yml` still show to everyone as before; remove them to give each player their own language.
- `boss_bar.style` takes Adventure’s names, like `NOTCHED_20`; 1.x’s `SEGMENTED_20` still works.
- The loot and rewards damage threshold moved from the code to `loot.min_damage_share` in `config.yml` and `min_damage_share` in `rewards.yml`, with the same value.
- MySQL now goes through MariaDB’s driver, which serves both; the config stays the same.
- Paper only: Spigot and Bukkit aren’t supported anymore.

## 1.12.0

- The same code as 1.11.1, released on 2025-08-09.

## 1.11.1

- Fixed the version number.

## 1.11.0

- Mini bosses: a minion may turn into a boss a few levels below its summoner, by `mini_bosses_level_curve`.

## 1.10.0

- Players’ progress in SQLite, MySQL, or PostgreSQL.
- Each class unlocks abilities at its own levels, in `classes.yml`.

## 1.0.0–1.9.0

- Released together on 2025-08-02: bosses of six classes with scaled health, damage, armor, and toughness; nine abilities; minions; gear by level; loot tables with random durability; rewards; boss bars; player levels; and the commands `reload`, `spawn`, and `killall`.
