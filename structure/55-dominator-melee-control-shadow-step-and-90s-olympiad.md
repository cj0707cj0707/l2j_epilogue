# 55. Dominator под своим контролем мили-цели, Shadow Step и Олимпиада90с

> **Актуальное обновление58:** Лимит90с ниже — история55, текущий OlympiadBattle=150000 (150с). Пятый core-патч и механики контроля/Shadow Step сохранены. См. [58](58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md).

База: `791e73a2c3b23aaa9cda918175870a42c602f99c`. Новая общая arena-тактика ботов и пятый проверяемый core-патч; исходный `libs/GameServer.jar` в репозитории не заменяется.

## 1. Dominator115: свой рут / Hammer Crush по мили-противнику

В общей тактике Олимпиады и тренировки новый `DominatorControlRetreat` проверяет **действующий собственный эффект** на фактическом противнике ближнего боя (существующая классификация `meleeOpponent`, не имя персонажа). Фактический115 без клиента, разрешённая пара/матч/инстанс. Люди-Dominator не управляются AI.

Триггеры:

- Любой реально наложенный этим Dominator **ROOT**, включая штатный1208 Seal of Binding. Замедление бега, чужой root, один запрос каста, сопротивление или голый ROOTED-флаг без актуального собственного BuffInfo не включают режим.
- Реально действующий **STUN именно от260 Hammer Crush этого Dominator**. Другой stun не подменяет260.
- BuffInfo должен быть включён, не удалён, совпадать по effector/effected и занимать текущий abnormal slot; на цели требуется соответствующий native ROOTED/STUNNED flag. Не используются предположения о длительности9/30с.

### Действия

1. До следующего обычного decision/reaction — **отойти примерно на200 от противника в сторону центра круга**. Точка: цель +199 по направлению цель→центр; запас1ед. нужен для integer rounding. Если цель точно в центре, используется текущая сторона бота, при полном совпадении — детерминированная ось. Точка ограничена внутренним радиусом449, чтобы после округления не выйти за450. Если уже достаточно далеко с центральной стороны, лишнего движения нет.
2. Полный обычный `MoveTo` принадлежит этой ветке до прибытия, а не200мс. Весь прямой отрезок между внутренними точками лежит внутри круга; geodata проверяет путь. Если старт снаружи из-за другого механизма — путь внутрь, не телепорт. Собственный root/CC не обходится; настоящий cast/pending не прерывается. При отказе пути/AI повтор не чаще500мс, а допустимые оставшиеся действия не блокируются навечно.
3. **Пока собственный root или Hammer-stun действительно действует, разрешены только1245 Steal Essence и1305/1306 CP-поддержка.** Hammer Crush, сон, прочие проклятия/бафы, обычная автоатака, recovery/orbit и родительское преследование исключаются. При недоступном Steal Essence бот не бежит обратно ради Hammer Crush и не подменяет его другим ударом. Изученный1245 включается в этот узкий пул даже при отсутствии в сохранённом override; неизученные навыки не выдаются.
4. CP можно восстанавливать при текущем уровне **≤85%**, то есть настоящий следующий CP-cast разрешён и на85%. После превышения85% новые CP-касты не выбираются. Восстановление идёт только обычными native навыками/MP/reuse; нет `setCurrentCp`, бесплатного долива, ограничения результата ровно85% или гарантии достичь порога до конца контроля. Сохраняется чередование CP и доступного Steal Essence, а не бесконечная очередь CP.
5. CP выделяет самого бота до окончания каста;1305 PARTY_CLAN проходит прежнюю проверку списка целей «только себя», иначе остаётся допустимый1306. Самолечение под invul запрещено, как прежде.
6. Как только собственный эффект снят/закончен/замещён — ограничение исчезает. Останавливается только принадлежащий этой ветке маршрут; чужой MoveTo не отменяется. Обычный Hammer Crush и ротация снова допустимы по штатным условиям.

Вне этого сценария **обычный порог CP остаётся ниже50%**. Против дальнего боя сохраняется pressure53. Собственный сон из54 по-прежнему даёт300ед., но если по мили-цели одновременно есть описанный root/Hammer-stun, приоритет имеет новый200ед./Steal+CP режим.

## 2. Кинжальщики: Shadow Step первым против кастеров

Для Adventurer93/Wind Rider101/Ghost Hunter108 в обоих arena-режимах **821 Shadow Step** получает детерминированный приоритет перед обычными наступательными кандидатами/обходом за спину против кастеров: mage class либо caster-профиль пресета, включая Cardinal и Dominator. Это не разрешение teleport-навыков всех классов или против любого лучника. В пресетах821 уже стоял первым, но общая тактика раньше отбрасывала все TELEPORT_TO_TARGET и не распознавала его как кандидат.

