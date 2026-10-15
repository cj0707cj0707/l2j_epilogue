# 06. Скрипты: квесты, ИИ, инстансы, события (`game\data\scripts\`)

## Как устроена скриптовая система

- Скрипты — **обычные Java-файлы** (`.java`, 1196 шт.), лежащие по своим пакетам. GameServer **компилирует их при старте** (встроенный компилятор) и грузит как классы.
- Пакет файла обязан совпадать с путём: `data/scripts/quests/Q00255_Tutorial/Q00255_Tutorial.java` → `package quests.Q00255_Tutorial;`
- Классы ядра, доступные скриптам, из JAR: `org.l2jmobius.gameserver.*` — например:
  - `entity.actor.Player`, `entity.actor.Npc`, `entity.Location`
  - `managers.*` (TerritoryWarManager и др.)
  - `mechanics.script.Quest`, `QuestState`, `State`, `QuestSound`
  - `mechanics.events.*` — событийная модель (`OnCreatureAttacked`, `OnPlayerLevelChanged`, `OnPlayerItemPickup`, `ConsumerEventListener` ...)
  - `network.serverpackets.*` — пакеты клиенту
- Управление загрузкой — `game\config\Scripts.xml`: через `<exclude file="..."/>` можно исключить папку/файл из загрузки, вложенными `<include>` — исключить всё, кроме указанных. По умолчанию исключены только `package-info.java` и `handlers` (хендлеры регистрируются иначе). Кэш компиляции: `data/scripts/__init__` (создаётся при первом запуске).

## Корневые каталоги `data\scripts\`

