# 09. Система ботов FakePlayersAI (`game\data\scripts\custom\fakeplayersai\`)

> **Текущее состояние 65:** бот115 не использует STUN/Hammer Crush по немили-целям; сближение к дальним противникам — до настоящей дальности Dreaming Spirit (обычно600), не40. Cardinal против близкого уязвимого мили: готовый Trance важнее бега/обычного урона, движение только между попытками до реального сна; лечение/щит и wake-комбо64 сохраняют приоритет. Подробная карта и проверки: [65](65-dominator-ranged-stun-ban-and-cardinal-trance-kite.md). При расхождении с историческими53/62–64 приоритет у65.
> **Сохранено из 64:** под3282 после собственногоTrance цепочка1398→один готовый1230→1394; если1230 на native/AI cooldown, сразу переход кTrance; лечение/щит важнее ([64](64-cardinal-clarity-mana-burn-prominence-trance.md)). Сохранено из63: под3282 Cardinal выживает через Trance/лечение/щит,1398 только после окончания фактического собственногоTrance; у93/101/108 собственный358-STUN → реальный пробег за спину до дистанции удара → только удары/531, без автоатаки даже при0MP ([63](63-cardinal-clarity-trance-and-dagger-bluff-rear.md)). При расхождении с историческими59/62/63 приоритет у64. Сохранено из62: спасательные native self-умения перед кайтом/отходом; root не блокирует все умения ranked131; bounded контроль115 с отдельными requested/landed/exhausted-состояниями; актуальный полный агрегатор тестов ([62](62-combat-survival-priorities-and-audit-regressions.md)). Правила61: Phoenix: готовыйFlame Icon раньшеAngelic/Sacrifice; под406/785 самолечение69 запрещено ([61](61-phoenix-flame-first-and-no-sacrifice-under-icons.md)). правила58/59 сохранены. Все19 классов кайтят танков под110/368/760; Phoenix/Hell/Shillien используют родные lowHP/seed/stage3 механики. При расхождении с историей ниже приоритет у [60](60-tank-defense-kiting-and-native-class-mechanics.md), [59](59-cardinal-clarity-survival-and-fixed-control-chance.md) и [58](58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md).

> Движение под страхом у игроков/ботов/саммонов: [23](23-bounded-fear-movement.md). Общий обработчик Fear, фиксированный радиус отступления, гарантированный возврат.

> BotTactics отвечает исключительно за тактическое управление ботами на арене (Олимпиада и тренировочная арена). Логика спауна, ресурсов, набора скиллов и принудительного сброса — в других файлах системы.

## Файловая структура

| Файл | Назначение |
|---|---|
| `BotManager.java` | Реестр/спаун, ranked-драйвер131; root-hook неподвижных умений; обязательная фаза собственногоBluff |
| `BotSpawner.java` | Спаун/деспаун ботов, выдача экипировки и скиллов, назначение пресетов |
| `BotClassPreset.java` | Пресеты классов: ротация скиллов, тип оружия, флаг кастера |
| `BotPresets.java` | Справочник пресетов по classId, таблицы снаряжения, методы выдачи снаряжения |
| `BotTactics.java` | Выбор native-действия/ownership; правила STUN/дистанции115 и Trance-кайт65; Clarity/Trance и Bluff-rear фазы63, запрет автоатаки под собственным358; сохранены self-support/rooted131/evidence115 |
| `CardinalSurvival.java` | Живой3282, recovery приоритет, после настоящего собственного сна1398→готовый1230→1394; одноразовое окно и cooldown skip64 |
| `BluffRear.java` | Собственный актуальный358-STUN; геометрия/LOS/geodata обхода до задней позиции в радиусе40; освобождение своего маршрута |
| `TankClassMechanics.java` | Native HP/seed/stage3/finisher;785 перед406, живые иконы для запрета69; сильная защита танка для общего кайта |
| `TacticsPolicy.java` | Политики/пороги/скоринг; бюджет двух попыток контроля/проклятий115, CP-weave |
| `OlympiadChase.java` | Фазовый драйвер преследования на Олимпиаде (rush/retreat) |
| `TrainingArena.java` | Тренировочная арена: границы, инициализация пар |
| `CombatAnalytics.java` | Аналитика боевых событий |

## API BotClassPreset — актуальные и совместимые методы

### Основные (текущие) названия
| Метод | Возвращает | Описание |
|---|---|---|
| `getGear()` | `GearKind` | Тип брони: HEAVY / LIGHT / ROBE |
| `getWeaponKind()` | `WeaponKind` | Тип оружия |
| `useArrows()` | `boolean` | Лучник (стрелы) |
| `useSpiritshot()` | `boolean` | Маг (спиритшоты) |
| `isCaster()` | `boolean` | weapon == MAGIC |

### Алиасы совместимости (добавлены 17.10.2026)
Эти методы появились из-за того, что при добавлении Cardinal-бота старые имена были убраны,
но `BotPresets.java` (на сервере) всё ещё их вызывал:
| Старый метод | Делегирует к | Причина сохранения |
|---|---|---|
| `getGearKind()` | `getGear()` | BotPresets вызывал getGearKind() |
| `usesArrows()` | `useArrows()` | BotPresets вызывал usesArrows() |
| `usesBolts()` | `weapon == CROSSBOW` | BotPresets вызывал usesBolts() (метода не было) |
| `usesSpiritshot()` | `useSpiritshot()` | BotPresets вызывал usesSpiritshot() |

## API BotPresets — актуальные и совместимые методы

