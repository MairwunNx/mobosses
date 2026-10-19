# Mo’Bosses

🏆 **Mo’Bosses** turns ordinary mobs into bosses. A zombie that spawns near a player may come out as a level 40 Epic boss with more health, harder hits, a glow in its class color, a health bar, and abilities like lightning, meteor showers, and minions. Every kill raises the player’s own Mo’Bosses level, and the bosses that find them grow with it.

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

1. Put the jar in `plugins/` on Paper 26.3 or a fork of it, on Java 25. With [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io), add `mobosses` to `MODRINTH_PROJECTS` instead.
2. Restart the server, then meet a boss right away: `/mobosses spawn zombie 30 epic`.
3. Read [First steps](https://github.com/MairwunNx/mobosses/wiki/First-steps) to tune how often bosses come.

Configs and databases from 1.x load as they are.

## Documentation

The [wiki](https://github.com/MairwunNx/mobosses/wiki) explains everything else: [bosses and classes](https://github.com/MairwunNx/mobosses/wiki/Bosses-and-classes), [abilities](https://github.com/MairwunNx/mobosses/wiki/Abilities), [loot and rewards](https://github.com/MairwunNx/mobosses/wiki/Loot-and-rewards), [player levels](https://github.com/MairwunNx/mobosses/wiki/Player-levels), [claims and protection](https://github.com/MairwunNx/mobosses/wiki/Claims-and-protection), [commands and permissions](https://github.com/MairwunNx/mobosses/wiki/Commands-and-permissions), [configuration](https://github.com/MairwunNx/mobosses/wiki/Configuration), [languages](https://github.com/MairwunNx/mobosses/wiki/Languages), and [integrations](https://github.com/MairwunNx/mobosses/wiki/Integrations). Source code and issues live on [GitHub](https://github.com/MairwunNx/mobosses).

## Related projects

- **[Moonlands](https://minecraft.mairwunnx.com)**—the hard-survival server where all of these run.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)**—REST and WebSocket API to a Paper server's world, players, and stats.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)**—lets an AI client read and run a server through WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)**—an AI helper in the game: ask with `/ask`, right in the chat.
- **[Allay Server](https://github.com/allay-ai/server)**—the backend behind Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)**—join and quit messages in each player's language.
- **[Mo’Afks](https://github.com/MairwunNx/moafks)**—a safe AFK mode that doesn't block the night.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)**—Prometheus metrics from WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)**—the Grafana dashboard for WorldAPI.

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.
