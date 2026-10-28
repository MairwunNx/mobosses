# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub and Modrinth.

## 2.18.0

### Fixed

- 🏰 **Mobs a structure places can be bosses.** A bastion’s piglin brutes, a mansion’s vindicators and evokers, and a monument’s guardians come with the world as it generates, which neither Paper nor the game announces as a spawn, so none of them ever rolled; brutes, found nowhere else, never came as bosses at all. Now such a mob counts as `CHUNK_GEN` when it first enters the world and rolls when a player first sees it, like any other. `CHUNK_GEN` joins the default `spawn.allowed_spawn_reasons`; add it to your own list if you keep one. Structures someone already walked through keep their mobs as they are, since nothing tells those apart from any other old mob.

## 2.17.0

### New

- 🐷 **Zombified piglins can be bosses**, the grown ones only: 5% before the multipliers, the Nether’s ×1.4 on top. One keeps to itself like any zombified piglin until someone hits it. It wears a piglin’s gold, netherite from about level 50, and summons grown zombified piglins. Its war cry rallies every piglin within 12 blocks and turns the zombified ones on its foe, who then has the crowd after them. It drops gold ingots, enchanted golden swords from level 5, and, from Rare bosses at level 20, a gold block. A new `adults_only` key in `bosses.yml` keeps a type’s babies ordinary.

## 2.16.0

### Changed

- 🦇 **The dragon and the wither send a pack to each player.** Their summons came out at the boss, high in the air or far off, where phantoms circled until someone wandered near. Now each player the dragon or the wither fights gets a pack of their own, set on them at once: phantoms 8 to 12 blocks over their head, wither skeletons on the ground a few blocks off. Each player has room for `summoning_max_alive` of them. Other bosses still summon at their feet.

## 2.15.0

### New

- 🌑 **Blindness.** The boss gathers darkness, with ink and a low warning, and a second later blinds every player in the fight still within 10 blocks, up to 16 by level, for 3 seconds and a tenth more a level, up to 11. Run at the warning and you get away; run 8 blocks past the radius once blind, and it lifts early, as it does once the boss is gone. Blindness from a potion or anything else stays. It starts at level 45 for a Common boss and 15 for a Mythic one; `blindness` in `abilities.yml` and `classes.yml` holds the numbers.

### Changed

- 🐉 **The dragon and the wither fight with everything they have.** A boss high over the fight looses its ring of arrows down at the ground around it instead of over everyone’s heads. The dragon summons phantoms, which fly, and the wither summons wither skeletons. Enrage’s Strength now adds 3 a level to the dragon’s blows and breath and to both bosses’ shots, which it never reached.
- ⚡ **Lightning hits harder where it lands.** Each bolt past every player’s own now strikes one of them outright by a 15% chance, up to three bolts a player in all, instead of always falling around them. Each bolt also sets the block it strikes alight half the time, as the game’s lightning does, for 8 seconds, though fire it spreads burns on as usual; it never does within 6 blocks of a claim or a protected region. `lightning_aimed` and `lightning_fire` in `abilities.yml` set the chances, the cap, the seconds, and the distance.

## 2.14.0

### Changed

- 🎯 **A boss turns on whoever hurts it most.** A vanilla creeper keeps chasing its first target however hard someone else hits it, and as a boss that left one player to take every blow. Now each player’s damage counts as threat, fading by half every 6 seconds, and a hit from a player whose threat beats that of the boss’s target by a quarter draws the boss to them. An even fight keeps it where it is. `aggro` in `config.yml` sets the fade and the margin, or turns it off.
- 🔥 **The fire ring burns around every player in the fight**, up to four, not only the nearest. It now burns next to grass, flowers, and trees, and on water as flames dancing on it that burn whoever wades through, twice a second unless they have Fire Resistance. Next to anything else that burns, like a house, it still never does. `fire_ring_near` in `abilities.yml` turns plants, trees, and water off one by one.
- 🏹 **The arrow arc is a ring of arrows** in every language, since it flies out all around the boss; an archer’s own aimed volley is the fan that flies at you.

## 2.13.0

### New