### Основные (текущие) — новое API после рефакторинга под Cardinal
| Метод | Возвращает | Описание |
|---|---|---|
| `getArmorChest/Legs/Helm/Gloves/Boots(preset, vesper)` | `int` | ID отдельных частей брони |
| `getWeapon(preset, vesper)` | `int` | ID оружия |
| `getShield(vesper)` | `int` | ID щита |
| `getJewels(vesper)` | `int[]` | ID украшений |
| `getAmmo(preset)` | `int` | ID стрел или болтов (0 для не-лучников) |
| `getShot(preset)` | `int` | ID соулшота или спиритшота |
| `getHealPotion()` | `int` | Greater Healing Potion (1539) |
| `getCpPotion()` | `int` | Greater CP Potion (5592) |
| `loadOverride(classId, key, default)` | `String` | Чтение одного переопределения из INI |
| `saveOverride(classId, key, value)` | `void` | Запись одного переопределения в INI |

### Алиасы совместимости (добавлены 17.10.2026)
BotSpawner, BotManager и TrainingArena при добавлении Cardinal не были обновлены
под новые имена методов. Все старые сигнатуры восстановлены как обёртки:
| Старый метод | Делегирует к | Примечание |
|---|---|---|
| `getArmorSet(preset, vesper)` | `(vesper?VESPER:DYNASTY)[gear].clone()` | int[5]: chest,legs,helm,gloves,boots |
| `getShieldId(preset, vesper)` | `getShield(vesper)` или 0 | 0 для двуручных/луков/магов/арбалетов |
| `getWeaponId(preset, vesper)` | `getWeapon(preset, vesper)` | учитывает weaponOverride |
| `getJewelryIds()` | `EPIC_JEWELS.clone()` | Актуально с56: пять эпиков, независимо от tier |
| `getAllGearIds()` | все ID из dynasty+vesper таблиц + EPIC_JEWELS | для уничтожения старого снаряжения |
| `getPotionId()` | `POTION_HEAL` (1539) | — |
| `getCpPotionId()` | `POTION_CP` (5592) | — |
| `getFighterShotId()` | `SOULSHOT_S` (1467) | — |
| `getBlessedSpiritshotId()` | `BLESSED_SPIRITSHOT_S` (3952) | — |
| `getBoltId()` | `BOLT_S` (9637) | — |
| `getArrowId()` | `ARROW_S` (1345) | — |
| `getNames(female)` | `Arrays.asList(MALE/FEMALE_NAMES)` | — |
| `loadOverrides()` | читает INI + применяет ко всем пресетам | bulk-вариант loadOverride |

## Таблицы снаряжения (константы BotPresets)

### Броня (индекс = GearKind.ordinal(): 0=HEAVY, 1=LIGHT, 2=ROBE)
| Тир | chest | legs | helm | gloves | boots |
|---|---|---|---|---|---|
| Dynasty HEAVY | 9416 | 9421 | 9422 | 9423 | 9424 |
| Dynasty LIGHT | 9425 | 9428 | 9429 | 9430 | 9431 |
| Dynasty ROBE | 9432 | 9437 | 9438 | 9439 | 9440 |
| Vesper HEAVY | 13432 | 13438 | 13137 | 13439 | 13440 |
| Vesper LIGHT | 13433 | 13441 | 13138 | 13442 | 13443 |
| Vesper ROBE | 13434 | 13444 | 13139 | 13445 | 13446 |

### Щит: Dynasty 9441, Vesper 13471 (только SWORD-классы)
### Украшения: постоянный эпик +6 (обновление56)
Queen Ant6660, Baium6658, Zaken6659, Valakas6657, Antharas6656. Два кольца, две серьги, ожерелье для всех19 классов, включая training. `EnchantJewel=6`; `UseS84` не переключает бижутерию. Старые Dynasty/Vesper ID сохранены только для совместимости/cleanup. Jewel-only migration сохраняет оружие/броню. Подробности: [56](56-native-debuff-chance-and-permanent-epic-jewelry.md).
### Расходники: SOULSHOT_S=1467, SPIRITSHOT_S=2514, BLESSED_SPIRITSHOT_S=3952, ARROW_S=1345, BOLT_S=9637, POTION_HEAL=1539, POTION_CP=5592

## Пресеты классов

| classId | Класс | Gear | Weapon | female | arrows | spiritshot |
|---|---|---|---|---|---|---|
| 88 | Duelist | LIGHT | DUAL | — | — | — |
| 90 | Phoenix Knight | HEAVY | SWORD | — | — | — |
| 91 | Hell Knight | HEAVY | SWORD | — | — | — |
| 92 | Sagittarius | LIGHT | BOW | — | ✓ | — |
| 93 | Adventurer | LIGHT | DAGGER | — | — | — |
| 94 | Archmage | ROBE | MAGIC | — | — | ✓ |
| 95 | Soultaker | ROBE | MAGIC | — | — | ✓ |
| 96 | Mystic Muse | ROBE | MAGIC | ✓ | — | ✓ |
| **97** | **Cardinal** | **ROBE** | **MAGIC** | **✓** | **—** | **✓** |
| 103 | Doombringer | LIGHT | DUAL | — | — | — |
| 105 | Soul Hound | LIGHT | RAPIER | — | — | — |
| 107 | Trickster | LIGHT | CROSSBOW | — | — | — |
| 108 | Inspector | LIGHT | ANCIENT | — | — | — |
| 110 | Storm Screamer | ROBE | MAGIC | ✓ | — | ✓ |
| 111 | Dominator | ROBE | MAGIC | — | — | ✓ |
| 112 | Doomcryer | ROBE | MAGIC | — | — | ✓ |
| 113 | Maestro | HEAVY | FIST | — | — | — |
| 114 | Fortune Seeker | HEAVY | TWO_HAND | — | — | — |
| 115 | Judicator | ROBE | MAGIC | — | — | ✓ |
| 116 | Titan | LIGHT | TWO_HAND | — | — | — |
| 118 | Grand Khavatari | LIGHT | FIST | — | — | — |
| 131 | Doombringer (3rd) | LIGHT | DUAL | — | — | — |

