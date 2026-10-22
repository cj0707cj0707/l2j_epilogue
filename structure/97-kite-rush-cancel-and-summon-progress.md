# 97. Rush против кайта, Cancel защиты танка и независающий pet-focus

База: `3b1a3378da5ae7b51cf152c6825c1578d4d9163f` (96). Изменены только Java-скрипты AI, регрессии и документация. Нового core-патча, правки XML навыков, изменения урона/неуязвимости/откатов нет.

## 1. Mystic Muse не появился против Storm Screamer: предел вывода

Свежие логи и состояние сервера пользователя недоступны; старые логи в репозитории не использовались. Факт причины конкретного пропущенного входа **не установлен**.

Прослежены следующие пути:

- `BotPresets`: Mystic Muse — **103**, Storm Screamer — **110**. ID95 принадлежит Soultaker, поэтому добавлять95 в mage-Cancel нельзя.
- `BotManager.spawnAll` → `missingRosterPresets` → `spawnBot`/`BotSpawner.createBot` → `registerBot`: сначала покрываются недостающие профессии; при неудачном создании выводится предупреждение, слот не скрывается дублем. Отдельного исключения103 нет.
- `OlympiadRegistration.register`: общий допуск online/noble/alive/not-teleporting, статус регистрации и ожидание5–20с; фильтра, исключающего103/110, нет.
- `OrderedBattles.start/find` и core `OrderedOlympiad`: выбираются реальные свободные боты выбранных классов; обычная регистрация и резервирование сериализованы одним lock.
- Штатный `OlympiadGame.portPlayersToArena` телепортирует обоих участников на стороны±1200. `BotManager` завершает clientless-телепорт через `onTeleported`; `OlympiadCombatGate` до открытия реальных ворот/старта обоих участников держит бой закрытым. Вход без snap из96 сохранён.

Ошибку восстановления персонажа, прерванный native port, рассинхронизацию конкретного матча/instance либо проблему установленного на ПК core нельзя подтвердить статическим кодом. Поэтому спавн, телепорты и gate не переписаны предположительно, таймером не открываются, несуществующий участник не создаётся заново. Регрессии допуска/заказных боёв/состава дополняют проверку, но не являются воспроизведением того живого матча.

## 2. Berserker: убегающий противник не блокирует следующий Rush Impact

В ranked-драйвере `rushDone` мог оставатьсяtrue, пока ожидалось завершение Enuma Elish/Crushing Pain. Уже убегающий противник находился вне их ближней дальности: он открывал следующую подходящую дистанцию, но драйвер всё ещё пытался закрыть предыдущую ближнюю серию. Старый `CAST`-approach filler-умения также мог отдать ход обычному преследованию раньше Rush.

Теперь перед этим approach используется существующий `RoleCombatPolicy.kited`: противник действительно движется наружу от берсеркера, а не просто имеет класс мага. При дистанции≥220, доступном793 и готовности movement-policy следующий Rush rearm не ждёт недостающих ближних filler-кастов. Освобождается только устаревший `CAST` intention, когда реального каста уже нет.

- Продолжающийся кайт может открывать новые Rush неоднократно; once-per-match-флага нет.
- Настоящий каст, pending Rush, собственный CC/root, MP/reuse/items/charges и проверка геодаты/LOS не обходятся.
- Если противник дальше native range600, сначала выполняется обычное сближение. Минимальная native дистанция200 не подменяется: AI использует прежний запас220.
- На близкой дистанции не добавлен принудительный непрерывный отход за убегающим противником.
- Прежняя фаза настоящего live-STUN и её собственный Rush-loop сохранены. В тренировке обычный selector уже выбирает legal793 первым; ranked-фазовый флаг в неё не переносится.

## 3. Cancel во время защиты танка, даже при живом питомце

В96 `tickSummonPriority` стоял раньше mage-Cancel, а два синхронных фильтра запрещали любой owner-directed offensive skill при существующем priority-pet. Поэтому уже имеющийся в коде Cancel для94/103 не мог дойти до защищённого Hell Knight/Phoenix Knight с живым питомцем.

