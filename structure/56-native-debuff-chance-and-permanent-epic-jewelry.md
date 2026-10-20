# 56. Родные шансы дебаффов, Chance-заточка и постоянный эпик +6

> **Актуализация78:** [броня/бижутерия+10, оружие+16; меню заточки включает тренировочных; Cardinal без автокаста1418, доступное самолечение первым приHP<75%](78-bot-gear10-and-cardinal-heal75-no-celestial.md). Противоречащие ниже +6 и приоритет Trance/собственного Celestial заменены78; прежний запрет лечения под invul сохранён.

> **Обновление73:** стан Bluff358 — строго **40%** на обычном и всех Chance-уровнях, для игроков и ботов; native `minChance=maxChance=40`, иммунитеты сохранены. Разворот/сброс цели80%, базовый откат12с и обход72 не изменены. [Карта файлов, проверки и установка73](73-bluff-fixed-stun40.md).

> **Обновление72:** исправлен выбор достижимого полного маршрута Bluff у границы; проверены точные пять эпиков Cardinal+6 и активный локальный IP. [Карта файлов, тесты и установка72](72-bluff-bounded-rear-route-and-cardinal-epic-check.md).

> Уточнение59: только bot Trance97 иDreaming Spirit/Fear115 теперь имеют15%; остальные nativeChance и экипировка сохранены. При3282 на противнике Cardinal действует строгий режим выживания. Актуальная карта: [59](59-cardinal-clarity-survival-and-fixed-control-chance.md).

База: `1090758ae0aa691a64ab1eb8a90e68dc558806e6` (обновление55). Изменения подготовлены и проверены локально; установки на работающий сервер и живых матчей не было.

## 1. Удалённые фиксированные ограничения

- `BotManager`: Phoenix Knight90, Hell Knight91 и Shillien Templar106 больше не регистрируются в `Formulas.FIXED_DEBUFF_RATE_PLAYERS`. Их continuous DEBUFF-навыки снова проходят родной расчёт, а не общий бросок25%. При регистрации и общей заточке удаляется только ID соответствующего бота, включая тренировочного.
- `handlers/skill/effects/TargetCancel.java`: убрана отдельная танковая25%-ная ветка мгновенного сброса цели, в частности Shield Slam/Shield Bash. Работает исходный `Formulas.calcProbability(_chance, effector, effected, skill)`.
- `CardinalTrance`: удалены private Skill/copy/cache и activate/min/max=65. Cardinal97 получает точный родной объект `SkillData` соответствующего изученного уровня1394. Вызов в arena tick сохранён как идемпотентное восстановление родного шаблона, а не ограничитель шанса.
- В скриптах больше нет добавления ботов в fixed-rate set. Оставшийся совместимый hook в поставленном ядре не активируется; шестой core-патч не нужен. Исходный `libs/GameServer.jar` не изменён.

Обычные XML activateRate/minChance/maxChance и локальные параметры родных эффектов **не удаляются**: это данные навыков, а не прежние искусственные общеклассовые ограничения. Общий запрет замены/продления категории дебаффа из обновления28 сохранён. Dagger damage-success70% относится к попаданию физических ударных навыков, не к шансу дебаффа, и не изменён. Снижение защиты танков50% также сохранено.

### Важное следствие штатной формулы

У родного Trance1394/230 (+30 Chance) `activateRate=100`. В этой сборке `Formulas.calcEffectSuccess` при значении>99 возвращает успех **до** обычного расчёта MEN/trait/resistance и до отдельной проверки абсолютного SLEEP trait immunity. Это не новый private100%-ный шаблон: восстановлены именно исходные XML и ядро. Предварительная проверка `DEBUFF_IMMUNITY` по-прежнему блокирует эффект; остальные проверки возможности применения/наложения навыка не изменены. Поэтому нельзя обещать, что +30 Trance всегда проходит всю цепочку применения, либо что обычные сопротивления всегда снижают его шанс. Аналогичный native fast-path возможен у других Chance-навыков с activateRate100, например Shackle403/230.