- 🐉 **The Ender Dragon and the Wither can be bosses.** Each rolls like any mob, 25% before the multipliers, with no cooldown or crowd in the way, since each is a fight someone set up. A built wither counts through the new `BUILD_WITHER` in `spawn.allowed_spawn_reasons`; add it to your own list if you keep one. The dragon never turns in a world’s first fight, only once it’s been beaten and comes back. Both keep the game’s own health bar, in its own color, with the boss’s name, class, and level on it, never despawn, and never cast abilities that would move them, pull you up to them, copy them, or build around them. The dragon’s blows and breath grow with its level, and its loot drops at each player’s feet instead of over the exit portal. Their experience stays near the game’s own up to about level 30 and tops out like a level 100 Mythic’s.
- 🍖 **More of the game’s own drops.** What a boss’s mob always drops, Looting’s extra included, comes times 1 + level × 0.03 × its class’s share, from 1 for Common to 2 for Mythic: ×1.3 from a level 10 Common, ×3.25 from a level 50 Epic. A fraction adds one more by its own chance, so on average it comes out exact. The drops lie on the ground for everyone as before, spawner fatigue cuts the extra, and the boss’s gear, items that don’t stack, nether stars, the dragon egg, and heads drop as they are. `loot.vanilla_drops` in `config.yml` holds the numbers and the list.

### Changed

- ⛓️ **The chain hauls you in like a rope.** It pulled once a second, too weakly to matter. Now, past its length, it pulls you back in heaves, hard on the heave and slack between, while the boss shortens it from 8 blocks to 3; it still hurts once a second. A sprint between heaves gains ground, and standing still gets you dragged in. An ender pearl past its slack gets you yanked back at once. The new keys under `chain` in `abilities.yml` set the reel, the pull by level, and the heave.

### Fixed

- 🔥 **No more fire ring without a ring.** Fire never goes next to anything that burns, so in grass, under leaves, or by a claim the ring could come out as a spark or two, while the boss still announced it. Now a ring with room for less than a third of its fire isn’t cast, and the boss picks something else.

## 2.12.0

### Changed

- 🎁 **Strong bosses pay like strong bosses.** Up to now a boss picked its loot from one big pile, where the many Common and Uncommon entries outweighed its own class: a Legendary boss dropped Legendary loot about one time in ten, and an Epic one time in six. Now a boss picks a class first, its own or one below at the same ×0.6, ×0.3, ×0.15, ×0.08, ×0.03 as before, then an entry within it, so a Rare, Epic, or Legendary boss drops loot of its own class about half the time, however many entries the classes below have.
- 🎒 **More items from higher levels.** Each player who earns loot gets one item, two from level 20, and three from level 40. `loot.items` in `config.yml` sets the step and the cap; `every_levels: 0` keeps one item.
- 🧹 **Less filler from level 10 to 70.** Coal, bones, paper, sugar, cooked meat, raw ore, quartz, prismarine, chainmail, shears, buckets, fishing rods, saddles, leads, and name tags leave those levels. In their place come enchanted books (Protection, Sharpness, Efficiency, Unbreaking, Power, Feather Falling, and from Rare bosses Mending), iron armor and tools with enchantments, diamonds, emeralds, and golden apples, with diamond gear for Uncommon bosses from level 17. Rare and better bosses drop more diamonds, emeralds, and experience bottles at a time, and a Legendary one may drop an enchanted golden apple. Resin clumps stop at level 15.

A server whose `loottable.yml` is the bundled one gets the new table on start; one you edited keeps yours, and the new class-first picking applies to it too.

## 2.11.1

### Changed

- 🔧 **Fresher libraries.** The MySQL and MariaDB driver moves to 4.0.0 and kotlinx.serialization to 1.12.0. The new driver checks TLS by default, so Mo’Bosses now asks it for a plain connection outright when `database.mysql.ssl` is `false`, which keeps your database connecting as before. The NeoForge mod is built against NeoForge 26.3.0.99 and asks for it or newer.

## 2.11.0

### New

