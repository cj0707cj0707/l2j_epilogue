# 48. Берсеркер: срочный Soul Cleanse при руте и запрете физических умений

База: `465a224d31c934822d814feed16ddce0af1138f1` (`main`).
Уточняет приоритеты берсеркера131 из42/43. Механика кинжальщиков47 не меняется.

## Что найдено

Native Soul Cleanse1510 из `game/data/stats/skills/01500-01599.xml`:

- **`isMagic=1`, `targetType=ONE`**; требуется выделить себя, а не передать self-эффект атакующему навыку;
- cast2000мс, reuse15000мс, MP25+7 initial; условие цели — расаKAMAEL;
- обычный `DispelByCategory`, debuff, rate100, max3. Не специальное принудительное снятие ROOT/PhysicalMute; если дебаффов больше трёх или есть неснимаемые эффекты, снятие нужного эффекта не гарантируется.

Прежняя классификация `DispelByCategory`/DISPEL превращала1510 в SLOW-кандидат обычной атакующей ротации, если добавить его туда. Кроме того, олимпийский фазовый драйвер возвращался по `isMovementDisabled()` ещё до любых действий; ROOT попадал под этот выход. Обычный `tickDefense` ограничен HP≤40% и не подходил для срочного очищения при любом HP.

## Новое правило

Только управляемый бот берсеркера/Doombringer **classId131**, с реально изученным1510:

1. Триггер — настоящий `isRooted()` **или** `isPhysicalMuted()` (`PSYCHICAL_MUTED` в native enum). Замедление, disarm и запрет обычных физических ударов сами по себе не являются этими триггерами.
2. На ближайшем допустимом боевом тике выбирает себя через `bot.setTarget(bot)` **до проверки цели и `bot.useMagic(skill, true, true)`**. Это обычный запрос магического навыка, не `applyEffects` и не ручное удаление дебаффа.
3. ROOT/PhysicalMute не блокируют эту магию. Сохраняются реальный MP/reuse, блокировка навыка на Олимпиаде, race/target-condition, магическое молчание, stun/sleep/paralyze/fear/прочий запрещающий контроль и all-skills lock. Неизученный1510 не выдаётся.
4. Приоритет не зависит от HP, дистанции/сна противника, готовности физических атак, difficulty/reaction-таймера или обычного отступления. В тренировке вызов стоит перед reaction/Rush, в олимпийском драйвере — перед phase/reaction и movement-disabled выходом.
5. Это **узкое срочное исключение** из прежнего правила42 «пока противник вSTUN — только Rush». Оно требуется, чтобы берсеркер мог снять собственный рут/физическое молчание, даже когда враг оглушён. Body Reconstruction833 и прочие filler-умения прежний Rush-only приоритет не обходят.
6. Существующий реальный каст/одновременный каст и незавершённый pending другого навыка не прерываются: срочная проверка повторяется после их разрешения. «Сразу» означает ближайший доступный100мс боевой тик без искусственного ожидания ротации, а не отмену native2с времени каста или обход15с отката.
7. На принятой попытке обычный pending сохраняет `pendingSelf=true`; авторотации останавливаются, self-target остаётся до завершения. На Олимпиаде краткая `cleanseLease` защищает target от parent maintenance/retarget; затем восстанавливаются цель-противник и прежнее управление фазами. Countdown/завершённая пара/люди/другие классы не получают каст.
8. Отклонённый native request восстанавливает противника и использует существующий failure backoff (первая пауза500мс), а не повторяет запрос каждый100мс. Ресурсы и reuse не сбрасываются.
9.1510 исключён из обычной offensive-классификации `kind`; в live-авторотации олимпийского берсеркера он также считается driver-managed. Override/пресеты и XML не переписываются.

Сфера этого изменения — **Олимпиада и тренировочная арена**, использующие исследованные боевые драйверы. Мировой AutoPlay отдельным100мс обработчиком самоочищения не расширялся.

## Карта файлов