## Архитектура BotTactics

### State (внутренний класс)
Пер-бот состояние, хранится в `STATES: LinkedHashMap<Integer, State>`.

Ключевые поля:
- `difficulty` — уровень сложности (null = глобальный default)
- `enemy`, `game`, `instance` — идентификация текущего противника/арены
- `anchorX/Y` — стартовая позиция бота при начале боя (общий предел смещения: 1400 единиц)
- `arenaCenterX/Y`, `hasArenaCenter` — центр арены (midpoint спавн-позиций обоих участников), используется для ограничения кайтинга на Олимпиаде
- `moveX/Y`, `moveUntil` — последний запланированный шаг
- `pendingId` — ID скилла, ожидающего подтверждения; `pendingSelf` сохраняет self-target до разрешения каста
- `cleanseLease` — краткое владение целью для срочного Soul Cleanse1510 олимпийского берсеркера; после завершения возвращает управление фазовому драйверу
- `nextStep`, `nextDecision` — таймеры шагов и решений
- `tankDefense`, `dominatorCp`, `rushLoop`, `trainingBurst` — специализированные под-автоматы

### Жизненный цикл
1. `tick()` / `tickDefense()` — вход от родителя (OlympiadChase или TrainingArena)
2. `prepare()` — обновление State: смена врага/арены, вычисление `anchorX/Y` и `arenaCenterX/Y`, HP-бэнд, отслеживание каста врага
3. `act()` — выбор действия: выбор скилла → `step()` → базовая атака
4. `step()` — тактические шаги движения
5. `pollAttempt()` — разрешение pending-скилла

## Метод step() — тактические шаги движения

Вызывается из `act()` при отсутствии pending-скиллов и готовности к действию.

### Виды шагов

| Тип | Условие | Направление | Примечание |
|---|---|---|---|
| Recovery | оружие недоступно ИЛИ нет скиллов | назад от врага, 120 ед. | для безоружных/разоружённых (cooldown 600 мс) |
| Dagger flank | вооружённый дагер, дистанция 1–170 (не включая границы), ещё не сзади; вне срочного действия/каста/контроля | обычный шаг к прежней задне-боковой точке | попытка раз в 2000 мс, независимо от готовности обычных атак; актуально по [47](47-dagger-rear-approach-two-seconds.md) |
| Ranged kite | маг/лучник/арбалетчик/разоружённый + `rangedStepAllowed` | назад от врага, 100 ед. | против мили-классов; при дизарме cooldown 600 мс |

### Ограничения шага
- **Общий предел**: `Math.hypot(px - anchorX, py - anchorY) > 1400` → шаг отменяется (cooldown 1500 мс)
- **Олимпиада, кайтинг**: `Math.hypot(x - arenaCenterX, y - arenaCenterY) > 450` → шаг отменяется (cooldown 1500 мс). Применяется только для ranged-ботов на Олимпиаде против мили-классов
- Geodata-проверка: `GeoEngine.canMoveToTarget`
- После успешного шага: `offensiveActionsSinceStep = 0`

## Поведение при неуязвимости противника (enemy.isInvul())

При появлении на противнике эффекта неуязвимости (Celestial Shield и аналогов):

- **Меле и лучники** (`!isCaster()` + оружие доступно): бот уступает родительскому auto-attack (`return false`). Движок блокирует урон, но анимация и логика атаки продолжаются.
- **Маги** (`isCaster()`): продолжают ротацию скиллов типа `DAMAGE` (нюки — Prominence, Hydro Blast и т.п.). CC/slow-скиллы подавляются флагом `offBlocked`, так как не могут примениться к неуязвимой цели.
- **Общее правило**: флаг `offBlocked = protectedSleep || (enemy.isInvul() && kind != DAMAGE)` — только non-DAMAGE скиллы офензивного характера блокируются; DAMAGE проходит насквозь в движок.

## Поведение при дизарме (weaponUnavailable)

При срабатывании Disarm или аналогичных скиллов (оружие снято):

- Бот переключается в режим **ranged kite** независимо от класса: условие `weaponUnavailable(bot)` добавлено к проверке `(isCaster() || BOW || CROSSBOW)` в методе `step()`.
- Параметр `caster` в `rangedStepAllowed` выставляется как `true` при дизарме → более широкий порог отступления (145 единиц вместо 110).
- Шаговый таймер: **600 мс** (как у recovery-режима), а не 4500 мс как у обычных ranged-ботов.
- После возврата оружия бот автоматически возвращается к своему штатному поведению.

## Политика сложности (TacticsPolicy.Difficulty)

- `EASY`, `MEDIUM`, `STRONG` (default)
- Влияет на время реакции (`reactionMs`), скоринг опций, случайный выбор
- Устанавливается глобально (`setDefaultDifficulty`) или per-bot (`setDifficulty`)

## Система скиллов

### Виды (Kind)
`DAMAGE`, `CONTROL`, `SLOW`, `HEAL`, `DEFENSE`, `BUFF`, `CHARGE`

### Источники ротации
- `BotSpawner.getTacticalRotation` — основная ротация
- `preset.getBuffs()` — бафы
- `BotSpawner.getTacticalDefenses` — защитные скиллы
- `BotSpawner.getTacticalCpSupport` — поддержка CP (для доминатора)

### Специальные случаи
- **Dominator (classId=115)**: CpWeave — автоматическое восстановление CP; Sleep-offense — атака спящего врага через 5 сек
- **Training Berserker (classId=131)**: режим тренировки с Rush-loop (skill 793)
- **Tank**: особые приоритеты stun/root/melee, закрытие дистанции
- **Cardinal (classId=97)**: Trance (1394) + Mana Burn (1398) + Prominence (1230) + Magical BackFire (1396); открывающий дебафф Mana Storm (1399) на противника; самобаф Prayer (1307) перед боем