- 🧱 **Mo’Bosses comes to Fabric and NeoForge.** Put `mobosses-fabric-2.11.0.jar` or `mobosses-neoforge-2.11.0.jar` in `mods/` on a Minecraft 26.3 server, and it plays as on Paper: the same bosses, abilities, loot, levels, menu, achievements, and configs, with players joining from a vanilla game. Put it in your own game, and your singleplayer and LAN worlds get bosses too, each world keeping its own hunters in its save. The mods keep bosses out of the claims of Flan, Open Parties and Claims, and the mods behind Common Protection API, fill `<papi:…>` through Text Placeholder API, and talk to WorldAPI and Allay as the plugin does. Bosses can come from other mods’ mobs, and loot and gear take other mods’ items and enchantments by id. Players with the mod get a settings screen, from Mod Menu or NeoForge’s mod list, which changes the files in the main menu and the server’s own in a world when they have `mobosses.config`, and boss loot as pages in JEI, REI, and EMI. Data packs get `mobosses:boss_killed` for advancements of their own. [Fabric and NeoForge](https://github.com/MairwunNx/mobosses/wiki/Mods) in the wiki has it all.
- ⚖️ **The game’s difficulty grades bosses.** On hard they stay exactly as they were. Normal bosses have about 88% of the health and damage, easy ones about 73%, and a hardcore world’s about 123%; armor, minions, cooldowns, and how often bosses come move by half as much, and the experience they drop with their health. Peaceful makes no bosses. A boss keeps what it was made with, so a change of difficulty moves only the bosses after it, and none is ever weaker than its vanilla mob. On easy or normal, your bosses get softer with this update; `difficulty.enabled: false` in `config.yml` keeps them as on hard. The modifiers and weights live under `difficulty`, and `./gradlew balance` prints them.
- 🧀 **No more cheese.** Bosses no longer get into boats or minecarts, and they stay out of portals. A boss you keep hitting from a pillar, over a wall, or while it sits in a hole or under a trapdoor steps out next to you after a few seconds (8 when it can’t reach you, 6 when it can’t move), and pulls you down when nothing next to you fits, as on top of a pillar. It does so once in 20 seconds at most, and an archer that can see you just keeps shooting. Every part has its switch under `anti_cheese` in `config.yml`.

### Changed

- 📈 **Levels grow from experience alone, and you see how far the next one is.** Up to now kills raised your level along with experience, and experience counted so little that the card read like “level 20, 97 kills, 95 experience”. Now each level asks for a set amount of Mo’Bosses experience, a kill gives about a hundred times what it did, and `/mobosses info` shows the way to the next level, like “To level 21: 799/1 598 · 50%”. Levels come about twice as fast as before: level 20 after some 50 kills of bosses at your level instead of 100, level 90 after 300 instead of 840, and level 100 after about 400, where the old curve took over six thousand. Kills still count for achievements, the bestiary, and the board of kills. Your players keep their levels: the first start gives everyone the experience that holds their level and their way to the next, and keeps the old rows in `player_progress_before_2_11`. Don’t go back to an older Mo’Bosses on the same database without restoring them. The new keys are `base`, `climb`, `knee`, and `steepness` under `progression.level_curve`, and `scale` under `exp_gain`; [Player levels](https://github.com/MairwunNx/mobosses/wiki/Player-levels) has the table.

### Fixed

- 🩹 **A boss you fight alone pays all of its share again.** Damage with no player behind it, a fall after you knock the boss back, lava, a cactus, poison, drowning, or fire your Fire Aspect or Flame didn’t light, went to nobody, so your share, and the experience, loot chances, and kill that hang on it, came out short of what you earned. Now it goes to the players who hit the boss in the last 30 seconds, split by what each dealt, so you keep it all alone and a group splits it as it fought. TNT you lit counts as your own hit.

## 2.10.0

### New

- 🤖 **Allay knows your bosses.** With [Allay](https://github.com/allay-ai/minecraft) 6.17.0 or newer on the server, the AI helper can answer “what can this boss do?”, “how rare is Mythic here?”, or “how far is my next level?” from your server’s own settings. Mo’Bosses hands it four tools: the classes with their chances, multipliers, and the levels abilities start at; the mobs that become bosses, with their minions and signature abilities; the asker’s level, experience to the next one, place among hunters, and bestiary; and the bosses within 64 blocks of them. Every tool only reads, and names come in the language Allay answers in. Without Allay nothing changes.

### Changed

- 📦 **The jar is now `mobosses-paper-2.10.0.jar`.** Mo’Bosses is getting ready for Fabric and NeoForge, and each platform’s jar will carry its name. On Paper it works exactly as before; delete the old `mobosses-2.9.0.jar` from `plugins/` when you put in the new one. The GitHub release still attaches `mobosses.jar`, so links to `releases/latest/download/mobosses.jar` keep working.

## 2.9.0

### Changed

- 🧩 **Mo’Bosses talks to WorldAPI through its new expansion API.** The data at `/extensions/mobosses` and the `ext.mobosses.*` events stay exactly as they were: the same paths, the same JSON, the same public and private parts, so sites, bots, and dashboards notice nothing. Mo’Bosses now needs WorldAPI 3.4.0 or newer for it; with an older one it says so in the log and runs on without sharing its data. WorldAPI drops its older bridge, which Mo’Bosses used until now, in 4.0.0.

## 2.8.0

### New

- 🔄 **Files you never changed follow the plugin.** Up to 2.7 the plugin copied `loottable.yml`, `bosses.yml`, `classes.yml`, `abilities.yml`, and the rest into `plugins/MoBosses/` once and never again, so a server that started on 2.1 kept 2.1’s loot, cobblestone stacks included, through every update. Now, on start, a file equal to a copy any 2.x release shipped gets this version’s, and the log says `updated to this version’s`. A file you edited stays as it is; delete it to take the new defaults.

### Changed

- 🧹 **No junk blocks in the loot:** the last one, oak logs for Common bosses up to level 5, gives way to leather leggings and boots, at the same weight, so every other item comes exactly as often as before. A test now keeps stone, dirt, gravel, sand, logs, planks, and the like out of the table.

## 2.7.0

### New

- 🎁 **Each boss drops what its mob is known for:** entries in `loottable.yml` can name boss `types`, and every type got a few. An evoker drops emeralds, experience bottles, and Protection and Sharpness books; a pillager arrows and crossbows; a breeze breeze rods, wind charges, and trial keys; a guardian sponges and, at legendary, the Tide trim. They join the shared table, so about one drop in ten comes from them. Their supplies grow by half the table’s rate per level, from 2–4 emeralds at the start to 6–12 at level 50, and single valuable items, like a saddle or a trial key, don’t grow at all (a new `per_level` key on any entry).
- 🎲 **Bonus loot from rare mobs:** a boss from a raid, mansion, bastion, fortress, trial chamber, or monument may drop one more of its own items (`bonus_loot_chance` in `bosses.yml`): an evoker or a ravager half the time, a piglin brute or a breeze about a third. One per fight, however many fought, for one of the players who got loot, picked by their shares.

### Changed

- 🧹 **`arrow_arc_vertical_spread` is gone** from `abilities.yml`: nothing read it.
- 🧹 **Less junk, more to fight with:** even the first levels drop stone swords and axes, bows, leather armor, and shields instead of sticks, dirt, and seeds, and from level 6 iron swords, chainmail, and iron ingots join in. From level 10 cobblestone, planks, wool, crafting tables, furnaces, signs, and observers give way to arrows, shields, chainmail, food, gunpowder, lapis, experience bottles, ender pearls, and shields. At level 17, junk fell from about half of all drops to 2%. The weights stay, so rare and better items come as often as before.
- 🧱 **Blocks and ingots instead of crumbs:** redstone is gone from the table, nuggets and dust turn into ingots from level 10, and blaze powder into rods. From level 20 a boss sometimes drops a ready block, iron, gold, lapis, copper, and rarely an emerald or, from level 50 on an Epic boss, a diamond block; blocks don’t grow with the level. High levels give more ingots: 6–12 iron from level 30.


### Fixed

- 🏠 **A boss forgets whoever left the fight.** Since 2.6.3 lightning, meteors, and arrow rain reach the players a boss fights at any distance, but that took in everyone who had ever hit it: a player who struck once and went home got lightning there. Now a player stays in the fight for 30 seconds after their last hit, or their pet's.
- 💀 **The killing blow casts nothing:** a dying boss no longer summons minions or splits into doubles with its last breath.
- 👥 **Doubles leave with their boss.** Copies outlived a boss killed before they faded, with its gear, name, and glow. The real boss now lands only where it fits and outside claims, never in a wall.
- 🪂 **Grapple doesn't kill with the fall:** slow falling lasts as long as the drop needs, not a flat 3 seconds.
- 🔥 **The fire ring keeps away from anything that burns** within the fire's reach, a block to each side and four above, not only next to it.
- 🌀 **Arrow rain, fireballs, and tridents** stop when their target leaves for another world.
- 🟣 **Swap** needs the enderman to see you and to fit where you stand: no swaps through walls or into a two-block tunnel.
- 💚 **Regeneration no longer ends phase two’s protection early:** its effects run out by themselves.
- 🛠️ **Grapple, pounce, and stampede** no longer fail when a player stands right above the boss.

## 2.6.4

### Fixed

- 🎯 **Aimed abilities go for the players the boss fights.** They used to pick the nearest player within their own range, so they hit a bystander who never joined the fight and missed a fighter a step farther away. Now they go for the player the boss chases, then the one whose hit set them off, then everyone who hit it, nearest first. This covers the meteor shower, arrow rain, fire and web rings, teleport strike, chain, grapple, and the signature volleys, fangs, fireballs, tridents, potions, wind charges, pounce, stampede, and swap.
- ☄️ **Meteors, arrow rain, and the fire and web rings reach you anywhere**, as lightning does since 2.6.3. Meteors looked for a target only 6 to 14 blocks from the boss and fell on nobody past that. The fire ring no longer flares up around the boss when it finds no one. `meteor_shower_radius_*` and `arrow_rain_target_search_radius` are gone from `abilities.yml`; old files still load. Projectiles, leaps, and teleports keep their ranges.

## 2.6.3

### Fixed

- ⚡ **Lightning finds its targets:** it looked for a player only within its own small radius of the boss, 3 to 10 blocks, and struck around the boss itself when you stood a step farther or shot from a bow. Now every player the boss fights gets a bolt, however far: whoever it chases, whoever set the ability off, and everyone who hit it. The other bolts scatter around them. A lightning rod or copper next to a player still draws that player’s bolt.

## 2.6.2

### Fixed

- 🔥 **Fire counts for whoever lit it:** a boss set alight by a Flame bow, a Fire Aspect blade, or a pet burned in nobody's name, so its burning took part of the kill from the player. A Flame bow alone left about a third of a weak boss to nobody: less experience, and sometimes no loot or kill. Now the fire's damage goes to whoever lit it, and fire from lava or the sun still goes to nobody.

## 2.6.1

### Changed

- 📦 **Built against newer libraries:** Paper API 26.3 build 205, Exposed 1.6.0, and WorldGuard 7.0.20. Nothing changes in the files or the commands.

## 2.6.0

### New

- 🌍 **Players pick their own language:** `/mobosses language <code>` sets the language Mo’Bosses speaks to you, like `ru` or `pt`, and `/mobosses language auto` hands it back to the country and game-language rules. Alone, or through the new Language button in the menu, it opens a picker with each language under its own name and Automatic; the current one glints, and a pick takes you back to the menu in the new language. The choice beats every automatic rule, from chat and ability warnings to boss bars, names over mobs, and loot, and survives restarts in `player-languages.yml`. It needs only `mobosses.use`, which everyone has.

## 2.5.0

### Changed

- 👀 **Bosses come where players are.** A mob no longer rolls as it spawns, mostly in caves nobody walks into, where it used to take the players’ cooldown with it. It waits until a player within 32 blocks could see it and rolls then, with a puff of smoke if it comes out a boss. Each mob rolls once in its life: walking off and coming back never rolls it again, and mobs that got a name, an owner, or a leash by then never roll. Minions still roll as they’re summoned (`spawn.on_sight`; off brings back the old way).
- 🎲 **Bosses come a quarter more often:** the world multipliers are 1.25 in the overworld, 1.4 in the Nether, and 1.6 in the End, up from 1.0, 1.1, and 1.3 (`spawn.world_multipliers`).

## 2.4.0

### New

- 🏆 **Achievements in a tab of their own** (press L): the hunt’s path by level (10 to 100) and by kills (10 to 1,000), the ladder of classes from Uncommon to Mythic, the bestiary and the Mythic collection, and feats: break an obelisk, break a chain, find the real one among doubles, win in the second phase, win alone at level 50 and up, win untouched at level 30 and up, hunt under a full moon, and avenge a death within 10 minutes. They toast and show in the chat as the game’s own do; levels and kills from before count at the next login. One language for all, as the game shows them (`achievements`).
- 📖 **A bestiary in the menu:** every boss type, with your kills and best level in each class, and “???” for the ones you haven’t beaten; it counts from this version on, in a new `player_boss_kills` table.
- 🔌 **Mo’Bosses’ data in [WorldAPI](https://modrinth.com/plugin/worldapi)** 2.3 and newer, if it runs: the best hunters, leaderboards by kills, level, or experience in WorldAPI’s own shape, a player’s card and bestiary, and the boss counts for everyone at `/extensions/mobosses/…`; the bosses alive, with coordinates, for tokens only. Kills, level-ups, achievements, deaths to bosses, second phases, and full moons stream as `ext.mobosses.*` topics, and kills, levels, achievements, and full moons land in its event history. Mo’Bosses keeps its data; WorldAPI reads it when asked.
- 🔭 **The bosses in the world, for admins** (`mobosses.bosses`): a menu page of the bosses in loaded chunks, nearest first, 45 to a page. A click shows a boss’s details, Shift and a click takes you to it, and a right click, twice, removes it.

## 2.3.0

### New

- 🔄 **Start over:** `/mobosses reset` wipes your level, experience, and kills and takes you off the board, after `/mobosses reset confirm`; the menu has a button for it that asks twice. Bosses near you come at level 1 again; spawn and reward cooldowns stay. Everyone has `mobosses.reset`; `/mobosses reset <player>` wipes someone else’s at once and needs `mobosses.reset.others`, part of `mobosses.admin`.
- 🗿 **Obelisk:** high-level bosses raise an end crystal that heals them through a beam, a share of their health each second, 1.2% at most at level 100. Players break it in 2 to 6 hits, more at higher levels; nothing else can, and it never explodes.
- ⛓️ **Chain** (from level 18 as a Common, 8 as a Mythic): the boss chains the farthest archer in sight. For 5 to 10 seconds they can’t stray past 8 blocks without being yanked back and hurt; three blows to the boss in melee break it.
- 👥 **Doubles:** the boss vanishes in smoke and comes back among up to 6 copies with its look, gear, name, and glow. A copy falls to one hit and drops only what its mob would, and 1 experience; the rest vanish after 20 seconds.
- 🔥 **A second phase** for bosses of level 40 and higher: at half health the boss can’t be hurt for 3 seconds, throws everyone back, casts an ability at once, burns with soul fire, and casts a third faster for the rest of the fight.
- New abilities have sections of their own in `abilities.yml` (`obelisk`, `chain`, `doubles`, `phase_two`), and their levels in `classes.yml`; a `classes.yml` without them keeps them off.
- 🌕 **Nights of the hunt:** under a full moon bosses come 1.5 times as often, and rarer classes more often still, Mythic twice as often; the players in the world hear it rise. Other phases change nothing (`full_moon`).
- 🛡️ **Bosses grow against groups:** each player past the first who has dealt 5% of a boss’s health adds 35% to it, up to six players, and the fighters see it over the hotbar. Someone who only stood by changes nothing (`group_health`).
- 🍀 **Luck in the loot:** the survival bonus tips drops toward rare and better items, and a player whose drops from Rare or better bosses brought nothing rare for 8 to 12 kills, a count rolled anew each time, gets a rare item at least, never above the boss’s class (`loot.pity`, `loot.survival_luck`).
- 💀 **Dying to a boss costs something:** the progress toward the next level burns; the level stays (`progression.death_penalty`).
- 🏭 **Spawner farms pay less over time:** a player who keeps killing spawner bosses in one spot gets less experience and loot after the third, down to a fifth, and it comes back with time; natural bosses are never affected (`spawner_fatigue`).
- ⏳ **A survival bonus, as in Sacred:** each minute a player spends playing without dying raises the chance of bosses near them: +25% after an hour, +50% after three, +75% after nine, toward double. Death takes it all, with a note in the chat; minutes idle for 3 or more don’t count, so standing AFK earns nothing. The card in `/mobosses info` and the menu shows it (`survival_bonus`).
- 🐺 **Bosses fight back against pets and golems.** A wolf’s or a golem’s hit sets off abilities, as a player’s does, and area abilities (lightning, shockwave, meteors, frost nova, whirlwind) and ability shots hit the pets and golems that hit the boss in the last 15 seconds. A pet’s damage counts toward its owner’s share, and so toward kills and loot, in full; in experience it’s worth almost all of theirs at low levels and 60% at level 100 (`progression.pet_damage`).
- 🕯️ **Forgotten bosses despawn:** one nobody comes within 64 blocks of for 10 minutes leaves without loot, so bosses no longer pile up in caves for good (`despawn`). Bosses from `/mobosses spawn` stay.
- 💥 **A creeper boss’s blast grows with its level**, from the game’s 3 to 8 at level 100, past a charged creeper’s 6; a charged boss goes from 6 to 11 (`formula.explosion`). A creeper that blows itself up still gives nobody anything.
- 💬 **Players learn why they got less:** after a kill, each one reads in the chat when a boss below their level gave part of its experience, or one above it more; when the kill didn’t count, or they dealt too little for loot, with the shares needed; when potion rewards are on cooldown; and what their pets’ damage was worth (`broadcast.kill_notes`).
- 🏹 **Bosses’ own arrows, fireballs, and tridents grow with their level**, by 60% of their melee growth (`formula.ranged_damage`).

### Changed

- 🤝 **Groups get their loot:** in a group of N players, no share threshold, for loot, rewards, or the kill, asks more than 60% over N (`progression.group_share`). Four friends who split a boss evenly used to get no loot at all.
- ❤️ **Top bosses are tougher than the game’s own.** A zombie boss has more health than the Ender Dragon from level 30 as a Common and 20 as a Rare, and more than the Warden from level 40 as an Epic, 20 as a Mythic, and 100 as a Common; the first levels stay about as they were. Small mobs start from a zombie’s 20 health (`formula.health.base_points`).
- ⚔️ **Bosses hit harder as they grow:** at level 100 a Common zombie hits for 13 instead of 7, a Mythic for 31, close to the Warden; level 1 stays as it was, and level 10 hits about a third harder (`formula.damage`).
- ✨ **Vanilla experience grows with the fight:** a level 1 Common zombie gives 8, little more than a zombie; from level 30, when it outlasts the Ender Dragon, a Common gives more than the dragon’s 500; a level 100 Mythic gives 20,000, more than the dragon’s first kill. Mobs that drop little start from 5 (`formula.experience`).
- 📏 **Relevance compares levels by ratio** (`progression.relevance`): a boss a few levels below you costs little experience, one half your level most of it, and a level 80 player still counts a level 60 boss as a kill, but not a level 40 one. A boss cut by variance always counts now.
- 🏅 **The board ranks by level**, from experience and kills together, as levels grow, instead of by experience alone.
- A kill that counts gives at least 1 Mo’Bosses experience; the first bosses used to show 0.
- Players who own an old `config.yml` keep their old `formula.health` and `formula.damage`; copy the new values from the bundled file to get the tougher bosses.

## 2.2.0

### New

- 🎯 **A signature ability for every boss type**, 18 in all, on top of the shared ones: witches throw potions, blazes fire fireball barrages, skeletons, strays, bogged, and pillagers loose aimed volleys, spiders pounce, endermen swap places with you, drowned hurl tridents, evokers send fangs along the ground, vindicators and brutes whirl, wither skeletons breathe a withering cloud, husks raise sandstorms, zombies spread infection, breezes fire wind charges, piglins rally their kind, phantoms bring darkness, vexes fade from sight, ravagers and hoglins stampede, guardians curse you with mining fatigue, and creepers light their fuse early. Each class gets them from its `signature` level in `classes.yml`; `signature` in `abilities.yml` switches each one, sets its cooldown and strength, and picks its boss types.
- 📖 **`/mobosses` opens a menu** for every player: their level, rank, experience, kills, and how long until a boss may spawn near them; the best hunters; and the bosses by class. Admins also get reload and remove-all buttons.
- ℹ️ **`/mobosses info [player]`** shows the same card in the chat, and **`/mobosses top`** the best hunters. Both need `mobosses.use`, which everyone has; another player's card needs `mobosses.info.others`.
- 📊 **More in Plan:** the server page counts the bosses spawned and killed, and has tables of the best hunters and of bosses by class and by type.
- 📢 **A rare boss doesn’t sneak up on you:** players near a new Epic boss or rarer hear about it, with a warning sound (`broadcast.spawns_from`, `spawns_radius`).
- 🛡️ **New gear for bosses:** copper armor between leather and iron for zombies, husks, drowned, skeletons, strays, bogged, wither skeletons, and piglins; copper swords and axes; spears of every material, golden ones for piglins; and maces for high-level zombies, vindicators, and brutes. Archers keep their bows.
- 🎁 **37 new loot entries** from recent game versions: wind charges, breeze rods, trial keys and ominous ones, ominous bottles, armor trim and netherite templates, echo shards, wolf armor, copper armor, spears from copper to netherite, elytra, and, at the top, two named maces and a heavy core.

### Changed

- Ability warnings show over the hotbar instead of in the chat, and everyone fighting the boss sees them, not only the player whose hit set one off (`broadcast.abilities_in: chat` brings the chat back).
- Statistics of bosses go to the database: a new `boss_stats` table, and a `name` column on players for the boards. Existing databases get both on the next start.

### Fixed

- Piglin, brute, and hoglin bosses in the overworld turned into zombified ones after a few seconds and stopped being bosses.
- The kill announcement left out players who had logged off before the boss fell.
- The console printed `<papi:…>` tags as text instead of leaving them empty.

## 2.1.0

### New

- 🏷️ **Names in each player’s language.** The names over bosses and minions now come in each viewer’s language, through text displays only they see. `name_tags.per_player: false` brings back the game’s own name tag in the default language.
- 🐲 **Eight new bosses:** Bogged, Blaze, Breeze, Cave Spider, Piglin Brute, Hoglin, Ravager, and Guardian, each with minions. A `bosses.yml` from 1.x or 2.0 doesn’t have them; copy them from the bundled file to get them.
- ✨ **Four new abilities.** A **shockwave** that knocks players back; a **frost nova** that freezes them as powder snow does; a **grapple** that pulls the farthest player in sight, against shooting a boss from a pillar; and **enrage**: once below 30% of its health, a boss hits harder and moves faster. A `classes.yml` without their levels keeps them off.
- 🧟 **Minions have better names** in every language, and they despawn as vanilla mobs do instead of filling the world.
- 📊 **Plan:** a player’s page shows their Mo’Bosses level, experience, and kills, the players table gets a level column, and the server page counts the bosses alive.
- 🧩 **MiniPlaceholders:** its tags work in every message, for the player who reads it, and `<mobosses_level>`, `<mobosses_xp>`, `<mobosses_kills>` work in TAB and elsewhere.
- 🛡️ **No bosses in WorldGuard plots.** No boss spawns in a region with owners or members unless `mobosses-spawn` allows it there; regions without owners, like spawn, keep their bosses (`no_bosses_in_owned_regions`).
- 💪 **Never weaker than vanilla.** A boss’s health, damage, armor, and toughness are at least its mob’s own, whatever the classes and curves say. Its gear only gets better: a piece goes on where the slot is empty or the piece is stronger, slots its set leaves out keep what the mob had, a skeleton’s pumpkin against the sun included, and an archer keeps its bow.
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

### Abilities

- **Cooldowns are rolled once**, when an ability goes off. They used to be rolled again on every hit, so a boss under many hits fired nearly at the base cooldown.
- **When the picked ability finds nothing to do**, like a fire ring over water, the boss tries another instead of wasting the hit.
- The **fire ring** goes out after `fire_ring_seconds` and never starts next to anything that burns, so it scorches the ground but doesn’t set a house on fire, and with `doFireTick` off it no longer stays for good.
- The **web ring** slows players only when it placed webs, and its cobwebs drop nothing when broken.
- The **teleport strike** lands only on solid ground the boss fits on, out of water and lava, in sight of its target, and where the protection plugins would let it spawn: no more bosses falling into ravines or stuck in walls.
- The **grapple** pulls a player off a pillar down, not up, with a moment of slow falling, instead of throwing them into the air.
- **Arrow arcs and arrow rain** hit players only: pets, villagers, horses, and the boss’s own minions stay out of it.
- **Arrow rain and meteors** start under the ceiling in caves instead of in the rock above.
- **Regeneration** waits until the boss has lost a fifth of its health.
- The **shockwave** roars first and slams a moment later, so a player who watches the boss can step back.

### Fixed

- Minions that rolled into bosses didn’t count toward a boss’s minion limit.
- A boss that healed could give out shares adding up to more than the whole boss, and so more experience and loot.
- A death another plugin cancelled still paid out the rewards, and paid again when the boss died for real.
- A zero for `exp_scale`, `kill_scale`, or `offset` in the config broke the level math.
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
