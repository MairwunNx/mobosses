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

🏆 **Mo’Bosses** превращает обычных мобов на серверах Paper в боссов. Зомби, который появился рядом с вами, может оказаться эпическим боссом 40-го уровня: больше здоровья, сильнее удары, свечение в цвет класса, полоса здоровья и способности вроде молний, метеоритного дождя и миньонов. Каждое убийство поднимает ваш собственный уровень Mo’Bosses, и боссы, которые вас находят, растут вместе с ним.

[English version](https://github.com/MairwunNx/mobosses/blob/master/README.md)

Mo’Bosses работает на Moonlands — моём сервере со сложным выживанием для друзей. Заходите и познакомьтесь с его боссами.

[![Играйте на Moonlands: сложные мобы, боссы, ИИ, приваты. ru.minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/mobosses/master/media/og-adv-image.ru.png)](https://ru.minecraft.mairwunnx.com)

## Что внутри

- 🐲 **25 видов боссов шести классов**, от обычного до мифического, от зомби и скелетов до ифритов, вихрей, разорителей и стражей.
- 📈 **Уровни растут вместе с игроками.** Босс берёт уровень игроков рядом, а слабые боссы почти ничего не дают, так что фарм слабых никого не прокачает.
- ✨ **Тридцать четыре способности:** шестнадцать общих, от молний и метеоритного дождя до лечащего обелиска и двойников, восемнадцать своих у разных видов боссов и вторая фаза на половине здоровья.
- 🎁 **Добыча, ради которой стоит драться:** больше 600 записей по уровню и классу, своя добыча каждому, кто бился, эффекты зелий в награду и счётчик удачи на редкие вещи.
- 🏆 **Есть к чему стремиться:** достижения в своей вкладке, бестиарий, лучшие охотники, ночи охоты в полнолуние и бонус выживания: чем дольше вы живы, тем больше боссов вас находит.
- 🌍 **Каждый игрок читает на своём языке,** даже имена над головами боссов: пять языков в комплекте, выбор по IP или по языку игры.
- 🛡️ **Постройки в безопасности:** приваты GriefPrevention и регионы WorldGuard закрыты для боссов, а CoreProtect записывает всё, что они сломали, и откатывает.
- 🧩 **Дружит с другими плагинами:** PlaceholderAPI, MiniPlaceholders, Plan, WorldAPI и SQLite, MySQL, MariaDB или PostgreSQL для уровней игроков.

## Как начать

1. Положите jar с [Modrinth](https://modrinth.com/plugin/mobosses), [Hangar](https://hangar.papermc.io/MairwunNx/MoBosses) или [GitHub](https://github.com/MairwunNx/mobosses/releases/latest/download/mobosses.jar) в `plugins/` на Paper 26.3 с Java 25 или добавьте `mobosses` в `MODRINTH_PROJECTS` в [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io).
2. Перезапустите сервер и сразу встретьте босса: `/mobosses spawn zombie 30 epic`.
3. Прочитайте [First steps](https://github.com/MairwunNx/mobosses/wiki/First-steps), чтобы настроить, как часто приходят боссы.

## Документация

Всё остальное — в [вики](https://github.com/MairwunNx/mobosses/wiki), на английском.

**Игрокам и владельцам серверов:** [Installation](https://github.com/MairwunNx/mobosses/wiki/Installation) · [First steps](https://github.com/MairwunNx/mobosses/wiki/First-steps) · [Bosses and classes](https://github.com/MairwunNx/mobosses/wiki/Bosses-and-classes) · [Abilities](https://github.com/MairwunNx/mobosses/wiki/Abilities) · [Minions](https://github.com/MairwunNx/mobosses/wiki/Minions) · [Loot and rewards](https://github.com/MairwunNx/mobosses/wiki/Loot-and-rewards) · [Player levels](https://github.com/MairwunNx/mobosses/wiki/Player-levels) · [Claims and protection](https://github.com/MairwunNx/mobosses/wiki/Claims-and-protection) · [Commands and permissions](https://github.com/MairwunNx/mobosses/wiki/Commands-and-permissions) · [Configuration](https://github.com/MairwunNx/mobosses/wiki/Configuration) · [Languages](https://github.com/MairwunNx/mobosses/wiki/Languages) · [Integrations](https://github.com/MairwunNx/mobosses/wiki/Integrations) · [Upgrading from 1.x](https://github.com/MairwunNx/mobosses/wiki/Upgrading-from-1.x) · [FAQ](https://github.com/MairwunNx/mobosses/wiki/FAQ)

**Разработчикам:** [Building from source](https://github.com/MairwunNx/mobosses/wiki/Building-from-source) · [Architecture](https://github.com/MairwunNx/mobosses/wiki/Architecture) · [Development](https://github.com/MairwunNx/mobosses/wiki/Development) · [Releasing](https://github.com/MairwunNx/mobosses/wiki/Releasing)

## Связанные проекты

- **[Moonlands](https://ru.minecraft.mairwunnx.com)** — сервер со сложным выживанием, где всё это работает.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)** — REST и WebSocket API к миру, игрокам и статистике сервера Paper.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)** — даёт ИИ-клиенту читать сервер и управлять им через WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)** — ИИ-помощник в игре: спрашивайте через `/ask` прямо в чате.
- **[Allay Server](https://github.com/allay-ai/server)** — сервер, на котором работает Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)** — сообщения о входе и выходе на языке каждого игрока.
- **[Mo’Afks](https://github.com/MairwunNx/moafks)** — безопасный режим AFK, который не мешает пропустить ночь.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)** — метрики Prometheus из WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)** — дашборд Grafana для WorldAPI.

[IP Geolocation by DB-IP](https://db-ip.com), лицензия CC BY 4.0.

> 🫡 Сделал Павел Ерохин (Pavel Erokhin), он же mairwunnx.