## 2. Как выбирается заточка

`BotSpawner.applySkillEnchants` работает только с clientless-ботом и уже известными ему навыками. Для harmful-навыка (`isDebuff` либо `hasNegativeEffect`, не passive/self) в приоритете **реальный маршрут Chance**, повышающий activateRate. Это действует и на ударные навыки с дебаффом, которым раньше предпочитался Duel/Power.

- Максимум+30 там, где он есть в `EnchantSkillGroupsData` и `SkillData`.
- Для native третьепрофных маршрутов с пределом+15 используется+15. Несуществующий+30 не создаётся.
- Без маршрута Chance сохраняется обычный навык/прежняя допустимая политика заточки. Например Shield Stun92, Horror65, Judgment401 нельзя превратить в Chance+30 придуманным уровнем.
- Cost/Element/Time/добавление нового дебаффа к чистому нюку не выдаются за Chance. Prominence/Hydro Blast/Hurricane не переводятся на ветку добавления дебаффа ради большего activateRate.
- Новые навыки/ротации не добавляются. Штатные cast/reuse/cost/эффекты берутся из соответствующего родного уровня. `addSkill(..., true)` сохраняет выбранную заточку обычным способом.
- Навыки игроков не перезатачиваются автоматически. Общий XML не менялся. У ботов учитываются существующие класс/изученные навыки и дополнительные ранее разрешённые bot-only навыки.

### Фактически изменённые маршруты относительно обновления55

Таблица рассчитана по полным наследуемым деревьям19 пресетов до уровня85 и прежней политике выбора заточки. Она отделяет новые переключения от навыков, которые и раньше уже имели Chance.

| Профессия | Фактически изменённый маршрут заточки |
| --- | --- |
| Phoenix Knight | 97 Sanctuary → Chance+30; 400 Tribunal → Chance+30 |
| Sagittarius | 101 Stunning Shot → Chance+30; 354 Hamstring Shot → Chance+15 |
| Adventurer | 101 Stunning Shot → Chance+30 |
| Archmage | 1339 Fire Vortex → Chance+15 |
| Soultaker | 1343 Dark Vortex → Chance+15 |
| Wind Rider | 101 Stunning Shot → Chance+30 |
| Moonlight Sentinel | 101 Stunning Shot → Chance+30; 354 Hamstring Shot → Chance+15 |
| Mystic Muse | 1236 Frost Bolt → Chance+30; 1237 Ice Dagger → Chance+30; 1340 Ice Vortex → Chance+15; 1342 Light Vortex → Chance+15 |
| Shillien Templar | 105 Freezing Strike → Chance+30; 115 Power Break → Chance+30; 122 Hex → Chance+30; 279 Lightning Strike → Chance+30 |
| Ghost Hunter | 101 Stunning Shot → Chance+30; 105 Freezing Strike → Chance+30; 115 Power Break → Chance+30; 122 Hex → Chance+30 |
| Ghost Sentinel | 101 Stunning Shot → Chance+30; 105 Freezing Strike → Chance+30; 115 Power Break → Chance+30; 122 Hex → Chance+30; 354 Hamstring Shot → Chance+15 |
| Storm Screamer | 1341 Wind Vortex → Chance+15; 1343 Dark Vortex → Chance+15 |
| Titan | 362 Armor Crush → Chance+15 |
| Grand Khavatari | 281 Soul Breaker → Chance+30 |
| Doombringer | 494 Shoulder Charge → Chance+30; 495 Blade Rush → Chance+30; 793 Rush Impact → Chance+15 |

Отдельно снятие фиксированного25% затрагивает все DEBUFF-навыки трёх указанных танков, в том числе не имеющие Chance (список ниже); снятие65% — Trance Cardinal. У Duelist, Dominator и части остальных профессий Chance-маршруты уже были выбраны правильно: новая политика их не переключает повторно. В частности Hammer Crush260 +30, Seal of Suspension1208 +30 и Bluff358 +15 остаются родными Chance-навыками. В56 предыдущий двойной откат Bluff сохранялся; **обновление57 отменяет его и возвращает штатные8000мс**. Chance+15 остаётся. См. [57](57-default-trance-and-bluff-cooldowns.md).