| Каталог | Содержимое |
|---|---|
| `ai\` | ИИ NPC (см. ниже) |
| `quests\` | Квесты (самый большой раздел) |
| `instances\` | Инстанс-зоны (Kamaloka, Pailaka, Seed'ы, SSQ ...) |
| `events\` | Игровые события (CTF, TvT, Deathmatch, свадьбы, сезонные) |
| `features\` | Системные фичи (SkillTransfer) |
| `handlers\` | Хендлеры: админ/user/voiced команды, действия, bypass, чат-каналы, эффекты скиллов, таргеты, предметы-хендлеры, наказания |
| `conquerablehalls\` | Захватываемые клановые холлы (Bandit Stronghold, Wild Beast Reserve, Rainbow Springs, Fortress of Dead, Devastated Castle, Fortress of Resistance) |
| `custom\` | Кастомные скрипты (см. ниже) |
| `vehicles\` | Транспорт: дирижабли и контроллеры (AirShipGludioGracia, Keucereus*, SoD/SoI Controller) |
| `village_master\` | Смена профессий (all классы, клан/альянс мастера) |

## `ai\` — ИИ NPC

| Подкаталог | Что там |
|---|---|
| `areas\` | Зональный ИИ локаций: Hellbound (Tully Workshop, Tower of Naia/Infinitum...), Gracia (Seed of Destruction, Hall of Suffering/Erosion...), Cruma Tower, FourSepulchers, Ketra/Varka, Rune Castle (Venom), Isle of Prayer, Pagan Temple и др. |
| `bosses\` | Гранд-боссы: Antharas, Baium, Valakas, Beleth, Core, Orfen, QueenAnt, QueenShyeed, Sailren, DrChaos, Frintezza |
| `others\` | Сервисные NPC: CastleChamberlain (106 файлов), NewbieGuide (649), HealerTrainer (94), ClassMaster (86), HuntingGroundsTeleport (26), FortressSiegeManager, WyvernManager, NpcBuffers и т.д. |

## `quests\` — квесты

- ~370 квестов: от `Q00001_LettersOfLove` до `Q10281_MutatedKaneusRune` + `Dummy` + `TerritoryWarScripts`.
- Именование: `Q<номер>_<ИмяКвеста>\<ИмяКвеста>.java` + HTML-диалоги рядом (`30008-001.htm`, `tutorial-22.htm` и т.п.).
- Крупные серии: Saga of the ... (Q67–Q100, по 73+ файла), Seven Signs (Q192–Q198), Path of the ... (первая профессия, Q401–Q418), Pailaka (Q128/129/144), Path to Becoming a Lord (Q708–Q716).
- Диалоги квестов лежат прямо в папке квеста и/или в `data\html\common\`.

## `instances\` — инстансы

CastleDungeon, CavernOfThePirateCaptain (Zaken), ChambersOfDelusion, CrystalCaverns, DarkCloudMansion, Kamaloka (обычная + Rim Kamaloka 24 файла), NornilsGarden (+ Quest), Pailaka ×3, SSQ: DisciplesNecropolisPast / HideoutOfTheDawn / SanctumOfTheLordsOfDawn.

## `events\` — события

CaptureTheFlag, Deathmatch, TeamVsTeam, Race, Elpies, Rabbits, CharacterBirthday, GiftOfVitality, HeavyMedal, L2Day, LoveYourGatekeeper, MasterOfEnchanting, SavingSanta, TheValentineEvent, WatermelonNinja, Wedding.

## `handlers\` — обработчики

| Подкаталог | Содержимое |
|---|---|
| `commands\admin\` (85 файлов) | Админ-команды (плагины `//command`), включая `AdminOly` (`//oly` — игроковая команда accessLevel 0) |
| `commands\user\` (15) | Пользовательские команды |
| `commands\voiced\` (16) | Голосовые `.menu`, `.cfg`, `.oly` (дистанционная регистрация на олимпиаду) и т.п. |
| `actions\click\`, `actions\shiftclick\` | Действия клика/шифт-клика |
| `bypass\communityboard\` (9) | Community Board |
| `bypass\npc\` (30) | Bypass-команды NPC |
| `chat\channels\` (14) | Чат-каналы |
| `items\` (31) | Хендлеры предметов (скроллы, свитки, квестовые предметы) |
| `punishments\` (3) | Наказания |
| `skill\effects\` (164) | Эффекты скиллов |
| `skill\targets\` (34) | Типы таргетинга скиллов |

## `custom\` — кастомные скрипты

DelevelManager, EchoCrystals, Enchanter, FactionSystem, FakePlayers, NoblessMaster, RaidbossInfo, SellBuff, ShadowWeapons, SkillEnchanter, Transmog, Validators — соответствуют конфигам в `game\config\Custom\` (→ [03-game-config.md](03-game-config.md)).

### Кастомные сервисные NPC в Гиране: Enchanter, Skill Enchanter, Blacksmith Aug и Adena Giver

Четыре NPC стоят рядом на центральной площади Гирана и дают бесплатные услуги. Диалоги генерируются кодом (HTML формируется внутри скриптов), bypass-навигация — через `Script <ИмяКласса> <событие>` (класс регистрируется в `ScriptManager` по simple name, обработка — `onEvent`). NPC объявлены как `type="Folk"` (иначе не заполнится `_lastFolkNpc` и bypass-события не дойдут до скрипта).

Все четыре NPC **рисуют свои окна в большом окне community board (alt+b), а не в диалоге NPC**:
`onFirstTalk`/`onEvent` отправляют html через `HtmlUtil.sendCBHtml(player, html, npc.getObjectId())`
и возвращают null; objectId NPC прикреплён к странице, поэтому bypass-ссылки проходят штатную
проверку кэша с NPC в качестве origin (радиус 250). Лимит страницы — 48750 символов (ShowBoard ×3)
вместо 8192 у NpcHtmlMessage.

| NPC | npcId | Файлы |
|---|---|---|
| **Enchanter** («Free Enchant») — заточка экипировки | 1004000 | `data\stats\npcs\custom\Enchanter.xml` (displayId 31960), `data\spawns\Others\Enchanter.xml` (83057,148613,-3455), `data\scripts\custom\Enchanter\Enchanter.java` |
| **Skill Enchanter** («Free Skill Enchant») — заточка скиллов | 1005000 | `data\stats\npcs\custom\SkillEnchanter.xml` (displayId 31960), `data\spawns\Others\SkillEnchanter.xml` (83121,148613,-3455, рядом с Enchanter), `data\scripts\custom\SkillEnchanter\SkillEnchanter.java` |
| **Blacksmith Aug** («Skill Augment») — аугментация скиллов на выбор в оружие | 1006000 | `data\stats\npcs\custom\BlacksmithAug.xml` (displayId 31960), `data\spawns\Others\BlacksmithAug.xml` (82993,148613,-3455, рядом с Enchanter), `data\scripts\custom\BlacksmithAug\BlacksmithAug.java`, `data\stats\augmentation\options\25000-25099.xml` (кастомные augment-опции) |
| **Adena Giver** («100 Million Adena») — выдача адены | 1007000 | `data\stats\npcs\custom\AdenaGiver.xml` (displayId 31960), `data\spawns\Others\AdenaGiver.xml` (83185,148613,-3455, рядом со Skill Enchanter), `data\scripts\custom\AdenaGiver\AdenaGiver.java` |

**Enchanter** (1004000): бесплатная заточка экипировки по слотам, +1…+16 (`item.setEnchantLevel`, без расходов). Bypass: `Script Enchanter slot <слот>` / `Script Enchanter ench <слот> <уровень>`. С 12.09.2026 — окно в community board (alt+b), таблица расширена до 750 px (в странице слота заточка в 2 ряда по 8 уровней).

**Skill Enchanter** (1005000, добавлен позже): бесплатная заточка скиллов **сразу до максимального уровня заточки** выбранного параметра:

- В списке показываются только скиллы, выученные до максимального базового уровня (`EnchantSkillGroupsData.getSkillEnchantmentBySkillId(id)` != null и текущий уровень ≥ `EnchantSkillLearn.getBaseLevel()`). Условия применения — как в retail `RequestExEnchantSkill`: игрок 76+, 3-я профессия (`player.getPlayerClass().level() >= 3`), `isAllowedToEnchantSkills()` (не в трансформе/лодке/касте и т.п.).
- Маршруты (параметры) берутся из `EnchantSkillGroupsData` (атрибуты `enchantGroupN` в XML скиллов `data\stats\skills\`). Кодировка уровня зачарованного скилла: **уровень = маршрут*100 + уровень заточки** (101–130, 201–230, 301–330, 401–430); максимум маршрута = размер группы в `EnchantSkillGroups.xml` (+30 для групп 1/2 — 2-е классы, +15 для групп 5/6 — 3-и классы). Максимальный уровень маршрута ищется через `EnchantSkillLearn.isMaxEnchant(маршрут*100+i)`.
- **Вариант Cost исключён из списка** (требование модификации): Cost-маршрут определяется эвристикой — у зачарованного уровня меньше `mpConsume`/`mpInitialConsume`, чем у базового скилла. Остальные маршруты подписываются по тому же сравнению статов база↔зачарованный скилл: Element (`elementPower`/`element`), Debuff (`abnormalType`), Chance (`activateRate`), Time (`abnormalTime`), Power (`power`), Duel (`getPower(true,false)`); нераспознанные — «Enchant N» (имена маршрутов — клиентские данные, на сервере их нет).
- Применение — как в retail: `player.addSkill(SkillData.getSkill(id, маршрут*100+макс), true)` + `sendSkillList()` + `updateUserInfo()`. Уровень пишется в БД `character_skills.skill_level` (например 130 = маршрут 1, +30) и корректно проходит проверку `SkillTreeData.isSkillAllowed` при релогине.
- **Остаться на текущей странице после заточки** (правка 12.09.2026): страница передаётся в самой bypass-ссылке заточки — `Script SkillEnchanter ench <skillId> <маршрут> <страница>` — и после применения список перерисовывается на той же странице (раньше всегда возвращало на первую). Обратная совместимость: старый формат без страницы (`ench <skillId> <маршрут>`) тоже принимается (страница 0).
- Bypass: `Script SkillEnchanter list <страница>` / `Script SkillEnchanter ench <skillId> <маршрут> [страница]`; список пагинирован (12 скиллов на страницу). Скиллы, у которых все маршруты — Cost, показываются с пометкой «Cost only» без кнопок.

**Blacksmith Aug** (1006000, добавлен 12.09.2026; список скиллов 12.09.2026 заменён на retail-набор): бесплатная **аугментация скиллов на выбор в оружие**. Игрок выбирает оружие из инвентаря (не только надетое), затем скилл из списка — скилл аугментируется в оружие как item skill:

- Список предлагаемых скиллов — `OFFERED_SKILLS` в скрипте: `{skillId, уровень, подпись типа}` — **все 178 скиллов датапака, чьё имя начинается с «Item Skill:»** (это retail-скиллы аугментации, ids 3080–3259): 83 активных (кастуются от имени аугмента, попадают в окно умений персонажа), 52 пассивных (`<passive_skill>`, применяются автоматически пока оружие надето) и 43 триггерных (`<attack_skill>` / `<magic_skill>` / `<critical_skill>` — срабатывают в бою с retail-шансами 1–5%). Уровень = максимальный базовый уровень скилла (атрибут `levels` в XML скилла). Пример: Item Skill: Prominence существует в трёх вариантах по таргетингу — 3165 (одиночная цель), 3173 (AoE у цели), 3180 (AoE вокруг себя). Кодировка опций повторяет retail: активные → `<active_skill id level/>`, пассивные → `<passive_skill id level/>`, триггерные → `<attack_skill|magic_skill|critical_skill id level chance/>` (шансы — как у top-grade retail-опций этого скилла). Чтобы добавить скилл: добавить строку в `OFFERED_SKILLS` И опцию в `data\stats\augmentation\options\25000-25099.xml` либо `25100-25199.xml` с id `25000 + индекс` (ids синхронизированы 1:1, `FIRST_OPTION_ID = 25000`; в одном файле максимум 100 опций — ограничение `optionsData.xsd`).
- Механика — штатная система аугментации ядра: `new Augmentation(optionId)` → `item.setAugmentation(...)` (пишется в БД `item_attributes`, переживает релогин). На надетом оружии бонус применяется сразу (`aug.applyBonus(player)`), на снятом — при следующей надевании (`Inventory$ItemSkillsListener`). При снятии оружия скилл уходит из списка, при надевании возвращается.
- Кастомные опции имеют id 25000+ — свободный диапазон (retail-опции аугментации и заточки занимают 1–24949). В тултипе оружия строка аугментации для кастомных id клиентом не подписывается (нет данных в клиентских файлах) — но активные скиллы попадают в окно умений персонажа и кастуются оттуда, пассивные применяются автоматически, триггерные срабатывают в бою; фиолетовое свечение аугмента работает.
- Защита от порчи списка умений: скилл, который игрок **уже знает**, аугментировать нельзя (`player.getKnownSkill(...) != null` → отказ), иначе надетый аугмент заменил бы свой вариант умения, а при снятии оружия умение вообще пропало бы до релогина.
- Дополнительно: замена аугментации («Change») и её снятие («Remove») бесплатно; проверка валидности bypass — skillId обязан быть из списка предложений, optionId никогда не берётся от клиента.
- Bypass: `Script BlacksmithAug` (список оружия) / `Script BlacksmithAug weapon <objectId>` / `Script BlacksmithAug list <objectId> <страница>` / `Script BlacksmithAug aug <objectId> <skillId> <страница>` / `Script BlacksmithAug remove <objectId>`.

**Adena Giver** (1007000, добавлен 15.10.2026): кнопка `Get 100,000,000 Adena` выдаёт ровно **100 000 000 адены** за одно нажатие, бесплатно и повторно, без кулдауна/суточного лимита. Открытие окна само по себе ничего не выдаёт. Bypass: `Script AdenaGiver give`; сумма задана серверной константой `ADENA_AMOUNT = 100_000_000L`, клиент её не передаёт. Перед начислением проверяются правильный NPC (`npc.getId() == NPC_ID`), `npc.canInteract(player)`, состояние игрока и запас до `PlayerConfig.MAX_ADENA`. Если полная сумма не помещается, выдача отклоняется без частичного начисления. Используется штатный `player.addAdena(ItemProcessType.QUEST, ADENA_AMOUNT, npc, true)`; ядро обновляет инвентарь и отправляет системное сообщение. HTML генерируется внутри скрипта и отправляется через `HtmlUtil.sendCBHtml` с objectId NPC. Файлы, установка и проверки → [16-giran-adena-giver.md](16-giran-adena-giver.md).

Отдельно стоит **`fakeplayersai\`** — кастомная система боевых ботов (BotManager / BotSpawner / BotPresets / BotSettings, реестр `registry.properties`): боты — настоящие безклиентские персонажи-игроки, которых ведут штатные авто-движки ядра (AutoPlayTaskManager + AutoUseTaskManager). Умеют PvP в мире и участвуют в олимпиаде; при спавне/загрузке ботам автоматически затачиваются скиллы (Power/Chance до максимума маршрута из `EnchantSkillGroups.xml`: +30 для 2-х классовых групп, +15 для 3-х). Управление — админ-команда `//bot` (`handlers\chat\commands\admin\AdminBot.java`). Подробно → [09-fake-players-ai.md](09-fake-players-ai.md).