Добавлено **одно узкое исключение**:

- только clientless Archmage94/Mystic Muse103;
- только уже изученный Cancel1056;
- только текущий танк с настоящим действующим110/368/760;
- только не спящий и не неуязвимый хозяин.

`tickSummonPriority` сначала даёт этому ready-Cancel возможность пройти обычный `tickMageCancel`/`act`. Shared candidate и event guard разрешают это же исключение. Весь прочий урон по хозяину по-прежнему запрещён, пока pet-focus действует. Ready-Cancel именно по этой действующей защите танка больше не удерживается generic decision/reaction latency; обычная задержка Cancel против остальных melee/Cardinal не изменена. Собственная native готовность и failure-backoff сохраняются.

Нативный Cancel остаётся600range/30s base reuse; попытка снятия не означает гарантированного dispel. Пока1056 откатывается, недоступен поMP/mute/CC/range/LOS или обычному condition, фаза не зависает в ожидании: продолжается атака питомца либо обычный защитный кайт. Уже начатый каст не обрывается ради смены цели.

**Точное название:** в XML этого сервера760 — `Anti Magic Armor`;110 — `Ultimate Defense`;368 — `Vengeance`. Название «Magical Barrier» из сообщения не использовано для произвольного расширения на постоянные/пассивные защиты. Прежняя кратковременная защитная тройка110/368/760 сохранена.

## 4. Нулевой урон Mystic Muse ↔ Hell Knight: устранённый риск и границы

Точную причину наблюдённого матча без живого состояния назвать нельзя. В исходниках, однако, подтверждён риск: pet-focus возвращалtrue бесконечно, в том числе при защищённом/неатакуемом питомце, недоступном пути или отсутствии пригодного урона. Он не проверял реальный прогресс и мог не допустить ни одной новой атаки на хозяина до конца матча. Это исправление устойчивости, не заявление о доказанной причине того матча.

`SummonPriority` теперь наблюдает реальную потерюHP питомца либо улучшение лучшей дистанции более чем на25:

1. Питомец получает прежний первый приоритет.
2. Если8с нет ни урона, ни реального сближения, приоритет временно снимается на3с. Lease освобождается; возвращается обычный драйвер текущего противника.
3. После3с тот же живой питомец получает новую возможность. Урон/сближение продлевают окно прогресса.
4. Смерть, исчезновение, смена питомца/противника/instance и teardown сбрасывают наблюдение. Accepted request сам по себе не считается уроном.

Это **уточняет96**: защищённый/заблокированный pet больше не означает бесконечное удержание хозяина. Неуязвимый хозяин всё равно не атакуется: существующие cast/attack guards и его защитная фаза остаются обязательными. Питомец не воскрешается, егоHP/эффекты/позиция не меняются. Реально начатый каст не обрывается при истечении окна.

У собственного саммона сохранена команда native `doSummonAttack` каждые3с, но при потере ранее установленного `ATTACK`/цели команда возобновляется сразу. Если сама native/path-команда не установила атаку, дальнейшие попытки по той же цели по-прежнему ждут3с: нет спама pathfinding на каждом тике. Собственный каст саммона, смерть, teleport, чужой владелец/instance и invul-цель по-прежнему исключают команду. Self-target владельца не меняется.

## Карта изменённых файлов

Префикс Java-файлов: `game/data/scripts/custom/fakeplayersai/`.