- Изученный821 добавляется в узкий пул против кастера даже при отсутствии в override; классифицируется как CONTROL, а исключение из blanket teleport-фильтра действует **только** для этого класса/ID/противника.
- Native `useMagic(821,true,true)` — не прямой `setXYZ`/самодельный телепорт. Сохраняются600 castRange,100мс hitTime,50MP,75000мс базовый reuse и native TeleportToTarget/TargetCancel.
- Проверяются MP, cooldown, failure-backoff, native conditions, LOS, проходимость к цели, запрет перемещения, cast/pending/CC, arena authority и обычные ограничения сна/invul. Отклонённый запрос не считается телепортацией и не повторяется каждый тик.
- За пределами600 бот штатно сближается; если821 недоступен, обычная разрешённая ротация/погоня продолжается без ожидания75с. Настоящий каст или срочный Escape Shackle при ROOT не вытесняются. После native перемещения дальность/позиция ударов заново проверяется ядром; Backstab не гарантируется.
- Остальные teleport-навыки остаются исключёнными; общий XML821, сохранённые ротации и человеческие персонажи не изменены.

## 3. Олимпийский бой90 секунд и переполнение в ядре

`game/config/Olympiad.ini`: **OlympiadBattle=90000** (миллисекунды). Отсчёт относится к активному бою после `makeCompetitionStart`, не к30-секундной подготовке/возврату; тренировочный Duel/TrainingArena не сокращается.

В исходном native `OlympiadGameTask.runGame()` найдено:

```
int counter = 0;
while (counter < BATTLE_PERIOD) {
    Thread.sleep(10000);
    if (game.haveWinner()) break;
    broadcastBattleState();
    counter = (short)(counter + 10000); // native i2s: переполнение после30000
}
```

Значение со знаком short никогда не достигает90000 (как и прежних360000), поэтому одной смены ini недостаточно. Пятый патч **olympiad-battle-timer-v1** заменяет единственный `i2s` в exact hash-pinned `runGame()Z` на `nop`. Счётчик остаётся int:9 интервалов по10000мс дают90000, при существующем победителе цикл завершается раньше. Другие инструкции/методы, подготовка, штатные `validateWinner`, очистка эффектов, возврат участников, учёт урона/очков не меняются. Результат по тайм-ауту определяет **прежняя native логика**, не принудительная ничья. Сохраняется штатный10-секундный шаг проверки и обычная задержка планировщика JVM; это не новый миллисекундно-точный watchdog.

### Безопасная установка core-патча

Новый `InstallOlympiadBattleTimer.java` принимает только проверенную базу observer-cleanup-v1 с исходным task hash `f1bd2e0e8eedc9b39621c622059b2908b09479356ac1d8f2061a9cd073ee7250`. Проверяет прежний payload/источники helpers, меняет только `OlympiadGameTask.class`, добавляет marker с хешами, проверяет выход, создаёт fsync backup `.pre-olympiad-battle-timer-<hash>` и атомарно заменяет JAR. Неизвестная/изменённая база, изменённый source/marker/payload или неправильный backup закрывают установку ошибкой. Повторный запуск лишь проверяет, не меняя JAR.

Четыре предыдущих установщика при наличии нового marker делегируют полную проверку пятому. Observer installer понимает ранее установленный exact52 source hash и текущий обновлённый вариант; прежний payload остаётся проверяемым. Обновление с уже установленного52 проверяется отдельно. Linux `GameServerTask.sh` и Windows `InstallDebuffGuard.cmd` устанавливают/проверяют все пять патчей **до запуска**, с отказом при ошибке. Старый launcher или прямой запуск исходного JAR не обеспечивает исправленный90с timeout.

## 4. Карта файлов

| Файл | Ответственность |
|---|---|
| `game/data/scripts/custom/fakeplayersai/DominatorControlRetreat.java` | Актуальный собственный ROOT/260-STUN; центральный200ед. waypoint; geodata,500мс retry, владение целым пробегом |
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | Приоритет/reset контроля; узкий Steal+CP пул и запрет chase/autos/других действий; scoped821-кандидат и первый выбор |
| `game/data/scripts/custom/fakeplayersai/TacticsPolicy.java` | Порог CP≤85 только под controlHold; прежняя двухаргументная CP-policy/порог50 сохранены; CP/attack weave |
| `game/config/Olympiad.ini` | Активный бой90000мс |
| `game/core-patches/olympiad-battle-timer/InstallOlympiadBattleTimer.java` | Пятый атомарный проверяемый патч native signed-short overflow |
| `game/core-patches/{debuff-no-refresh,arena-boundary,olympiad-diversity,observer-cleanup}/Install*.java` | Делегирование проверки нового combined core; observer legacy upgrade |
| `game/GameServerTask.sh`, `game/InstallDebuffGuard.cmd` | Fail-closed цепочка пяти патчей Linux/Windows |
| `tests/fakeplayersai/DominatorControlShadowTest.java` | Actual native effects/act/poll/targets/costs/reuse и запись MoveTo/useMagic, оба режима и три dagger-класса |
| `tests/fakeplayersai/dominator_control_shadow_tests.py` | Wiring guards, компиляция, новые native проверки вместе с regression54 |
| `tests/fakeplayersai/olympiad_battle_timer_tests.py` | Копия JAR, exact task-only bytecode fix, full-chain/legacy/idempotence/tamper/backup/config90s и JVM verification |
| `tests/fakeplayersai/dominator_cp_titan_pressure_tests.py` | Обновлённый overload marker; scoped Cardinal exception; явный SKIP только отсутствующего исторического a1bb787 |
| `structure/README.md`, `09`, `47`, `52`, `53`, `54` | Навигация и уточнение прежних правил |