### Полная карта Chance по19 профессиям

Это аудит доступных изучаемых навыков, **не** перечень обязательных боевых кастов: ротация/условия каждого класса остаются прежними. Последний столбец отдельно фиксирует native fallback DEBUFF-навыки танков, для которых снят25%-ный clamp, но Chance не существует.

| Профессия | Native Chance (ID, имя, заточка, серверный уровень) | Без маршрута Chance: обычный навык/прежняя допустимая заточка |
| --- | --- | --- |
| Duelist (88) | 260 Hammer Crush **+30** (`130`) | — |
| Phoenix Knight (90) | 97 Sanctuary **+30** (`130`); 353 Shield Slam **+15** (`115`); 400 Tribunal **+30** (`230`); 403 Shackle **+30** (`230`); 404 Mass Shackling **+30** (`230`) | 18 Aura of Hate; 28 Aggression; 92 Shield Stun |
| Hell Knight (91) | 127 Hamstring **+30** (`130`); 342 Touch of Death **+15** (`215`); 353 Shield Slam **+15** (`115`); 403 Shackle **+30** (`230`) | 18 Aura of Hate; 28 Aggression; 65 Horror; 92 Shield Stun; 103 Corpse Plague; 401 Judgment; 762 Insane Crusher; 763 Hell Scream |
| Sagittarius (92) | 101 Stunning Shot **+30** (`130`); 354 Hamstring Shot **+15** (`215`) | — |
| Adventurer (93) | 101 Stunning Shot **+30** (`130`); 358 Bluff **+15** (`115`); 412 Sand Bomb **+30** (`230`) | — |
| Archmage (94) | 1069 Sleep **+30** (`130`); 1072 Sleeping Cloud **+30** (`130`); 1074 Surrender To Wind **+30** (`130`); 1083 Surrender To Fire **+30** (`130`); 1160 Slow **+30** (`130`); 1164 Curse: Weakness **+30** (`130`); 1167 Poisonous Cloud **+30** (`330`); 1169 Curse Fear **+30** (`230`); 1222 Curse Chaos **+30** (`130`); 1338 Arcane Chaos **+15** (`115`); 1339 Fire Vortex **+15** (`315`) | — |
| Soultaker (95) | 1064 Silence **+30** (`130`); 1069 Sleep **+30** (`130`); 1083 Surrender To Fire **+30** (`130`); 1160 Slow **+30** (`130`); 1164 Curse: Weakness **+30** (`130`); 1167 Poisonous Cloud **+30** (`330`); 1169 Curse Fear **+30** (`230`); 1170 Anchor **+30** (`130`); 1222 Curse Chaos **+30** (`130`); 1263 Curse Gloom **+30** (`130`); 1269 Curse Disease **+30** (`130`); 1298 Mass Slow **+30** (`130`); 1336 Curse of Doom **+15** (`115`); 1337 Curse of Abyss **+15** (`115`); 1343 Dark Vortex **+15** (`315`); 1381 Mass Fear **+30** (`230`); 1382 Mass Gloom **+30** (`230`) | — |
| Cardinal (97) | 1042 Hold Undead **+30** (`130`); 1069 Sleep **+30** (`130`); 1164 Curse: Weakness **+30** (`130`); 1201 Dryad Root **+30** (`130`); 1360 Mass Block Shield **+15** (`115`); 1361 Mass Block Wind Walk **+15** (`115`); 1394 Trance **+30** (`230`); 1396 Magical Backfire **+30** (`230`) | — |
| Wind Rider (101) | 101 Stunning Shot **+30** (`130`); 102 Entangle **+30** (`130`); 358 Bluff **+15** (`115`); 412 Sand Bomb **+30** (`230`) | — |
| Moonlight Sentinel (102) | 101 Stunning Shot **+30** (`130`); 102 Entangle **+30** (`130`); 354 Hamstring Shot **+15** (`215`) | — |
| Mystic Muse (103) | 1069 Sleep **+30** (`130`); 1071 Surrender To Water **+30** (`130`); 1072 Sleeping Cloud **+30** (`130`); 1164 Curse: Weakness **+30** (`130`); 1169 Curse Fear **+30** (`230`); 1206 Wind Shackle **+30** (`130`); 1223 Surrender To Earth **+30** (`130`); 1236 Frost Bolt **+30** (`330`); 1237 Ice Dagger **+30** (`330`); 1338 Arcane Chaos **+15** (`115`); 1340 Ice Vortex **+15** (`315`); 1342 Light Vortex **+15** (`315`) | — |
| Shillien Templar (106) | 105 Freezing Strike **+30** (`330`); 115 Power Break **+30** (`130`); 122 Hex **+30** (`130`); 223 Sting **+30** (`130`); 279 Lightning Strike **+30** (`330`); 342 Touch of Death **+15** (`215`); 352 Shield Bash **+15** (`115`); 402 Arrest **+30** (`230`) | 18 Aura of Hate; 28 Aggression; 103 Corpse Plague; 129 Poison; 401 Judgment |
| Ghost Hunter (108) | 101 Stunning Shot **+30** (`130`); 105 Freezing Strike **+30** (`330`); 115 Power Break **+30** (`130`); 122 Hex **+30** (`130`); 223 Sting **+30** (`130`); 358 Bluff **+15** (`115`); 412 Sand Bomb **+30** (`230`) | — |
| Ghost Sentinel (109) | 101 Stunning Shot **+30** (`130`); 105 Freezing Strike **+30** (`330`); 115 Power Break **+30** (`130`); 122 Hex **+30** (`130`); 223 Sting **+30** (`130`); 354 Hamstring Shot **+15** (`215`) | — |
| Storm Screamer (110) | 1064 Silence **+30** (`130`); 1069 Sleep **+30** (`130`); 1074 Surrender To Wind **+30** (`130`); 1160 Slow **+30** (`130`); 1167 Poisonous Cloud **+30** (`330`); 1169 Curse Fear **+30** (`230`); 1206 Wind Shackle **+30** (`130`); 1222 Curse Chaos **+30** (`130`); 1224 Surrender To Poison **+30** (`130`); 1338 Arcane Chaos **+15** (`115`); 1341 Wind Vortex **+15** (`315`); 1343 Dark Vortex **+15** (`315`) | — |
| Titan (113) | 260 Hammer Crush **+30** (`130`); 362 Armor Crush **+15** (`315`) | — |
| Grand Khavatari (114) | 95 Cripple **+30** (`130`); 281 Soul Breaker **+30** (`130`) | — |
| Dominator (115) | 260 Hammer Crush **+30** (`130`); 1092 Fear **+30** (`130`); 1096 Seal of Chaos **+30** (`130`); 1097 Dreaming Spirit **+30** (`130`); 1099 Seal of Slow **+30** (`130`); 1104 Seal of Winter **+30** (`130`); 1208 Seal of Binding **+30** (`130`); 1246 Seal of Silence **+30** (`130`); 1247 Seal of Scourge **+30** (`130`); 1248 Seal of Suspension **+30** (`130`); 1366 Seal of Despair **+15** (`115`); 1367 Seal of Disease **+15** (`115`); 1462 Seal of Blockade **+15** (`115`) | — |
| Doombringer (131) | 485 Disarm **+30** (`130`); 494 Shoulder Charge **+30** (`230`); 495 Blade Rush **+30** (`230`); 501 Violent Temper **+30** (`130`); 793 Rush Impact **+15** (`315`); 794 Mass Disarm **+15** (`115`); 1435 Death Mark **+30** (`230`); 1445 Surrender to the Unholy **+30** (`230`) | — |

