# Mo’Bosses

<p align="center">
  <a href="https://modrinth.com/plugin/mobosses"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/mobosses?logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href="https://hangar.papermc.io/MairwunNx/MoBosses"><img alt="Hangar" src="https://img.shields.io/hangar/dt/MoBosses?label=Hangar&color=1E88E5"></a>
  <a href="https://github.com/MairwunNx/mobosses/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/MairwunNx/mobosses?label=release"></a>
  <img alt="Paper 26.3" src="https://img.shields.io/badge/Paper-26.3-F7CF0D">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <a href="https://github.com/MairwunNx/mobosses/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/MairwunNx/mobosses/build.yml?branch=master&label=build"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/MairwunNx/mobosses"></a>
</p>

🏆 **Mo’Bosses** turns ordinary mobs on Paper servers into bosses. A zombie that spawns near you may come out as a level 40 Epic boss with more health, harder hits, a glow in its class color, a health bar, and abilities like lightning, meteor showers, and minions. Every kill raises your own Mo’Bosses level, and the bosses that find you grow with it.

[Русская версия](https://github.com/MairwunNx/mobosses/blob/master/README.RU.md)

Mo’Bosses runs on Moonlands, my hard survival server with friends. Come in and meet its bosses.

[![Play on Moonlands: hard mobs, bosses, AI, claims. minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/mobosses/master/media/og-adv-image.png)](https://minecraft.mairwunnx.com)

## What you get

- 🐲 **25 kinds of bosses in six classes**, from Common to Mythic, from zombies and skeletons to blazes, breezes, ravagers, and guardians.
- 📈 **Levels that grow with your players.** A boss takes the level of the players near it, and weak bosses count for little, so farming them levels nobody up.
- ✨ **Thirty-four abilities:** sixteen shared ones, from lightning and meteor showers to a healing obelisk and doubles, eighteen that belong to particular boss types, and a second phase at half health.
- 🎁 **Loot worth the fight:** over 600 entries by level and class, a private drop for each player who fought, potion rewards, and a pity count for rare items.
- 🏆 **Something to chase:** achievements in a tab of their own, a bestiary, the best hunters, nights of the hunt under a full moon, and a survival bonus that brings more bosses the longer you live.
- 🌍 **Every player reads their own language,** even the names over bosses’ heads: five languages bundled, picked by IP or game language.
- 🛡️ **Builds stay safe:** GriefPrevention claims and WorldGuard regions keep bosses out, and CoreProtect logs whatever they break, ready to roll back.
- 🧩 **Plays well with others:** PlaceholderAPI, MiniPlaceholders, Plan, WorldAPI, and SQLite, MySQL, MariaDB, or PostgreSQL for players’ levels.

## Get started

1. Put the jar from [Modrinth](https://modrinth.com/plugin/mobosses), [Hangar](https://hangar.papermc.io/MairwunNx/MoBosses), or [GitHub](https://github.com/MairwunNx/mobosses/releases/latest/download/mobosses.jar) in `plugins/` on Paper 26.3 with Java 25, or add `mobosses` to `MODRINTH_PROJECTS` in [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io).
2. Restart the server, then meet a boss right away: `/mobosses spawn zombie 30 epic`.
3. Read [First steps](https://github.com/MairwunNx/mobosses/wiki/First-steps) to tune how often bosses come.

## Documentation

The [wiki](https://github.com/MairwunNx/mobosses/wiki) has everything else.

**For players and server owners:** [Installation](https://github.com/MairwunNx/mobosses/wiki/Installation) · [First steps](https://github.com/MairwunNx/mobosses/wiki/First-steps) · [Bosses and classes](https://github.com/MairwunNx/mobosses/wiki/Bosses-and-classes) · [Abilities](https://github.com/MairwunNx/mobosses/wiki/Abilities) · [Minions](https://github.com/MairwunNx/mobosses/wiki/Minions) · [Loot and rewards](https://github.com/MairwunNx/mobosses/wiki/Loot-and-rewards) · [Player levels](https://github.com/MairwunNx/mobosses/wiki/Player-levels) · [Claims and protection](https://github.com/MairwunNx/mobosses/wiki/Claims-and-protection) · [Commands and permissions](https://github.com/MairwunNx/mobosses/wiki/Commands-and-permissions) · [Configuration](https://github.com/MairwunNx/mobosses/wiki/Configuration) · [Languages](https://github.com/MairwunNx/mobosses/wiki/Languages) · [Integrations](https://github.com/MairwunNx/mobosses/wiki/Integrations) · [Upgrading from 1.x](https://github.com/MairwunNx/mobosses/wiki/Upgrading-from-1.x) · [FAQ](https://github.com/MairwunNx/mobosses/wiki/FAQ)

**For developers:** [Building from source](https://github.com/MairwunNx/mobosses/wiki/Building-from-source) · [Architecture](https://github.com/MairwunNx/mobosses/wiki/Architecture) · [Development](https://github.com/MairwunNx/mobosses/wiki/Development) · [Releasing](https://github.com/MairwunNx/mobosses/wiki/Releasing)

## Related projects

- **[Moonlands](https://minecraft.mairwunnx.com)** – the hard-survival server where all of these run.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)** – REST and WebSocket API to the world, players, and stats of a Paper, Fabric, or NeoForge server.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)** – lets an AI client read and run a server through WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)** – an AI helper in the game: ask with `/ask`, right in the chat.
- **[Allay Server](https://github.com/allay-ai/server)** – the backend behind Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)** – join and quit messages in each player's language.
- **[Mo’Afks](https://github.com/MairwunNx/moafks)** – a safe AFK mode that doesn't block the night.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)** – Prometheus metrics from WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)** – the Grafana dashboard for WorldAPI.

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.

> 🫡 Made by Pavel Erokhin (Павел Ерохин), aka mairwunnx.
