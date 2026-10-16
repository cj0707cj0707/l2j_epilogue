# Документация по структуре сервера Lineage 2 Gracia Epilogue (L2J-Mobius)

> Назначение этой папки — справочник по структуре и устройству сервера в `C:\EpilogueServer`.
> При любых последующих манипуляциях с сервером (правка конфигов, датапака, скриптов, БД) сначала сверяйся с этими документами.

## Что это за сервер

- **Проект:** L2J-Mobius, хроника **The 2nd Throne: Gracia Epilogue** (CT 2.6).
- **Тип:** развёрнутая (скомпилированная) сборка — ядро в JAR, датапак и скрипты в исходниках (`game/data/scripts/**/*.java`, компилируются сервером при старте).
- **Java:** OpenJDK 25 (аргументы запуска — в `game/java.cfg`, ZGC, heap 2–4 ГБ).
- **БД:** MySQL/MariaDB, схема **`l2jmobiusepilogue`** (подключение — `game/config/Database.ini`, `login/config/Database.ini`, пользователь `root`, без пароля).
- **Язык скриптов:** Java (квесты, ИИ, инстансы, события, хендлеры) + HTML-диалоги + XML-данные.

## Архитектура в двух словах

```
Клиент L2 (Gracia Epilogue)
      │  порт 2106 (протокол логина)        порт 7777 (игровой)
      ▼                                        ▼
LoginServer (login/)  ──регистрация──▶  GameServer (game/)
      │                                        │
      └──────────── MySQL: l2jmobiusepilogue ──┘
```

- **LoginServer** — авторизация аккаунтов, выдача hexid, список серверов. Таблицы БД: `accounts`, `gameservers` и др.
- **GameServer** — вся игровая логика: мир, NPC, квесты, инстансы, осады, олимпиада и т.д. Таблицы БД: 116+ (`characters`, `items`, `clan_data`, ...).
- Связка Login↔Game регистрируется один раз (файл `game/config/hexid.txt`); перерегистрация — `login/GameServerRegister`.

## Последнее обновление

[23. Ограниченное движение под страхом](23-bounded-fear-movement.md) — случайные короткие шаги вокруг места наложения; общий Fear handler, радиус100, установка и откат.


[22. Навыки классов, усиления до старта, ворота и результаты тренировок](22-class-skills-openers-and-training-results.md) — карта файлов, установка, проверки и откат. Подготовка10сек, entry-only сброс откатов, единая заточка ботов.

## Состав документации

| Файл | Содержание |
|---|---|
| [01-root-structure.md](01-root-structure.md) | Корневые папки сервера: `game`, `login`, `libs`, `db_installer`, `backup`, `images` |
| [02-launching.md](02-launching.md) | Запуск/остановка серверов, `java.cfg`, логи, консольные скрипты |
| [03-game-config.md](03-game-config.md) | `game/config` — все конфиги GameServer (+ `Custom/` — кастомные фичи) |
| [04-login-server.md](04-login-server.md) | `login/` — устройство LoginServer, его конфиги и данные |
| [05-datapack-data.md](05-datapack-data.md) | `game/data` — датапак: XML-данные, geodata, зоны, спавны, мультиселлы, статы |
| [06-scripts-and-quests.md](06-scripts-and-quests.md) | `game/data/scripts` — Java-скрипты: квесты, ИИ, инстансы, события, хендлеры |
| [07-database.md](07-database.md) | `db_installer` — схема БД, установщик, бэкапы-дампы |
| [08-admin-and-mods.md](08-admin-and-mods.md) | Админ-команды, кастомные моды, типовые задачи администратора |
| [09-fake-players-ai.md](09-fake-players-ai.md) | Система боевых ботов FakePlayersAI: устройство, олимпиада, конфиг, `//bot`, журнал правок |
| [10-stun-visual-and-olympiad-ui.md](10-stun-visual-and-olympiad-ui.md) | Визуал стана (звёзды над головой) и панель противника на олимпиаде (HP/CP + бафы/дебафы), журнал правок ядра и скриптов |
| [11-line-of-sight-fix.md](11-line-of-sight-fix.md) | Фикс 11.09.2026: атака/касты/чарджи сквозь стены, двери и заборы (`GeoEngine.canSeeTarget`/`canMoveToTarget`), журнал правок ядра |
| [12-blink-fix.md](12-blink-fix.md) | Фикс 13.09.2026: Blink (скилл 1448 Soul Hound) — self-эффекты применяются даже при пустом списке целей, журнал правок ядра |
| [13-oly-remote-registration.md](13-oly-remote-registration.md) | Команда 14.09.2026: `/oly`, `//oly`, `.oly` — дистанционная регистрация на бесклассовую олимпиаду без менеджера; патч гейта AdminCommandHandler (команды с accessLevel=0 доступны игрокам) |
| [14-bluff-turn.md](14-bluff-turn.md) | Фикс 18-19.09.2026: Bluff / Blinding Blow разворачивают противника спиной (ValidateLocation окружающим + «нулевой шаг» MoveToLocation/StopMove собственному клиенту жертвы — без телепорта и затемнения экрана) |
| [15-dominator-enlightenment.md](15-dominator-enlightenment.md) | Фикс 03.10.2026: Enlightenment у Dominator — «волшебный» вариант 1532 (+40% M.Atk) вместо «хилерского» 1533 (+10% M.Atk/+40% Heal), как у всех магических классов; то же в бот-пресете |
| [16-giran-adena-giver.md](16-giran-adena-giver.md) | NPC Adena Giver в Гиране: 100 000 000 адены за нажатие, файлы, проверки и установка |
| [17-olympiad-predictive-chase.md](17-olympiad-predictive-chase.md) | Олимпийские боты: чтение направления движения противника, прогноз перехвата и сокращение дистанции |
| [18-bot-combat-fixes-and-rollback.md](18-bot-combat-fixes-and-rollback.md) | Исправление ложных кастов ботов, улучшения боя и резервная точка/порядок отката |
| [19-pursuit-movement-ownership.md](19-pursuit-movement-ownership.md) | Перехват v2: конфликт CAST-подхода/автокаста, приоритет движения, диагностика, установка и отдельный откат |
| [20-olympiad-suite-and-giran-services.md](20-olympiad-suite-and-giran-services.md) | Тактика/сложности, изолированные тренировки, аналитика, четыре NPC Гирана, инструкция, восстановление и безопасный откат |
| [21-strong-tempo-and-training-fixes.md](21-strong-tempo-and-training-fixes.md) | Strong/100ms, атака вместо частого кайта, исправление NPC-кнопок, олимпийский бафф и30сек. на селфбафф |