## 3. Постоянная бижутерия для всех ботов

| Предмет | ID | Слот | Заточка |
| --- | --- | --- | --- |
| Ring of Queen Ant | 6660 | одно кольцо | +6 |
| Ring of Baium | 6658 | второе кольцо | +6 |
| Earring of Zaken | 6659 | одна серьга | +6 |
| Necklace of Valakas | 6657 | ожерелье | +6 |
| Earring of Antharas | 6656 | вторая серьга | +6 |

`BotPresets.EPIC_JEWELS` и `getJewelryIds()` закрепляют все пять уникальных предметов независимо от UseS84/Dynasty/Vesper. Возвращается копия массива. `FakePlayersAI.ini: EnchantJewel=6` сохраняет заточку в постоянном конфиге; будущие явные изменения экипировки/админ-настроек могут изменить её. Armor/Weapon enchant и tier сейчас не менялись.

- Новые roster/training-боты получают пять предметов при обычном `equipGear`.
- Восстановленные из registry/DB roster-боты проходят `registerBot → ensurePresetGear → ensurePresetJewelry`. Миграция **только бижутерии** не вызывает полный destroy/recreate оружия и брони. Прежняя отдельная проверка недостающего/недопустимого оружия/брони остаётся как раньше.
- Уже имеющиеся эпики переиспользуются; недостающие создаются. Старая надетая бижутерия снимается штатным Inventory API и остаётся в инвентаре при миграции.
- Используются native equip/unequip и paperdoll listeners: бонусы не имитируются вручную. `updateDatabase` сохраняет enchant и слот после экипировки/снятия, а не только первоначальную вставку предмета. Это добавлено и в общий `giveAndEquip`.
- Повторная проверка корректного комплекта ничего не создаёт/не перезаписывает. Неверная заточка или снятый эпик восстанавливаются без дублирования предметов.
- `getAllGearIds` знает старые и новые ID для обычного cleanup. Парсер rotation/buffs overrides остаётся независимым от списка экипировки.
- Игрокам бижутерия автоматически не выдаётся.