## Журнал правок

### 15.10.2026 — strong/темп и исправление тренировки

**BotTactics.java**
- Поведение при сложности `STRONG` ускорено: уменьшен порог `offensiveActionsSinceStep` до разрешения следующего шага
- Исправлена тренировочная арена: бот-берсеркер (classId=131) больше не застревает в режиме idle после завершения rush-loop

**TacticsPolicy.java**
- `rangedStepAllowed`: добавлен параметр `offensiveActionsSinceStep`; шаг разрешён раньше после атакующих действий
- Порог определения мили-противника уточнён: `physicalAttackRange < 250`

### 17.10.2026 — ограничение кайтинга на Олимпиаде (радиус 450 от центра арены)

**Файл:** `BotTactics.java`

**Суть:** Дальнобойный бот (маг, лучник, арбалетчик) на Олимпиаде во время кайтинга против мили-классов не может отходить от центра арены дальше чем на 450 единиц координат.

**Центр арены** вычисляется как midpoint спавн-позиций обоих участников в момент начала боя: `arenaCenterX = (bot.getX() + enemy.getX()) / 2`, аналогично Y. Хранится в `State.arenaCenterX/Y` + флаг `hasArenaCenter = true` (только при `game >= 0`, то есть только на Олимпиаде).

**Механика:** В методе `step()`, внутри блока кайтинга (`else if (preset.isCaster() || BOW || CROSSBOW)`), после вычисления целевой точки `x, y` — если бот находится в режиме Олимпиады и расстояние от `(x, y)` до центра арены превышает 450 единиц, шаг отменяется: `s.nextStep = time + 1500; return false;`.

**Ограничение действует:** только на Олимпиаде, только для ranged-классов, только в механике кайтинга против мили-противников. Общий предел `anchorX/Y + 1400` остаётся без изменений как универсальный fallback.

**Изменения в коде:**
- `State`: добавлены поля `int arenaCenterX, arenaCenterY; boolean hasArenaCenter;`
- `prepare()`: при смене врага/арены вычисляется и сохраняется центр арены
- `step()`: добавлена проверка радиуса 450 внутри блока ranged kite

### 17.10.2026 — продолжение атаки при неуязвимости; кайт при дизарме

**Файл:** `BotTactics.java`

**Проблема 1 — неуязвимость (Celestial Shield и аналоги):**
Ранее при `enemy.isInvul()` все офензивные скиллы блокировались флагом `sleeping` (`protectedSleep || enemy.isInvul()`), а базовая атака останавливалась из-за `!offenseAvailable`. Бот полностью простаивал.

**Решение:**
- Введён флаг `offBlocked = protectedSleep || (enemy.isInvul() && kind != Kind.DAMAGE)` внутри цикла скиллов в `act()`. Скиллы типа `DAMAGE` (нюки, физические атаки) не блокируются инвулом — движок сам обрабатывает иммунитет к урону. CC/slow подавляются, так как не могут попасть в неуязвимую цель.
- После обработки скиллов: если враг инвулнерабелен, бот — не маг, оружие доступно → `return false` (уступает parent auto-attack). Меле/лучники продолжают автоатаку.
- Маги выбирают из доступных DAMAGE-скиллов ротации (Prominence, Hydro Blast и т.д.) через обычный `TacticsPolicy.select()`.

**Проблема 2 — дизарм (Disarm и аналоги):**
Ранее разоружённые меле-боты не могли атаковать и не кайтили активно (только слабый recovery-шаг 120 ед. при дистанции <350).

**Решение в `step()`:**
- Условие ranged-kite расширено: `(isCaster() || BOW || CROSSBOW || weaponUnavailable(bot))`.
- Параметр `caster` в `rangedStepAllowed` выставляется как `preset.isCaster() || weaponUnavailable(bot)` → порог отступления 145 ед. (как у мага).
- Шаговый таймер: `recovery || weaponUnavailable(bot) ? 600 : ...` — разоружённый бот шагает каждые 600 мс, а не 4500 мс.
- После возврата оружия бот автоматически возвращается к штатному поведению.

**Затронутые методы:** `act()` (цикл скиллов + fallback auto-attack), `step()` (ranged-kite ветка + таймер).

### 17.10.2026 — Cardinal-бот (classId=97): добавлен в пресеты; исправлена компиляция

**Файлы:** `BotPresets.java`, `BotClassPreset.java`

**Суть:** При добавлении Cardinal-бота (бои на Олимпиаде и тренировочной арене) API `BotPresets` и `BotClassPreset` был рефакторен (методы переименованы), но `BotSpawner.java`, `BotManager.java` и `TrainingArena.java` обновлены не были. Сервер падал при старте с 34 ошибками компиляции.

**BotClassPreset.java — добавлены алиасы:**
- `getGearKind()` → делегирует `getGear()`
- `usesArrows()` → делегирует `useArrows()`
- `usesBolts()` → `weapon == CROSSBOW`
- `usesSpiritshot()` → делегирует `useSpiritshot()`

**BotPresets.java — исправлены внутренние вызовы + добавлены алиасы:**
- Внутри `getArmorChest/Legs/Helm/Gloves/Boots/getAmmo/getShot`: `getGearKind()` → `getGear()`, `usesArrows()` → `useArrows()`, `usesSpiritshot()` → `useSpiritshot()`
- `getArmorSet(preset, vesper)` → возвращает `int[5]` (все части брони)
- `getShieldId(preset, vesper)` → 0 кроме SWORD-классов
- `getWeaponId(preset, vesper)` → учитывает weaponOverride
- Исторически `getJewelryIds()` зависел от tier; с56 → `EPIC_JEWELS.clone()` независимо от UseS84.
- `getAllGearIds()` → все ID из dynasty+vesper для cleanup
- `getPotionId()` → 1539, `getCpPotionId()` → 5592
- `getFighterShotId()` → 1467, `getBlessedSpiritshotId()` → 3952
- `getBoltId()` → 9637, `getArrowId()` → 1345
- `getNames(female)` → `Arrays.asList(MALE/FEMALE_NAMES)`
- `loadOverrides()` → читает `FakePlayersAIOverrides.ini` и применяет rotation/buffs/weapon ко всем пресетам

