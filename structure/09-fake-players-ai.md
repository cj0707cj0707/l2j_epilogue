# 09. Система ботов FakePlayersAI (`game\data\scripts\custom\fakeplayersai\`)

> Движение под страхом у игроков/ботов/саммонов: [23](23-bounded-fear-movement.md). Общий обработчик Fear, фиксированный радиус отступления, гарантированный возврат.

> BotTactics отвечает исключительно за тактическое управление ботами на арене (Олимпиада и тренировочная арена). Логика спауна, ресурсов, набора скиллов и принудительного сброса — в других файлах системы.

## Файловая структура

| Файл | Назначение |
|---|---|
| `BotManager.java` | Реестр ботов, флаги состояния, идентификация |
| `BotSpawner.java` | Спаун/деспаун ботов, выдача экипировки и скиллов, назначение пресетов |
| `BotClassPreset.java` | Пресеты классов: ротация скиллов, тип оружия, флаг кастера |
| `BotPresets.java` | Справочник пресетов по classId, таблицы снаряжения, методы выдачи снаряжения |
| `BotTactics.java` | **Тактическое ИИ**: выбор действия, шаги движения, ownership |
| `TacticsPolicy.java` | Политики и пороги: разрешения, скоринг, сложность |
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
| `getJewelryIds()` | `getJewels(BotManager.getSettings().isUseS84())` | без параметра vesper |
| `getAllGearIds()` | все ID из dynasty+vesper таблиц | для уничтожения старого снаряжения |
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
### Украшения: Dynasty {9457, 9455, 9456}, Vesper {14165, 14163, 14164}
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
- `pendingId` — ID скилла, ожидающего подтверждения
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
| Dagger flank | дагер, враг спереди, нет готовых скиллов | в тыл врага | обход для Backstab |
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
- `getJewelryIds()` → `getJewels(BotManager.getSettings().isUseS84())`
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