## 4. Назначение изменённых файлов

| Файл | За что отвечает после изменения |
| --- | --- |
| `game/data/scripts/custom/fakeplayersai/BotManager.java` | Регистрация/восстановление ботов, очистка старого fixed25 gate; прежняя tank-defense логика отдельно |
| `game/data/scripts/custom/fakeplayersai/BotSpawner.java` | Приоритет native Chance, реальные пределы заточки, миграция пяти эпиков, сохранение enchant/слотов |
| `game/data/scripts/custom/fakeplayersai/BotPresets.java` | Постоянные пять ID эпика; defensive copy; старые ID остаются в cleanup |
| `game/data/scripts/custom/fakeplayersai/CardinalTrance.java` | Восстановление точного native1394 вместо private65%-ной копии |
| `game/data/scripts/custom/fakeplayersai/ArenaCombatRules.java` | Сохраняет вызов native Trance repair; изменён поясняющий комментарий |
| `game/data/scripts/handlers/skill/effects/TargetCancel.java` | Родной instant target-cancel roll без танкового25% |
| `game/data/scripts/handlers/chat/commands/admin/AdminBot.java` | Текст админ-интерфейса: Chance приоритетнее, максимум native |
| `game/config/Custom/FakePlayersAI.ini` | Постоянный EnchantJewel=6 |
| `tests/fakeplayersai/NativeDebuffEpicTest.java` | Реальные XML/SkillData/EnchantGroups/деревья19 классов, сравнение старых маршрутов, обе арены, native paperdoll миграция |
| `tests/fakeplayersai/native_debuff_epic_tests.py` | Изолированная компиляция всех custom/effect scripts, запуск аудита и регрессий |
| `tests/fakeplayersai/ArenaOrbitTranceManaTest.java` | Вместо obsolete65% проверяет native Trance, fast-path/DEBUFF_IMMUNITY и сохранённые orbit/mana/boundary правила |
| `tests/fakeplayersai/cp_melee_rush_recovery_tests.py`, `dagger_rear_approach_tests.py` | Исправлены устаревшие source assertions: учитывают уже опубликованный в55 `!controlHold` guard; игровые проверки сохранены |
| `structure/README.md`, `09-fake-players-ai.md`, `41-fixed-trance-orbit-boundary-and-mage-mana.md`, этот документ | Актуальная карта и явное снятие прежней65%-ной инструкции |