## 5. Проверки и границы подтверждения

Локально пройдены:

- `dominator_control_shadow_tests.py`: **2465 новых native проверок** режима200/CP85/узкой ротации и первого Shadow Step, плюс **271540** regression54 сна/круга/саммонов; компиляция всех custom/effect scripts.
- `olympiad_battle_timer_tests.py`: upgrade старого observer52, exact task-only bytecode diff, verified backup, идемпотентность всех пяти installer, отказ при неизвестном/подменённом ядре или source, реальный `OlympiadConfig.load()` и `BATTLE_PERIOD=90000`, `-Xverify:all`, компиляция скриптов против пяти патчей, Windows/Linux launch-chain.
- `dominator_pressure_bluff_tests.py`:1856; `cp_melee_rush_recovery_tests.py`:29287+458; `dominator_cp_titan_pressure_tests.py`:458 текущих production проверок. Только необязательная историческая репродукция `a1bb787` явно **SKIP**: этого старого commit object нет в локальной истории. Проверки текущего CP/target handlers не пропущены.
- `dagger_rear_approach_tests.py`:3168; `escape_shackle_tests.py`:377; `berserker_soul_cleanse_tests.py`:714.
- `observer_cleanup_tests.py`:3891 до и3891 после патча; `arena_orbit_trance_mana_tests.py`:26276; `cardinal_arena_parity_tests.py`:6062; `olympiad_summon_rules_tests.py`:10554+3530. Соответствующие native/script compilation и wiring guards пройдены.
- `git diff --check` без ошибок. Исходный `libs/GameServer.jar` не изменён, SHA-256: `8f27196ca9d74b82bbe9ad0d12d70da0196fa4e0d1bf23595267b724c79a23dd`.

Полный живой runGame90с/движение по production geodata/клиентские пакеты/БД не запускались. Core-тест проверяет реальные disassembly/config/class verification, моделирует только проверенный шаг счётчика и не выдаёт этот прогон за живой90-секундный матч. AI-тесты используют native SkillData/Player/BuffInfo и настоящий private act/poll; запись cast/MoveTo заменяет только взаимодействие с живым миром.

## 6. Установка, live smoke, откат

1. Остановить GameServer, сохранить backup текущих Java/config/launchers/core-patches/JAR. Установить весь production комплект из этого коммита **вместе**, включая новый helper, новый installer, четыре обновлённых старых installer, launcher и Olympiad.ini. Не заменять registry/БД/production overrides тестовыми данными.
2. Запустить штатным обновлённым launcher (Windows при необходимости `InstallDebuffGuard.cmd`). Проверить `log/debuff-guard-install.log`: пять проверенных патчей, отсутствие ошибок компиляции. Затем полный запуск GameServer. Не ограничиваться hot reload и не запускать исходный JAR напрямую.
3. Dominator против мили: успешный1208 → отход к центру примерно200; успешный260 с реальным STUN → то же. Проверить удержание дистанции/отсутствие Hammer/других curses/autos, Steal+CP, CP80/85/>85, недостатокMP/reuse, собственный root, снятие контроля, сопротивление/чужой эффект. Повторить на тренировке и против человека/бота.
4. Все три dagger-класса против мага/Cardinal/Dominator: при изученном готовом821 первая наступательная попытка в600 — Shadow Step; при cooldown/root/нетMP/стене — никакого обхода запретов, нормальные доступные действия. Заход за спину раз2с из47 и Escape Shackle при root из49 сохраняются.
5. Олимпиада: оба участника живы после90с активного боя — матч завершается штатным расчётом и возвращает игроков; убийство до90 завершает раньше. Отдельно проверить human–human, human–bot, bot–bot и отсутствие изменения длительности подготовки/тренировки. Повторный запуск launcher не перепатчивает JAR.
6. Для отката55 остановить сервер, согласованно вернуть файлы к базе55, удалить только новый helper/installer вместе с их вызовами и восстановить verified `.pre-olympiad-battle-timer-*` JAR (или заново установить прежние четыре патча на проверенную исходную копию). Возврат одного ini оставит исправленный int-счётчик, но изменит только выбранную длительность. Не возвращать один старый launcher поверх нового combined JAR: нужен согласованный комплект.