**Пресет Cardinal (classId=97):**
- Gear: ROBE, Weapon: MAGIC, female=true, spiritshot=true
- Rotation: Trance (1394), Mana Burn (1398), Prominence (1230), Magical BackFire (1396)
- Buffs (self): Prayer (1307), Greater Battle Heal (1218)
- Opening debuff на противника: Mana Storm (1399)
- Тактика: kite 450u после Trance, Mana Burn во сне, Prominence как нюк

## 17.10.2026: общий сбой навыков при входе/спавне

`spawnBot(...) failed ... Skill.getId() because skill is null` вместе с отсутствующими1320/1322 вызван повреждением общего `game/data/stats/skills/01300-01399.xml`, а не отдельным пресетом: в XML оказался placeholder `__XML_B64__`. Восстановлен полный файл99 определений; BotSpawner/BotManager/пресеты этим исправлением не изменены. Общая цепочка `rewardSkills` ломала также вход людей. Диагностика, тесты и установка: [35-character-login-skill-xml-recovery.md](35-character-login-skill-xml-recovery.md).

## 17.10.2026: Кардинал — имя без цифр и Prominence

Для97 исправлено повторное наращивание числового суффикса: `Cardinal` либо уникальный буквенный вариант при занятом имени, с учётом собственного objectId и кэша реестра. `BotSpawner.applyBotOnlySkills` явно выдаёт боту1230/28 при создании, восстановлении и в тренировке до общей заточки; человеческие деревья не меняются. В тактическом бою готовый законный Prominence имеет приоритет перед обычной ротацией/кайтингом, не раньше10с после принятого запроса; оба фильтра сна согласованы для1230/1398 Кардинала. Срочный хил и native ограничения сохранены. Карта файлов/проверки/применение и границы мирового AutoUse: [36-cardinal-name-and-prominence.md](36-cardinal-name-and-prominence.md).

## 17.10.2026: Кардинал — согласование Олимпиады и тренировки

Оба режима используют общий `BotTactics.tick`, но устранены олимпийское вмешательство в owned-цель и удержание900 вместо600 для готового Trance. Новый `CardinalArenaSupport` задаёт реальное временноеregMp×0.10 только противнику разрешённой активной пары, снимает свой owner на всех путях завершения и не меняет глобальный XML1399. В тактическом матче с ботом97 больше нет full-MP floor для обоих ботов. Фактический XML1399 содержит разовый MP drain; прежние обещания -90%/65% в комментариях сами не были реализацией. Диагностика, границы, проверки и установка: [37-cardinal-olympiad-training-parity.md](37-cardinal-olympiad-training-parity.md).

## 17.10.2026: Кардинал — неисчерпаемая мана

Уточнение правила37 по запросу: самому боту97 возвращается полное MP на каждом активном тактическом тике, одинаково в тренировке и Олимпиаде, включая ожидание/выполнение каста. Сначала подтверждается предыдущая попытка, затем восстанавливается ресурс. Пресет и фактический класс97, отсутствие клиента, онлайн и живое состояние проверяются; общие скиллы и MP противника не меняются, `regMp×0.10` сохраняется. Карта изменений, регрессия и применение: [38-cardinal-unlimited-arena-mana.md](38-cardinal-unlimited-arena-mana.md).

## 17.10.2026: физические боты — автоатака при нехватке MP

Немагический бот с нулевым MP/нехваткой на изученную атакующую ротацию уступает обычной атаке вместо recovery/кайтинга. Включены MP-исключение для прежнего запрета автоатак ножей, оружейная дистанция и отдельный выход берсеркера из ресурсного recovery. Старый олимпийский full-MP floor физическим ботам отключён. Для лучников только в этом состоянии временно обнуляется MP-стоимость обычного выстрела, иначе штатное ядро не стреляет при0MP; сами навыки, стрелы и MP не бесплатны. Маги исключены, Cardinal из38 не меняется. Файлы, проверки, безопасная очистка и применение: [39-low-mana-physical-autoattack.md](39-low-mana-physical-autoattack.md).


## 17.10.2026: кайт Soultaker, защита танков и самолечение

Soultaker95 отходит от свободных мили-противников, не ожидая откатов всех нюков. Против магов760 предшествует368 без наложения сильных защит друг на друга. Phoenix90 получает неперсистентную копию Sacrifice69/130 (+30 Power, reuse0), лечит только себя ниже70% с сохранением nativeHPcost/cast. Общее активное самолечение HP/CP ботов запрещено под `isInvul()` на уровне выбора/каста/эффекта. Body Reconstruction833 использует границу≤40%; отдельный олимпийский Rush-драйвер подключён к общему отхилу, тренировочный Rush не вытесняет готовый833. Установка требует согласованного обновления custom-скриптов и восьми effect-handlers, затем полного перезапуска. Файлы, ограничения, тесты и откат: [40-soultaker-tank-recovery-and-invulnerability.md](40-soultaker-tank-recovery-and-invulnerability.md).


## 17.10.2026: Trance65%, окружность450, бот–бот460 и маги безMP

