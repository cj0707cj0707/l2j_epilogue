# 03. Конфигурация GameServer (`game\config\`)

> Актуальные исправления навыков/ворот/победителя и настройки тренировки (10сек, сброс откатов при входе, заточка как на Олимпиаде): [22](22-class-skills-openers-and-training-results.md). При противоречии со старыми записями использовать документ22.


Все настройки ядра GameServer. Формат — INI и XML; почти каждый параметр снабжён комментарием с дефолтом в самом файле. Значения ниже — **фактически установленные** в этой сборке. Сервер настроен практически retail-like (рейты 1x), нестандартные правки отмечены.

Быстрая навигация: [Сетевые](#serverini) · [БД](#databaseini) · [Рейты](#ratesini) · [Игроки](#playerini) · [Кастомные моды](#каталог-custom-gameconfigcustom---44-файла)

## Обзор файлов

| Файл | За что отвечает |
|---|---|
| `Server.ini` | Сеть GS: адрес логина, порт 7777, лимиты, рестарты |
| `Network.ini` | Пакетный слой, пулы буферов и потоков |
| `Database.ini` | Подключение к MySQL + автобэкапы |
| `Threads.ini` | Размеры пулов потоков |
| `Rates.ini` | Множители XP/SP/дропа/спойла/виталити |
| `Player.ini` | Всё о персонажах: лимиты, кланы, скиллы, заточка, аугментация |
| `PVP.ini` | PvP/PK правила, недропаемые предметы |
| `General.ini` | Общие фичи: чат, CommunityBoard, дроп, лотерея, манор |
| `Feature.ini` | Замки/холлы/крепости/Seven Signs/кланы |
| `Siege.ini`, `FortSiege.ini`, `ConquerableHallSiege.ini`, `SiegeSchedule.xml`, `TerritoryWar.ini` | Осады и ТВ |
| `Olympiad.ini` | Олимпиада |
| `GrandBoss.ini`, `GraciaSeeds.ini` | Респавны гранд-боссов, сиды Грации |
| `NPC.ini`, `GeoEngine.ini` | Мобы и гео-движок |
| `FloodProtector.ini` | Антифлуд |
| `IdManager.ini` | Object ID и чистка БД |
| `AccessLevels.xml`, `AdminCommands.xml` | Права ГМ и админ-команды → [08-admin-and-mods.md](08-admin-and-mods.md) |
| `ClassMaster.xml` | NPC смены класса (выключен) |
| `DynamicExpRates.xml` | Поуровневые рейты (все = 1) |
| `BotReportPunishments.xml` | Наказания по жалобам на ботов |
| `Development.ini` | Отладка (выключена) |
| `Scripts.xml` | Управление загрузкой скриптов |
| `Interface.ini` | GUI-консоль |
| `hexid.txt` | Ключ авторизации GS на LS (ServerID=2, секрет) |
| `ipconfig.xml` | Активная локальная конфигурация:127.0.0.1; без внешнего HTTP-автоопределения (см.71) |
| `default-ipconfig.xml` | Неактивный образец IP-конфигурации |
| `chatfilter.txt` | Слова чат-фильтра (фильтр выключен) |
| `Custom\` | 44 `.ini` кастомных модов — см. низ документа |

## Server.ini

| Параметр | Значение | Комментарий |
|---|---|---|
| LoginHost / LoginPort | 127.0.0.1 / 9014 | Адрес LoginServer |
| GameserverHostname / Port | 0.0.0.0 / **7777** | Клиентский порт |
| PacketEncryption | False | Blowfish-шифрование пакетов отключено |
| RequestServerID / AcceptAlternateID | 1 / True | ID Bartz; при занятости выдаст другой |
| DatapackRoot / ScriptRoot | `.` / `./data/scripts` | Корень датапака |
| MaximumOnlineUsers | 2000 | |
| AllowedProtocolRevisions | **152** | Протокол Gracia Epilogue |
| ServerListType / Age / Brackets | Free / 0 / False | Вид в списке серверов |
| DeadlockWatcher / CheckInterval / RestartOnDeadlock | True / 20 / False | Детектор дедлоков |
| EnableHardwareInfo / MaxPlayersPerHWID | False / 0 | HWID-контроль выключен |
| CnameTemplate / PetNameTemplate / ClanNameTemplate | `.*` | Без ограничений имён |
| CharMaxNumber | 7 | Персонажей на аккаунт |
| PrecautionaryRestartEnabled | False | Рестарт при 95% CPU (неактивен) |
| ServerRestartScheduleEnabled | False | Расписание 08:00 среда (неактивно) |

## Network.ini

| Параметр | Значение | Комментарий |
|---|---|---|
| ShutdownWaitTime | 5 | Сек на финализацию пакетов |
| DropPackets | True | Сброс «одноразовых» пакетов |
| DropPacketThreshold | **2500** | Нестандартно (default 250) — очередь в 10 раз больше |
| ThreadPoolSize | 0 | Авто: ядра × 4 |
| BufferPool.AutoExpandCapacity | True | |
| Пулы буферов | Empty 100×2 … Maximum 10×32768 | Ступени от 64 до 32768 байт |

## Database.ini

| Параметр | Значение |
|---|---|
| Driver | `com.mysql.cj.jdbc.Driver` |
| URL | `jdbc:mysql://localhost/l2jmobiusepilogue?...` (utf-8, autoReconnect) |
| Login / Password | `root` / *(пусто)* |
| MaximumDatabaseConnections | 100 |
| BackupDatabase | **False** (бэкап при рестарте выключен) |
| MySqlBinLocation | `C:/xampp/mysql/bin/` |
| BackupPath / BackupDays | `../backup/` / 30 |

## Threads.ini

| Параметр | Значение |
|---|---|
| ScheduledThreadPoolSize | -1 (авто: ядра × 4) |
| InstantThreadPoolSize | -1 (авто: ядра × 2) |
| ThreadsForLoading | False |

## Rates.ini

**Сервер полностью retail-like — все множители = 1.**

| Параметр | Значение |
|---|---|
| RateXp / RateSp | 1 / 1 |
| RatePartyXp / RatePartySp | 1 / 1 |
| RateDropManor / RateExtractable / RateKarmaLost | 1 / 1 / -1 |
| QuestItemDropAmountMultiplier / RateQuestRewardXP/SP/Adena | 1 / 1 / 1 / 1 |
| DeathDropAmountMultiplier / ChanceMultiplier (и Spoil/Herb/Raid) | 1 / 1 |
| DropAmountMultiplierByItemId | 57,1;6656,1;…;10314,1 — персональные множители адены и боссовой бижутерии |
| DropMaxOccurrencesNormal / Raidboss | 2 / 7 |
| DropAdena Min/MaxLevelDifference | 8 / 15 (Item: 5/10) |
| RateVitalityLevel1–4 | 1.5 / 2 / 2.5 / 3 |
| RateVitalityGain / Lost | 1 / 0.6 |
| PlayerDropLimit / PlayerRateDrop* | 0 — игроки ничего не дропают при смерти |
| KarmaDropLimit / RateDrop / Item / Equip / Weapon | 10 / 40 / 50 / 40 / 10 (%) |
| PetXpRate / PetFoodRate / SinEaterXpRate | 1 / 1 / 1 |
| BossDropEnable | False (кастом-дроп рейдов выключен) |

## Player.ini (ключевое)

| Параметр | Значение | Комментарий |
|---|---|---|
| MaximumPlayerLevel / MaxSubclass | 85 / 3 | Подкласс с 40 уровня |
| StartingAdena / StartingLevel | 0 / 1 | |
| MaxAdena | **3 000 000 000 000 000** | Нестандартно (default 99 млрд) |
| AutoLearnSkills / DivineInspiration | False / False | Скиллы учатся у NPC |
| MaxBuffAmount / MaxDanceAmount | 20 / 12 | |
| AltStoreDances | **True** | Танцы сохраняются при релоге (нестандартно) |
| EnableVitality / StartingVitalityPoints | True / 20000 | |
| MaxExpBonus / MaxSpBonus | 3.5 / 3.5 | |
| MaxRunSpeed / MaxPAtkSpeed / MaxMAtkSpeed | 250 / 1500 / 1999 | |
| MaxPCritRate / MaxMCritRate / MaxEvasion | 500 / 200 / 250 | |
| EnchantChanceElementStone/Crystal/Jewel/Energy | 50 / 30 / 20 / 10 (%) | |
| DisableOverEnchanting / OverEnchantProtection / Punishment | True / True / JAIL | |
| AugmentationNG/Mid/High/TopSkillChance | 15 / 30 / 45 / 60 (%) | |
| RetailLikeAugmentation | True | Цвета 55/35/7/3 |
| AugmentationBlackList | 6656…22175 (66 id) | Боссовая бижутерия |
| EnableFameSystem | **False** | Fame выключен |
| CraftingEnabled / CraftMasterwork | True / True | RecipeLimit 50 |
| DaysBeforeJoinAClan / CreateAClan / Dissolve | 1 / 10 / 7 | |
| AltMaxNumOfClansInAlly | 3 | |
| Inventory slots | 80/100/250, склад 100/120/200, freight 200 | |
| AutoLoot / AutoLootHerbs / AutoLootRaids | False / False / False | |
| RaidLootRightsInterval / CCSize | 900 сек / 45 | |
| UnstuckInterval / PlayerSpawnProtection | 300 / 600 | |
| MaxFreeTeleportLevel / DeleteCharAfterDays | 40 / 7 | |
| PartyXpCutoffMethod | level / 20 | |
| RandomizeAutoAttackDamage | True | |

## PVP.ini

| Параметр | Значение |
|---|---|
| ListOfNonDroppableItems | 57 (адена) и др. — не дропаются |
| MinimumPKRequiredToDrop | 6 |
| PvPVsNormalTime / PvPVsPvPTime | 120000 / 60000 мс (флаг) |
| AntiFeedEnable | False |
| AwardPKKillPVPPoint | False |

## General.ini (ключевое)

| Параметр | Значение | Комментарий |
|---|---|---|
| GMStartupInvisible / GMStartupInvulnerable / GMStartupBuilderHide | False / False / False | Автоматические GM invul/invisible отключены; BuilderHide тоже выключен, иначе включает оба флага через setHiding. См. [52](52-gm-login-and-olympiad-observer-cleanup.md). |
| LogChat / LogItems / GMAudit | False | Логирование выключено |
| SkillCheckEnable / Remove | True / True | Чистка невалидных скиллов |
| AutoDestroyDroppedItemAfter | 600 сек | |
| CharacterDataStoreInterval | 15 мин | Автосейв персонажей |
| SaveDroppedItem | False | Дроп на земле не сохраняется |
| HtmCache | False | Кэш HTML выключен |
| GlobalChat / TradeChat | ON / ON | |
| AllowWarehouse/Refund/Mail/Attachments/Wear | True | |
| EnableCommunityBoard / BBSDefault | True / _bbshome | |
| UseChatFilter | False (замена: `ChatFilterChars = ^_^`) | |
| AllowManor / AltManorRefreshTime | True / 20 | |
| AllowLottery | True | Билет 2000, приз 50000 |
| AltFishChampionshipEnabled | True | Награды 800k…100k |
| AltItemAuctionEnabled | True | |
| CustomNpcData / CustomTeleportTable / CustomSkillsLoad / CustomItemsLoad / CustomMultisellLoad / CustomBuyListLoad | все **True** | Загрузка кастом-данных включена (default False) |
| AltBirthdayGift | 22187 | |
| EnableBotReportButton | True | |
| EnablePrimeShop | False | |
| DefaultPunish | KICK | За нелегальные действия |

## GeoEngine.ini

| Параметр | Значение |
|---|---|
| PathFinding | 2 (по ячейкам геодаты) |
| GeoDataPath | `./data/geodata/` — **каталог пуст, гео не установлено** |
| PathnodePath | `./data/pathnode/` |
| PathFindBuffers | 100x6;128x6;192x6;256x4;320x4;384x4;500x2 |

## GrandBoss.ini (респавны, часы)

| Босс | Interval | Random | Примечание |
|---|---|---|---|
| Antharas | 120 | 24 | WaitTime 20 мин |
| Valakas | 120 | 24 | WaitTime 30 мин |
| Baium | 120 | 3 | |
| Beleth | 120 | 24 | Ретейл 192/148 — ускорено; MinPlayers 36 |
| Core / Orfen / QueenAnt | 30 / 48 / 24 | 2 | |
| Frintezza | 48 | 8 | |

`DisableRaidBossHealFromPlayers = false` — рейды принимают хил/баффы от игроков.

## GraciaSeeds.ini

TiatKillCountForNextState=10, Stage2Length=720 мин, MinLevel=75, Min/MaxPlayers=36/45; EkimusKillCount=5; залы SoI 18–27 игроков.

## NPC.ini

| Параметр | Значение |
|---|---|
| AltMobAgroInPeaceZone | True |
| AltAttackableNpcs | True (NPC можно атаковать) |
| MaxAggroRange | **450** (default 1500 — Mobius-правка) |
| MinNPCLevelForDmgPenalty | 78 (Epilogue-штраф урона) |
| RaidHpRegenMultiplier и др. | 100 (без изменений) |
| AltMobAgroInPeaceZone | True |

## Olympiad.ini

**Олимпиада работает 24/7** (кастом от 10.09.2026): `OlympiadStartTime = 0` (00:00), `OlympiadCPeriod = 86400000` (24 ч) — соревновательный период не закрывается никогда; снят и 10-минутный запрет регистрации перед ролловером (ядро, `registerNoble`). Бой 6 мин; награда — Grand Olympiad Token (13722): 50/40 за классед/нонклассед; Hero при 180 очках; CompetitionDays = все 7. Минимум участников для старта боёв — **2/2** (кастом от 08.09.2026: `OlympiadClassedParticipants = 2`, `OlympiadNonClassedParticipants = 2`; дефолт 5/9 — бои стартуют от 2 зарегистрированных ноблесов). Ежемесячная валидация (1-е число, 12:00, `OlympiadVPeriod = 1 ч`) боёв не прерывает: матчи и регистрация продолжаются, обрабатываются герои и очки.

## Осады

- `Siege.ini`: цикл 2 недели, длина 120 мин, мин. уровень клана 5, макс. 500 кланов; лимиты наёмников: Gludio 100 → Aden 400.
- `SiegeSchedule.xml`: Gludio/Giran/Aden/Goddard/Schuttgart — вс 16:00; Dion/Oren/Innadril/Rune — вс 20:00.
- `FortSiege.ini`: 60 мин, 21 крепость (координаты командиров/флагов), регистрация только своей территории (`JustToTerritory=True`).
- `TerritoryWar.ini`: 120 мин, мин. 40 уровень, MinTerritoryBadgeForNobless=100.
- `ConquerableHallSiege.ini`: мин. клан 4, MaxAttackers 500, Fame выключен.

## FloodProtector.ini (интервалы, 1 тик = 100 мс)

UseItem 0 (выкл), RollDice 42, ItemPetSummon 16, HeroVoice 100, GlobalChat 5, Subclass 20, DropItem 10, ServerBypass 3, MultiSell 10, Transaction 10, Manufacture 3, SendMail 100, CharacterSelect 30, ItemAuction 9, PlayerAction 1. Наказания (`PunishmentType`) везде = none.

## Прочие

- **IdManager.ini**: DatabaseCleanUp=True (чистка БД при старте), диапазон ObjectID 0x10000000–0x7FFFFFFF. На работающем сервере не менять.
- **Interface.ini**: EnableGUI=True (Swing-GUI с админ-командами без входа в игру), DarkTheme=True.
- **Development.ini**: всё выключено, кроме DebugUnknownPackets=True.
- **ClassMaster.xml**: смена класса у NPC выключена (`classChangeEnabled="false"`); опции заданы (1-я профа бесплатно, 2-я 1M адены).
- **DynamicExpRates.xml**: уровни 1–87, все множители exp/sp = 1.
- **BotReportPunishments.xml**: жалобы 25/75/100/125/150/175 → дебаффы 6038–6057.
- **chatfilter.txt**: слова фильтра (фильтр выключен, UseChatFilter=False).
- **ipconfig.xml**: активная ручная настройка127.0.0.1 для клиента на том же ПК; исключает HTTP-автоопределение внешнего IP. **default-ipconfig.xml** — только неактивный образец, его LAN-заглушки не используются. См.[71 — локальная сеть без ожидания внешнего IP](71-local-ipconfig-no-external-detection.md).
- **hexid.txt**: `HexID=<секрет>` и `ServerID=2` (хотя `RequestServerID=1`; см. [04-login-server.md](04-login-server.md)).

## Каталог Custom (`game\config\Custom`) — 44 файла

### Включено (Enabled=true)

| Мод | Суть |
|---|---|
| `OfflineTrade.ini` | **OfflineTradeEnable=True, OfflineCraftEnable=True**, команда `.offline`, цвет имени 808080, OfflineMaxDays=0 (без срока), RestoreOffliners=True |
| `AutoPotions.ini` | Автопотки: CP/HP/MP на 70% (items 5592/5591, 1540/1539/1061/1060, 728); голосовые `.apon/.apoff` выключены |
| `RandomSpawns.ini` | **EnableRandomMonsterSpawns=True**, разброс до 150; список исключений (Sel Mahum, Four Sepulchers и др.) |
| `ServerTime.ini` | DisplayServerTime=True — `/time` показывает время сервера |
| `ChatModeration.ini` | ChatAdmin=True — `.banchat/.unbanchat` для access 100 |
| `SchemeBuffer.ini` | Активный конфиг: до 4 схем, валюта Adena (57), цены из `SchemeBufferSkills.xml` |
| `PrivateStoreRange.ini` | ShopMinRangeFromPlayer=50, ShopMinRangeFromNpc=100 |

### Выключено, но настроено (включаются одним флагом)

| Мод | Суть |
|---|---|
| `AutoPlay.ini` | Автофарм `.play` (**EnableAutoPlay=True** — требуется для ботов FakePlayersAI; подсистемы potion/skill/item включены) |
| `Banking.ini` | Обмен адена↔золото (1 млрд/слиток) |
| `BossAnnouncements.ini` | Анонсы рейдов |
| `CancelReturn.ini` | Возврат снятых баффов |
| `Captcha.ini` | Капча от ботов (100 убийств + рандом) |
| `ChampionMonsters.ini` | Чемпионы 5%, HP×8 |
| `CommunityBoard.ini` | Кастомный CB с магазином/баффером/телепортами (CustomCommunityBoard=False, используется стандартный) |
| `CustomMailManager.ini` | Почта через таблицу `custom_mail` |
| `DelevelManager.ini` | NPC дебаффа уровня (1002000, Gold Einhasad) |
| `DualboxCheck.ini` | Лимиты на IP (все = 0, без ограничений; whitelist 127.0.0.1) |
| `FactionSystem.ini` | Фракции Good/Evil (координаты/цвета заданы) |
| `FakePlayers.ini` | Фейк-игроки (чат, шоты, награда за PvP) |
| `FakePlayersAI.ini` | **Боты FakePlayersAI** — боевые боты на настоящих персонажах-игроках (PvP + олимпиада, `//bot`). → [09-fake-players-ai.md](09-fake-players-ai.md) |
| `FindPvP.ini`, `FreeMounts.ini`, `HellboundStatus.ini`, `OnlineInfo.ini`, `PasswordChange.ini` | Голосовые команды `.findpvp`, свободные маунты, `.hellbound`, `.online`, `.changepassword` |
| `MerchantZeroSellPrice.ini` | Продажа торговцам по 0 |
| `MultilingualSupport.ini` | Языки en;el (по сути только en) |
| `NoblessMaster.ini` | NPC ноблеса (1003000, бесплатно) |
| `NpcStatMultipliers.ini` | Глобальные множители статов мобов (все 1.0) |
| `OfflinePlay.ini` | Оффлайн-автофарм |
| `PremiumSystem.ini` | Премиум: PremiumRateXp/Sp=2, DropAmount=2 (AccountWidePremium=True) |
| `PvpAnnounce.ini` / `PvpRewardItem.ini` / `PvpTitleColor.ini` | Анонсы PK/PvP, награды (57), цвета по рангам 500–5000 |
| `Rebirth.ini` | Реберсы: мин. 75, до 3 раз, 1M адены, наследование скиллов |
| `ScreenWelcomeMessage.ini` | Экран «Welcome to our server!» |
| `SellBuffs.ini` | Продажа баффов `.sellbuff` (57, 100k–100M, до 20 баффов) |
| `StartingLocation.ini` / `StartingTitle.ini` | Старт в Giran / титул Newbie |
| `Transmog.ini` | Трансмогрификация у NPC Zumzi 900009 |
| `WalkerBotProtection.ini` | Защита от L2Walker |
| `WarehouseSorting.ini` | Сортировка склада |
| `AllowedPlayerRaces.ini` | Все расы разрешены |
| `ClassBalance.ini` | Пустые множители баланса классов |

### Вывод по кастому

Из нестандартного на сервере реально работают: **офлайн-торговля и крафт, автопотки (CP/HP/MP 70%), рандомные спавны мобов, scheme-буфер, `/time`, чат-модерация, автофарм `.play` и боты FakePlayersAI**. Остальные моды настроены, но выключены — включаются флагом в своём `.ini` (+ обычно нужен рестарт).

## OlympiadTactics.ini — новый тактический пакет

`game/config/Custom/OlympiadTactics.ini`: `Enabled=true` — новый тактический слой рейтинговых ботов; `DefaultDifficulty=strong` (`normal/experienced/strong`); `TrainingEnabled=true` — допуск новых изолированных тренировок. Ручные изменения флагов требуют перезапуска; GM-сложность можно сохранить у Server Guide. Отключение новых тренировок не отключает recovery. Сложность не повышает характеристики. См. [20](20-olympiad-suite-and-giran-services.md), включая аварийное отключение/порядок отката.

### Актуализация темпа/подготовки

По [документу21](21-strong-tempo-and-training-fixes.md): default strong с реакцией100ms, `TrainingPreparationSeconds=30` (30..60), сохраняется GM-настройкой. Общий120-секундный таймер штатной дуэли включает запуск и selfbuff-подготовку; это не120сек. чистого боя. Ручные изменения читать после перезапуска.


## Актуализация тренировок

[24. Вход, подготовка, новая локация и возврат](24-training-preparation-location-and-return.md): `TrainingArena.java` управляет фазами и восстановлением обоих за1с до боя; `TrainingPeaceZones.java` сохраняет мирный порт обычного мира, исключая только авторизованную тренировочную копию; `BotSpawner.applyTrainingPreparationSelfBuffs` выполняет реальные подготовительные селфкасты. Порог HP/MP отменён, прежние эффекты снимаются при входе; повторные остановки человека после боя убраны.