## 5. Проверки

Проверки выполняются локально на реальном поставленном ядре/датапаке, без рабочего сервера и БД:

- `native_debuff_epic_tests.py`: 1626 проверок; native Chance/реальные уровни; все19 классов × ranked/training; legacy gate/TargetCancel; идемпотентность; пять уникальных эпиков+6; переиспользование и paperdoll; отсутствие изменений оружия/брони; изоляция override-парсера. Native Item/Inventory slot selection используются реально; запись БД, world/packet/stat-listener callbacks заменены тестовыми seams. Реальные численные бонусы эпика и сохранность после production DB restart отдельно в live-smoke.
- `arena_orbit_trance_mana_tests.py`: 26353 проверки native Trance/orbit/MP/установленного boundary core, fail-closed без boundary; integrity/idempotence/tamper/unknown-JAR guards.
- `dominator_control_shadow_tests.py`: 271540 geometry/control +2465 behavioral checks.
- `dominator_pressure_bluff_tests.py`:1856; `cp_melee_rush_recovery_tests.py`:29287+458; `cardinal_arena_parity_tests.py`:6062.
- `dagger_rear_approach_tests.py`:3168; `berserker_soul_cleanse_tests.py`:714; `escape_shackle_tests.py`:377.
- `olympiad_battle_timer_tests.py`: native BATTLE_PERIOD=90000, JVM verification, точный task-only patch, legacy upgrade, backup/five-patch idempotence/tamper и Linux/Windows startup; все custom/effect scripts компилируются против цепочки пяти патчей. Тренировка не получает олимпийский90с timeout.
- `git diff --check`; исходный JAR SHA256 `8f27196ca9d74b82bbe9ad0d12d70da0196fa4e0d1bf23595267b724c79a23dd` не изменён.

## 6. Установка, live-smoke и откат

1. Остановить GameServer. Сохранить резерв изменяемых скриптов, `FakePlayersAI.ini`, registry/overrides и БД (в частности character skills/items). Не подменять рабочую БД/registry/overrides файлами репозитория.
2. Перенести все изменённые Java-файлы из раздела4, включая `TargetCancel` и `AdminBot`; в рабочем конфиге выставить `EnchantJewel=6`, не затирая прочие локальные параметры. Проверить актуальность полной ранее установленной цепочки пяти core-патчей из55. Новый JAR/новый core-патч этому обновлению не требуются.
3. Полный запуск штатным fail-closed launcher. Hot reload не заменяет этот шаг: старые singleton/scripts и private skill objects должны быть выгружены. При восстановлении ботов мигрируются навыки/бижутерия; новые тренировочные создаются сразу с эпиком. Проверить отсутствие ошибок equip/compile/restore в журнале.
4. Live-smoke: все19 профессий, новый/сохранённый roster и training, бот–бот и бот–игрок. Проверить две серьги/два кольца/ожерелье+6, реальные stat/skill бонусы, сохранность после restart, неизменность оружия/брони; родные уровни навыков и отсутствие25%/65%; отсутствие дублей при повторной регистрации. Проверить обычные cast/reuse/ресурсы/иммунитеты с учётом native activate100 fast-path, непродление CC, Escape Shackle, Soul Cleanse, retreat/CP/Shadow Step/90с.
5. Откат: остановить сервер и восстановить резерв скриптов/конфига. Мигрированные навыки/слоты/предметы уже могли сохраниться в БД: один rollback исходников не отменяет их. Для точного отката использовать согласованный DB backup; старая бижутерия при jewel-only миграции оставалась в сумках, но не полагаться на это вместо резервной копии. Не восстанавливать всю БД поверх нового игрового прогресса без отдельного плана.