Нативный Chance+30 у1394 даёт activateRate100 и пропускает resistance roll. Cardinal теперь получает private template65/65/65; заточка/обычные статы больше не увеличивают шанс, абсолютные иммунитеты сохраняются. Общий и legacy arena кайт используют устойчивую окружность450 вокруг настоящего центра. Новая native coordinate защита460 регистрируется сразу для обоих ботов, не для боёв с человеком. Требуется согласованная установка custom-скриптов, core-patches и launchers при остановленном GameServer. Автоматическая олимпийская доливкаMP магам снята; исчерпание/недостаток на все атакующие навыки снижаетHP мага до101 в обоих arena-режимах, не повышает HP<101 и не отменяет Cardinal full-MP. Полная карта файлов, тесты, ограничения и откат: [41-fixed-trance-orbit-boundary-and-mage-mana.md](41-fixed-trance-orbit-boundary-and-mage-mana.md).


## 17.10.2026: CP Dominator, сближение88/114 и повторный Rush131

Изученные CP-heal1305/1306 больше не отсекаются сохранённой damage/buff-ротацией115. Duelist88/Grand Khavatari114 против магов сближаются до реального melee envelope независимо от готовых дальних зарядных атак. Общий детектор проверяет настоящий STUN, не сон и не исходный каст; пока на враге есть действующий STUN, берсеркер выполняет только обычный отход/Rush Impact либо ждёт native готовности, без heal/filler/autoattack. Исправлены нулевой зазор после CHARGE, безопасные точки отхода внутри бот–бот450 и повторная готовность burst-умений. Это уточняет приоритет833/low-MP из39/40 только на время STUN; вне STUN они сохранены. Согласованно обновить custom-папку с новым RushStunSupport и полностью перезапустить сервер. Причины, реальные ограничения, проверки и откат: [42-dominator-cp-melee-pressure-and-rush-recovery.md](42-dominator-cp-melee-pressure-and-rush-recovery.md).


## Berserker: Body Reconstruction и сохранённый override

В реальном buffs.131/rotation.131 нет833, поэтому существующий порог≤40% не срабатывал: навык отсутствовал в списке кандидатов. BotTactics теперь обязательно добавляет уже изученный833 для131 независимо от override, одинаково для тренировки и олимпийского tickDefense. NativeMP/reuse15мин, запрет invul и приоритет Rush-only на время STUN сохранены. Причина,94 регрессионные проверки и установка: [43-berserker-body-reconstruction-pool.md](43-berserker-body-reconstruction-pool.md).


## Олимпиада: разнообразие пар №1+№2

Все bot-registration пути используют общий shuffle и независимые5–20с без обхода быстрым тикером/кнопкой. Новый native helper для бесклассовой очереди с управляемыми ботами учитывает последние3 соперника и2 профессии с обеих сторон; старейший ожидающий получает случайного наименее повторяющегося соперника. Если новых вариантов нет, после20с допускается минимальный повтор, чтобы очередь не зависала. Память ограничена/неперсистентна, состав классов и игровые очки/боевые правила сохранены. Нужна согласованная установка custom/core-patches/launchers и полный перезапуск, не один hot reload. Установка,3545 проверок и границы подтверждения: [44-olympiad-random-registration-and-opponent-history.md](44-olympiad-random-registration-and-opponent-history.md).


## Олимпиада: один саммон и статичные имена97/88

Призыв перенесён из повторяемого maintenance в проверенную одноразовую подготовку native game: старый саммон удаляется, новый получает полныйHP до старта; после смерти/отзыва в том же матче повторного нет. Отдельный game-identity claim и разрешение на уровне Summon effect закрывают direct applyEffects; люди/тренировка не меняются. Cardinal/Duelist используют канонические имена без постфиксов с учётом собственного objectId; чужое занятое имя не отбирается, конфликтующий восстановленный ник сохраняется стабильно с warning. Нужны согласованное обновление custom helper/gate/spawner/manager и handler Summon, полный рестарт. Карта файлов, проверки и оговорки: [45-one-olympiad-summon-and-static-cardinal-duelist-names.md](45-one-olympiad-summon-and-static-cardinal-duelist-names.md).


## Cardinal в19-классовом пуле; кинжальщики удерживают melee

Уточнение45: Cardinal/Duelist при занятом основном имени получают фиксированные смысловые Bishop/Gladiator, без цифровых/буквенных постфиксов и без захвата чужих имён. Новый default/ini BotCount19; spawnAll сначала покрывает отсутствующие профессии и не маскирует провал класса дублем. Registry сохраняется после каждой успешной регистрации, включая последний новый бот с правильным именем. В46 вооружённые Adventurer/Wind Rider/Ghost Hunter исключены из recovery-retreat и удалён автоматический flank; в47 отдельный flank восстановлен с интервалом2000мс, без recovery-retreat. Обычное преследование и native blow-условия сохранены, disarm — отдельное защитное исключение. Обновить Java-комплект и своё BotCount=19 без перезаписи production registry, затем полный рестарт. Причины, ограничения живой диагностики и тесты: [46-cardinal-spawn-coverage-and-dagger-melee-pressure.md](46-cardinal-spawn-coverage-and-dagger-melee-pressure.md).


## 18.10.2026: возвращён заход кинжальщиков за спину раз в две секунды

Актуальное уточнение46: вооружённым Adventurer93/Wind Rider101/Ghost Hunter108 возвращена отдельная rear-approach ветка `BotTactics.step`, одинаковая для Олимпиады и тренировки. Старый generic recovery-отход остаётся запрещённым через `TacticsPolicy.daggerStepAllowed`; запрет теперь ограничивает recovery, а не весь `step`. При дистанции<170 и положении не сзади — реальный MoveTo к прежней задне-боковой точке (85 назад/70 вбок от направления врага), шаг≤160. `State.nextStep` резервируется на2000мс **до** geodata/AI: неудачная попытка тоже не повторяется каждый тик. Готовые обычные blows не блокируют обход; срочный heal/defense/interrupt, каст/pending/CC, сон и low-MP fallback сохраняют приоритет. Уже сзади/вдали — штатный melee/pursuit, при disarm — прежний recovery600мс. Не телепорт, не разворот врага и не гарантированный Backstab. Карта файлов, проверки и установка: [47-dagger-rear-approach-two-seconds.md](47-dagger-rear-approach-two-seconds.md).