## Типовой каркас квеста

```java
public class Q00255_Tutorial extends Quest
{
    private static final int ROIEN = 30008;           // NPC id как константы
    // ...
    public Q00255_Tutorial()
    {
        super(255);                                    // id квеста
        addStartNpc(ROIEN);                            // кто начинает
        addTalkId(ROIEN);                              // диалоги
        addKillId(MOB);                                // хук на убийство
        addCondLevel(...);                             // условия (если требуется)
    }

    @Override
    public String onEvent(String event, Npc npc, Player player) { ... }   // bypass-события
    @Override
    public String onKill(Npc npc, Player killer, boolean isPet) { ... }   // дроп/флаги
    @Override
    public String onTalk(Npc npc, Player player) { ... }                  // диалог
}
```

Состояние квеста хранится в БД `character_quests` (quest_name, var, value).

## Как добавить/изменить скрипт

1. Правка существующего — просто изменить `.java` и **перезапустить GameServer** (либо использовать админ-команду перезагрузки скриптов, если доступна).
2. Новый квест — создать папку `quests\Q00XXX_Name\`, файл `Q00XXX_Name.java` с правильным `package`, зарегистрировать NPC/эвенты в конструкторе, положить HTML рядом.
3. Отключить скрипт целиком — добавить `<exclude file="ИмяПапки"/>` в `game\config\Scripts.xml`.
4. Ошибки компиляции видны в консоли и `log\error0.log`; скрипт с ошибкой просто не загрузится (сервер продолжит работу).