## Куда смотреть при типовых задачах

| Задача | Документ |
|---|---|
| Поменять рейты (XP/SP/дроп/адена) | `03-game-config.md` → `Rates.ini` |
| Изменить порт/IP/сетевые настройки | `03-game-config.md` → `Network.ini`, `Server.ini`; `04-login-server.md` |
| Включить/настроить офлайн-трейд, баффера и т.п. | `03-game-config.md` → `Custom/*.ini` |
| Добавить/поправить квест или ИИ NPC | `06-scripts-and-quests.md` |
| Поправить диалог NPC | `05-datapack-data.md` → `html/` |
| Изменить спавны/добавить мобов | `05-datapack-data.md` → `spawns/` |
| NPC для выдачи 100 миллионов адены в Гиране | `16-giran-adena-giver.md` |
| Добавить предмет в магазин | `05-datapack-data.md` → `buylists/`, `multisell/` |
| Проблемы с БД, восстановление | `07-database.md` |
| Аккаунты, баны, права | `08-admin-and-mods.md` |
| Боты FakePlayersAI (спавн, ротации, олимпиада) | `09-fake-players-ai.md`; новая тактика/тренировки — `20-olympiad-suite-and-giran-services.md` |
| Перехват убегающего противника ботами на олимпиаде | `17-olympiad-predictive-chase.md`; актуальный фикс/диагностика — `19-pursuit-movement-ownership.md` |
| Визуальные эффекты (стан/звёзды), панель противника олимпиады | `10-stun-visual-and-olympiad-ui.md` |
| Неработающие скиллы (Blink и т.п.) — что смотреть в цепочке каста | `12-blink-fix.md` |
| Bluff / Blinding Blow не разворачивают противника | `14-bluff-turn.md` |
| Дистанционная регистрация на олимпиаду (`/oly`, `//oly`, `.oly`) | `13-oly-remote-registration.md` |

## Важные файлы-«точки входа»

| Файл | Роль |
|---|---|
| `game/GameServer.vbs` / `.sh` | Запуск GameServer (Windows/Linux) |
| `login/LoginServer.vbs` / `.sh` | Запуск LoginServer |
| `game/java.cfg`, `login/java.cfg` | Аргументы JVM (память, GC, кодировка) |
| `game/config/Database.ini` | Подключение GameServer к MySQL |
| `login/config/Database.ini` | Подключение LoginServer к MySQL |
| `game/config/hexid.txt` | Идентификатор зарегистрированного GameServer |
| `game/config/Server.ini` | Имя сервера, порт 7777, макс. игроков |
| `game/config/Scripts.xml` | Управление загрузкой скриптов (exclude/include) |