## Berserker131: срочный Soul Cleanse при ROOT/PhysicalMute

По [48](48-berserker-emergency-soul-cleanse.md) изученный1510 используется как **магический targeted ONE self-cast**, не как физическая атака и не как диспел противника. На первом допустимом100мс тике при `isRooted()`/`isPhysicalMuted()` бот выбирает себя, проверяет nativeMP/reuse/race/магическое молчание, запрашивает обычный `useMagic` и сохраняет себя целью до разрешения pending. В тренировке проверка стоит перед reaction/Rush; на Олимпиаде — перед phase/reaction и выходом по movement-disabled. Это узкое срочное исключение из прежнего Rush-only правила при STUN на противнике, без изменения обычного приоритета Rush/Body Reconstruction вне самоочищения. HP-порога нет; casts/прочий pending/реальный запрещающий контроль не прерываются. Вне этих двух триггеров1510 не выбирается, `kind` не допускает его в атакующие SLOW/Dispel-кандидаты. Стандартные2с cast/15с reuse/MP и до3 снятых дебаффов сохранены. Карта файлов, проверки и применение — документ48.

## Кинжальщики93/101/108: Escape Shackle только при ROOT

По [49](49-dagger-escape-shackle-root-only.md) `BotTactics.escapeShackle` проверяет изученный453 перед обычным reaction/nextDecision в общем `act` обеих арен. Триггер только `isRooted()`: готовый453 получает приоритет; cooldown/недостатокMP/отказ возвращают управление обычной ротации, на следующих тиках сохраняющийся root проверяется снова. Действующий cast/pending другого навыка не прерывается.453 — **физический SELF**, native60MP/1с cast/15с reuse, не магический1510. `BotSpawner` исключает453 из случайных автонавыков/автобафов/подготовки и generic tactical-выбора даже при overrides; `kind` возвращает null. Используются существующие pending/failure, новый State-таймер не нужен. Native XML453 снимает только `ROOT_PHYSICALLY,3`: магический/неснимаемый root не удаляется искусственно. Заход за спину из47 сохраняется после освобождения. Карта файлов, тесты и установка —49.

## Dominator115: обязательный серверный Victory of Pa'agrio

По [50](50-dominator-mandatory-server-victory.md) `BotSpawner.applyDominatorArenaBuff` добавляет1414 к текущему серверному набору перед Олимпиадой и тренировкой **независимо от сохранённых ротаций/бафов**. Только фактический115, бот или человек: изученная версия с заточкой либо штатный1414/1 без выдачи навыка. Прямой эффект только на получателя, без PARTY_CLAN-раздачи противнику. Уже имеющийся1414 не продлевается; после старта бесплатного серверного восстановления нет. Class115/id1414 исключён из CoV-strip даже без изученного навыка, другие классы и семейство остаются под запретом. Уточняет learned-only/override ограничения из34; боевые casts и CP-пул не менялись.


## Cardinal/Bishop и сообщение о противнике при прибытии

По [51](51-cardinal-recovery-and-olympiad-opponent-notice.md) `BotManager.spawnAll` при положительном BotCount сначала покрывает все19 пресетов, даже если старое18 либо19 с дублем уже заполнены; существующие актёры не удаляются,0/disabled сохраняются. Новый `BotRosterRecovery` перед созданием97 ищет потерянных офлайн-ботов Cardinal/Bishop: class/base97, без удаления/World/login-аккаунта, точный текущий AccountPrefix+6цифр. Один ник не даёт права присвоить персонажа; чужие имена не освобождаются. Это read-only поиск с последующим штатным load/register/save; registry сервера не заменять. Уточняет ограничения лимита/восстановления из46.

Новый `OlympiadOpponentNotice` подключён до enabled-выхода AI: по событию окончания телепортации человек получает «Олимпиада: ваш противник — НИК.» из своей нативной пары, после прибытия обоих на стадион и до старта боя. Ограниченные повторы250/1000/2000мс учитывают позднюю публикацию native mode; `olympiadPreStart` — резерв. Дедупликация по identity игры, не номеру стадиона; наблюдатели/clientless-боты/возврат в мир исключены. Цель, UI и разрешение боя не меняются. Установить BotManager + оба новых Java-файла вместе и полностью перезапустить сервер; детали, тестовые границы и live smoke —51.


## 53. Dominator против дальнего боя; общий Bluff

[Полная карта, проверки и установка](53-bluff-reuse-and-dominator-ranged-pressure.md). `BotTactics` распознаёт115 против caster/bow/crossbow, удерживает короткий approach≤40, исключает recovery-шаги назад и дальние filler-касты до сближения. `TacticsPolicy.DominatorPressure` приоритизирует доступный сон/стан → проклятия по действующей rotation → damage. `pollAttempt` продвигает этап только по штатному подтверждению исполнения; CP-поддержка1305/1306 вмешивается без сброса этапа. Для этого сценария старое5-секундное окно проклятий и damage-first после CP заменены фазовым приоритетом; melee-противники, другие классы, Victory1414 и overrides не менялись. Исторически в53 XML358 получал reuse8000→16000 для игроков и ботов; в57 возвращён8000мс; механика разворота/захода за спину остаётся прежней.


## 54. Отход после своего сна; живой саммон атакует раз в3с