| Файл | Назначение / изменение |
|---|---|
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | Срочная ветка `soulCleanse`, публичный validated hook `tickSoulCleanse` для фазового драйвера, временный флаг `State.cleanseLease`, использование существующего pending/failure/resource-check; исключение1510 из offensive `kind`. Training `act` вызывает очищение раньше reaction/Rush. |
| `game/data/scripts/custom/fakeplayersai/BotManager.java` | `driveBerserker` вызывает urgent hook до phase throttle/root gate; помечает остановленные engines. Fast parent не переписывает owned/self-cast цель.1510 добавлен в driver-managed навыки. |
| `tests/fakeplayersai/BerserkerSoulCleanseTest.java` | Реальный production emergency/act/poll/public hook на native Player/Skill; только useMagic и UI-доступ к target заменены записывающей фикстурой. |
| `tests/fakeplayersai/berserker_soul_cleanse_tests.py` | Полная компиляция custom/effect-скриптов против поставленных JAR, source-проверка места hook/порядка self-target, запуск Java-регрессий. |
| `structure/09-fake-players-ai.md` | Обновлены State/ownership и актуальная механика1510. |
| `structure/42-dominator-cp-melee-pressure-and-rush-recovery.md`, `43-berserker-body-reconstruction-pool.md` | Пометка о новом узком исключении из Rush-only, чтобы прежнее описание не воспринималось как актуальный безусловный запрет. |
| `structure/README.md`, этот документ | Индекс, описание поведения/проверок и установка. |

## Проверки и границы

`python3 tests/fakeplayersai/berserker_soul_cleanse_tests.py`: **714 проверок**, прошли. Компилируются все custom/effect-скрипты против репозиторного `libs/GameServer.jar`.

Проверены оба arena-режима; ROOT, PhysicalMute, оба сразу; свободный/оглушённый/спящий противник; срочный запрос при100%HP и заведомо будущем reaction-таймере. Тест вызывает настоящий `BotTactics.act`/private `soulCleanse`/`pollAttempt` и публичный validated hook, а не переписанную политику. Проверяет self-target именно в момент useMagic, отсутствие повторного запроса во время каста, удержание цели при исчезновении триггера, возврат противника/освобождение lease/teardown; границу MP31/32, native cooldown, magic silence/CC, unknown skill, человеческого клиента, другие классы, отсутствие ложных срабатываний на disarm/autoattack mute, pending другого умения, native request rejection/backoff и закрытую подготовку/неавторизованную тренировку.

Смежные регрессии прошли: заход кинжальщиков3168, физический low-MP2531, recovery1126, CP/melee/Rush29287+458, Body Reconstruction94, Cardinal parity6062. Это выбранные затронутые регрессии, а не утверждение о запуске всего legacy test-suite.

Фикстура использует настоящий Player и Skill1510/native flags/condition/resource/reuse; три записывающих override (`useMagic`, `setTarget`, `getTarget`) созданы стандартным Java25 classfileAPI из-за private-конструкторов Player. **Реальный кастовый scheduler, сетевой клиент, БД, production GameServer и фактическое снятие дебаффов в живом матче не запускались.** Geodata-адаптер в фикстуре использует пустой временный каталог. Native effect1510/XML остаётся без изменений.

## Применение и откат

1. Остановить GameServer; сохранить свои `BotTactics.java` и `BotManager.java`.
2. Обновить оба файла из `game/data/scripts/custom/fakeplayersai/` согласованно поверх актуального комплекта47. XML1510, JAR/core-patches, ini, registry и БД не заменять.
3. Полностью перезапустить GameServer для компиляции Java-скриптов; не hot reload.
4. Live smoke в тренировке и Олимпиаде: у131 изучен1510, хватаетMP, откат готов; при100%HP наложить ROOT, затем отдельно PhysicalMute. Бот выделяет **себя**, начинает магический Soul Cleanse на ближайшем доступном тике, сохраняет self до окончания, затем возвращается к врагу. Повторить приSTUN на противнике. Проверить неуспешный request, недостатокMP, magic silence, незавершённый другой каст и native15с reuse — бесплатного обхода не должно быть. При нескольких дебаффах проверять фактический native результат, не обещать снятие всех.

Откат: при остановленном сервере вернуть оба production Java-файла из базового коммита/своего согласованного резерва и полностью перезапустить. Данные персонажей и XML этим изменением не мигрируются.