| Файл | Ответственность |
|---|---|
| `BotManager.java` | Ranked rearm793 против реального наружного кайта; освобождение только stale filler CAST |
| `BotTactics.java` | Единое узкое исключение `tankDefenseCancel`, его вызов до pet-focus, срочный anti-defense Cancel без generic задержки |
| `BotSpawner.java` | Shared tactical candidate разрешает1056 на защищённого хозяина; прочий owner offense остаётся закрытым |
| `BotRecoveryRules.java` | То же исключение в native skill-use event guard; invul/attack guards не ослаблены |
| `SummonPriority.java` | Weak identity питомца, наблюдениеHP/дистанции,8с без прогресса →3с retry-gap, сбросы |
| `SummonAttackPulse.java` | Немедленное восстановление потерянногоATTACK;3с retry при native/path-отказе |
| `tests/fakeplayersai/ArenaKiteRecovery97Test.java` | Реальные методы/шаблоны и guards; обе арены,94/103, танки/питомцы, все3 защиты, gates, timer/lease, Panther и повторный ranked793 |
| `tests/fakeplayersai/DominatorHoldMageCancel80Test.java` | Уточнено ожидание первого тика: готовый Cancel по защите танка уже кастуется, повторный запрос не допускается |
| `tests/fakeplayersai/run_ranked_cyclic_rush_lifecycle_tests.py`, `run_ranked_rush_original_stun_tests.py`, `training_berserker_pressure_tests.py` | Добавлена отсутствовавшая compile-зависимость `GiranServices/Preparation.java`; проверка cast-guard в тренировке ограничена его телом, а не соседним rooted-movement блоком; отсутствующий исторический b8d608d явно отмечаетсяSKIP без пропуска текущих native-тестов |
| `tests/fakeplayersai/passive_barrier_bluff_tests.py` | Новый fixture в полном наборе; воспроизводимые baseline-отказы Cancel/Rush/Pulse |
| `structure/README.md`, `09`, `83`, `96`, этот файл | Навигация и актуальные исключения к правилу96 |

## Проверки

```bash
python tests/fakeplayersai/passive_barrier_bluff_tests.py
python tests/fakeplayersai/run_ranked_cyclic_rush_lifecycle_tests.py
python tests/fakeplayersai/run_ranked_rush_original_stun_tests.py
python tests/fakeplayersai/training_berserker_pressure_tests.py
python tests/fakeplayersai/ordered_olympiad_tests.py
python tests/fakeplayersai/olympiad_diversity_tests.py
python tests/fakeplayersai/run_olympiad_gate_tests.py
python tests/fakeplayersai/olympiad_summon_rules_tests.py
```

Результат прогона97: **24 боевых fixture, 437,099 проверок** (из них669 новых); все custom/effect-скрипты скомпилированы. Отдельно прошли1015 source-exact ranked lifecycle,25140 original-STUN и304 текущие training burst/pressure/lifecycle проверки; ordered/diversity/gate/summon regressions такжеPASS. Историческая training-сверка с отсутствующим в checkout b8d608d отмеченаSKIP; текущая тренировка выполнена полностью. Три baseline97-запуска завершились ожидаемыми assertion именно на старых ветках Cancel/Rush/Pulse.

Дополнительно каждый из следующих запусков **должен завершиться с ошибкой assertion**: он компилирует новые сценарии с соответствующими production-файлами из базы96, чтобы показать регрессию именно исправленной ветки. Ненужные server/world/DB-сервисы в фикстурах не запускаются.

```bash
python tests/fakeplayersai/passive_barrier_bluff_tests.py --regression-baseline97 cancel
python tests/fakeplayersai/passive_barrier_bluff_tests.py --regression-baseline97 rush
python tests/fakeplayersai/passive_barrier_bluff_tests.py --regression-baseline97 pulse
```

Headless-тесты проверяют выбор/допуск/сохранность фаз и команды реального native пути, но записывают cast/movement вместо живой сетевой сессии. Они не доказывают фактическое попадание или появление модели на клиенте пользователя.

## Установка

Остановить сервер, сохранить рабочую копию, обновить перечисленные script-файлы **вместе** и полностью перезапустить через штатный launcher. В97 `libs/GameServer.jar` и файлы `game/core-patches` не менялись; пересобирать core из-за97 не нужно. Если96 ещё не устанавливался, сначала выполнить его безопасную инструкцию для согласованной шестипатчевой core-цепочки. Не смешивать новый helper96 с устаревшим установленным JAR.