[Карта файлов, условия, проверки и установка](54-post-sleep-retreat-and-summon-attack-pulse.md). Новый `SleepRetreat` ведёт Cardinal97 на максимальную доступную внутри450 дистанцию Mana Burn1398 (native900 с модификаторами), Dominator115 — примерно300. Только фактический собственный активный SLEEP, один отход на эффект; обычный MoveTo с geodata, целая хорда внутри круга, без отмены cast/обхода root. Пока действует свой сон,115 не сближается обратно до40; доступные curses/damage/CP выбираются с текущей позиции. `SummonAttackPulse` повторяет native doSummonAttack каждые3000мс даже при неизменной цели саммона, не меняя цель владельца. Общий State-таймер у manager100мс, тактики/тренировки и maintenance; только уже живой собственный саммон, ни призыва, ни оживления. Правило одного саммона из45 сохранено.


## 55. Dominator под своим контролем мили-цели; Shadow Step;90с Олимпиада

[Полная карта файлов/проверки/установка](55-dominator-melee-control-shadow-step-and-90s-olympiad.md). `DominatorControlRetreat` распознаёт актуальный собственный ROOT или STUN от260 по мили-цели, ведёт115 к центральной стороне на200 внутри450. До снятия эффекта разрешены только1245 и1305/1306; нет Hammer/chase/autos/других curses. CP-cast допускается до превышения85%, вне режима остаётся50%. На своём сне без этого контроля действует прежний300ед. отход54. В `BotTactics` изученный821 получает первый наступательный выбор для93/101/108 против mage/caster-пресетов, с native600/MP/reuse/root/LOS/geodata. `OlympiadBattle=90000` требует нового `olympiad-battle-timer` installer: native short-counter переполнялся и не достигал лимита. Пятый патч меняет только i2s→nop в native runGame; обновить вместе все installer/launchers и полностью перезапустить.


## Обновление56: native Chance и постоянная бижутерия

Сняты tank25% (continuous и TargetCancel) и private Trance65%. `BotSpawner.applySkillEnchants` предпочитает реальную Chance-ветку для harmful-навыков, в том числе damage+control: +30 либо native максимум+15. Нет Chance — нет выдуманной заточки. Человеческие навыки не перезатачиваются. Trance1394/230 имеет native activateRate100; это штатный fast-path ядра, а не новый искусственный clamp.

`BotPresets.getJewelryIds` задаёт все пять эпиков; `BotSpawner.ensurePresetJewelry` мигрирует только украшения сохранённых ботов, переиспользует имеющиеся и сохраняет enchant/слоты. Новые roster/training-боты используют тот же пресет. Парсер skill/buff overrides не связан с экипировкой.

Канонический список19 классов/навыков, текущие ограничения и ответственность файлов — [структура56](56-native-debuff-chance-and-permanent-epic-jewelry.md). Старые записи журнала выше описывают историю, не текущую политику бижутерии/шансов.


## Обновление57: откаты Trance/Bluff по умолчанию

Bluff358: native reuse8000мс вместо удвоенного16000, для игроков и ботов всех dagger-классов. Trance1394: XML уже содержит исходный1000мс; в `BotTactics` удалён дополнительный `nextCardinalTrance`/3с timer, включая выбор навыка и pursuit range. Native cooldown/conditions/cast, reaction/backoff и sleep-retreat остаются. Chance+30/+15, эпик+6 и Prominence10с не менялись. Карта файлов/проверки/установка: [57](57-default-trance-and-bluff-cooldowns.md).


## 58. Олимпиада150с / invul-кайт / обычная MP / Maximum Clarity

`InvulnerableMeleeKite` — отдельный общий MoveTo по кругу450 перед наступлением/legacy131; защита собственного каста и освобождение управления после invul. `BotTactics.prepare` запускает боевой отсчёт и `CardinalArenaSupport.start`: native Skill3282/1, self-effect противнику на60с, once-only с точным BuffInfo cleanup. Ни FuncMul0.10, ни HP101, ни общего MP-floor; Cardinal97 full-MP остаётся. `BotManager` не доливает MP остальным классам периодически вне боя. Полная карта файлов, совместимость с пятью core-патчами, тесты/установка/откат: [58](58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md).


## Обновление59: выживание Cardinal и три шанса15%

`CardinalSurvival.java` определяет режим по фактическому BuffInfo3282 и whitelist1394/1418/1230/1218; `BotTactics` выбирает защиту/лечение раньше контроля/урона, сохраняет nativeCelestial движением, не отдаёт запрещённую атаку родителю и очищает survivalKite. `BotSpawner.tacticalSkillAllowed` дублирует guard, `BotManager` включает общий путь при3282 даже без TacticalAI toggle.

`BotControlChance.java` создаёт отдельные неперсистентные15% копии1394 для97 и1097/1092 для115; `CardinalTrance.java` — совместимая точка входа. NativeSkillData/игроки/другие дебаффы/откаты не меняются. Файловая карта, точные приоритеты, ограничения/проверки/установка: [59](59-cardinal-clarity-survival-and-fixed-control-chance.md).


## Дополнение66: заказные бои бот–бот

У Olympiad Trainer добавлены два выбора профессии из текущего пула и «Старт боя». `OrderedBattles.java` выбирает реальные roster-объекты; `OlympiadRegistration.java` не регистрирует зарезервированных ботов, а две точки `BotManager.java` останавливают только мировой бой свободного ожидающего участника. Текущие олимпийские матчи не прерываются.

Шестой core-helper `game/core-patches/ordered-olympiad/OrderedOlympiad.java` под монитором native менеджера ждёт обоих, резервирует свободную арену (сначала15), через5000мс запускает обычный олимпийский task и переносит заказчика штатным наблюдателем. Это настоящий олимпийский матч с обычными очками, не тренировочная дуэль. Прежний diversity matcher/тактика не заменяются.

Полная карта файлов, ограничения, тесты, установка и откат: [66. Заказные олимпийские бои](66-ordered-olympiad-bot-battles.md). Цепочка запуска теперь из **шести** стадий; обновлять core-patches целиком, оба launcher и изменённые Java-скрипты, затем полностью перезапустить. Репозиторный JAR не заменён.
