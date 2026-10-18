# 08. Администрирование: права, аккаунты, баны, моды

## Уровни доступа (`game\config\AccessLevels.xml`)

| Level | Имя | isGM | Особенности |
|---|---|---|---|
| -1 | Banned | нет | Забанен: нет транзакций, урона, опыта |
| 0 | User | нет | Обычный игрок |
| 10 | Chat Moderator | нет | Модерация чата |
| 20 | Test GM | нет | Фикс-респ, alt+g |
| 30 | General GM | нет | Доступ к админ-панелям (`admin` меню) |
| 40 | Support GM | нет | |
| 50 | Event GM | нет | |
| 60 | Head GM | нет | Транзакции, полный урон |
| 70 | Admin | **да** | Все права (isGM=true) |
| 100 | Master | **да** | Максимум (silence, diet, set_mod, смена конфигов) |

Уровни наследуются через `childAccess` (у 100 — childAccess 70, и т.д.). Уровень аккаунта задаётся в БД: `UPDATE accounts SET accessLevel = 100 WHERE login='...'` (или `AccountManager` из `login\`). Доступность конкретных команд — в `game\config\AdminCommands.xml` (469 команд; каждая помечена `accessLevel="..."`).

## Админ-команды (`game\config\AdminCommands.xml`)

- Формат: `<admin command="admin_name" description="..." accessLevel="N" confirmDlg="true"?/>`.
- Вызов в игре: `//имя_команды` (префикс `admin_` опускается). Главные меню-панели: `//admin` … `//admin7` (панели управления: server management, mods, character, GM menu).
- Примеры: `announce`, `announce_crit`, `announce_screen` — объявления; `sethero`, `givehero` — герои; `saveolymp`, `endolympiad`, `checkolympiad` — олимпиада; `set_mod` / `setconfig` — живое изменение конфигов из игры; `gmliston/gmlistoff`, `silence`, `diet`, `tradeoff`.
- Игроковые админ-команды (правка 14.09.2026): гейт `AdminCommandHandler.onCommand` пропускает
  не-ГМа, если команда объявлена с `accessLevel="0"` (проверка `AdminData.hasAccess` по цепочке
  childAccess). Такая команда одна — `oly` (`//oly`): дистанционная регистрация на бесклассовую
  олимпиаду. → [13-oly-remote-registration.md](13-oly-remote-registration.md)
- Панельные HTML лежат в `data\html\admin\` (233 файла).
- Особенность олимпиады: invul администратора — участника матча (например, включённый `//invul` / `//setinvul`)
  принудительно снимается ядром за 1 секунду до начала боя (`OlympiadGameTask`, отметка «1»; наблюдатели
  арены не затрагиваются). → [09-fake-players-ai.md](09-fake-players-ai.md), журнал правок за 09.09.2026.
- Логирование действий ГМ: `log\gmaudit0.log`, `log\audit0.log`.

## GM: вход и выход из просмотра Олимпиады

По [52](52-gm-login-and-olympiad-observer-cleanup.md) `General.ini` содержит `GMStartupBuilderHide=False`, `GMStartupInvulnerable=False`, `GMStartupInvisible=False`. Отключение только Invulnerable недостаточно: native `setHiding(true)` через BuilderHide выставляет оба флага и silence. Ручные GM-команды сохраняются.

Новый core-патч `game/core-patches/observer-cleanup/InstallObserverCleanup.java` снимает GM-исключение в `Player.leaveOlympiadObserverMode`: при выходе принудительно `setInvisible(false)` и `setInvul(false)` для всех зрителей. Во время просмотра защита остаётся. Не снимает настоящие skill-effects/штатную краткую teleport-защиту и не меняет обычный observer mode. Это отдельное правило от снятия invul участника перед боем.

Требуются весь согласованный core-patches-комплект, обновлённые CMD/Task.sh и полный рестарт; это четвёртый hash-verified патч после debuff/boundary/diversity. Бинарный JAR в Git не заменён; установка меняет локальный JAR атомарно с backup. Назначение файлов, тесты, smoke и откат —52.

## Аккаунты

- Автосоздание при входе (`login\config\Server.ini → AutoCreateAccounts = True`).
- Ручное создание/смена пароля/удаление: `login\AccountManager.vbs` (интерактивная консоль).
- Таблица `accounts` (→ [07-database.md](07-database.md)): `accessLevel`, `lastIP`, `lastServer`.
- IP-ограничения на аккаунт: таблица `accounts_ipauth`.

## Баны и наказания

| Инструмент | Где |
|---|---|
| Бан аккаунта | `accessLevel = -1` (или другая отрицательная ветка) в `accounts` |
| Бан IP (статический) | `login\banned_ip.cfg` (`IP [timestamp_истечения]`) |
| Бан IP (динамический) | ГМ-команды (таблица `punishments` в БД) |
| Мут чата | ГМ-команды/ChatModeration (мод) |
| Автобан брутфорса | `Server.ini`: 5 неверных паролей → бан IP на 15 минут |
| Жалобы на ботов | `BotReportPunishments.xml` + таблица `bot_reported_char_data` |

## Кастомные моды (`game\config\Custom\` + `data\scripts\custom\`)

Моды Mobius, каждый — отдельный `.ini` в `config\Custom\` + иногда скрипт в `data\scripts\custom\` + HTML в `data\html\mods\`/CommunityBoard. Краткий каталог (детали — [03-game-config.md](03-game-config.md)):

| Мод | Назначение |
|---|---|
| `AutoPotions.ini` | Автопотки (HP/MP/CP при падении %) |
| `SchemeBuffer.ini` + `SchemeBufferSkills.xml` | Scheme-баффер (свои наборы баффов) |
| `SellBuffs.ini` + `SellBuffData.xml` | Продажа баффов игроками |
| `OfflineTrade.ini` / `OfflinePlay.ini` | Офлайн-торговля/игра (таблицы `character_offline_*`) |
| `FakePlayers.ini` + `FakePlayerChatData.xml` + Routes.xml | Фейковые игроки (npcId 80000+) |
| `FakePlayersAI.ini` + скрипты `custom\fakeplayersai\` | **Боты FakePlayersAI** — боевые боты (настоящие безклиентские игроки): PvP, олимпиада, управление `//bot`. → [09-fake-players-ai.md](09-fake-players-ai.md) |
| `AutoPlay.ini` | Автофарм `.play`; **EnableAutoPlay=True обязателен** — на нём работают боты FakePlayersAI |
| `Wedding.ini` + `mods_wedding` | Свадьбы |
| `Rebirth.ini` + `rebirth_system` | Система реберса |
| `PremiumSystem.ini` + `account_premium` | Премиум-аккаунты |
| `Transmog.ini` + `character_transmogs` | Трансмогрификация внешности |
| `FactionSystem.ini` | Фракции (Faction vs Faction) |
| `Captcha.ini`, `WalkerBotProtection.ini`, `DualboxCheck.ini` | Антибот/антидуалбокс |
| `Banking.ini` | «Банк» (обмен адена↔голд-бар) |
| `DelevelManager.ini`, `NoblessMaster.ini`, `ClassMaster.xml` | Упрощение прогресса (дебафф уровня, ноблес, профы) |
| `ChampionMonsters.ini`, `NpcStatMultipliers.ini`, `RandomSpawns.ini` | Модификация мобов |
| `PvpAnnounce.ini`, `PvpRewardItem.ini`, `PvpTitleColor.ini`, `FindPvP.ini` | PvP-украшения/награды |
| `ChatModeration.ini` | Модерация чата |
| `StartingLocation.ini`, `StartingTitle.ini`, `ScreenWelcomeMessage.ini`, `OnlineInfo.ini`, `ServerTime.ini` | Стартовый UX |
| `WarehouseSorting.ini`, `PrivateStoreRange.ini`, `MerchantZeroSellPrice.ini`, `FreeMounts.ini`, `CustomMailManager.ini`, `HellboundStatus.ini`, `CancelReturn.ini`, `PasswordChange.ini`, `MultilingualSupport.ini`, `AllowedPlayerRaces.ini`, `AutoPlay.ini`, `ClassBalance.ini`, `BossAnnouncements.ini`, `CommunityBoard.ini` | Прочие фичи |

## Типовые задачи

| Задача | Как |
|---|---|
| Сделать ГМом | `UPDATE accounts SET accessLevel=100 WHERE login='...'`; перезайти |
| Забанить аккаунт | `UPDATE accounts SET accessLevel=-1 WHERE login='...'` |
| Выдать премиум | В игре у NPC Premium или `account_premium` (enddate в мс) |
| Изменить конфиг на ходу | ГМ-команда `//set_mod` (если включено) или правка `.ini` + рестарт |
| Посмотреть кто онлайн | Панель `//admin` → или мод `OnlineInfo` |
| Сменить пароль аккаунта | `login\AccountManager.vbs` или UPDATE хэша |
| Спавнить/настраивать боевых ботов | `//bot` (меню), подробнее → [09-fake-players-ai.md](09-fake-players-ai.md) |
| Перечитать скрипты без рестарта | Если доступно — команда reload в `//admin`; иначе рестарт GS |
