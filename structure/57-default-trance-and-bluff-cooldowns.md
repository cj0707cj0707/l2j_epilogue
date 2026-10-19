# 57. Trance и Bluff: штатные откаты

База: `0e58264a6f320d004e31f6b8c86ce1893accabc7` (обновление56). Отменены только дополнительные изменения откатов Trance/Cardinal и Bluff/кинжальщиков. Изменения внесены в репозиторий, не установлены на работающий сервер.

## Штатные значения и источник

Проверена последняя целая версия до изменения Cardinal: `ee21f68e96cd6e25e378ac89fde15e8a7065aaab` (родитель `9b9a527f`). В ней Trance1394 имеет reuseDelay1000мс, Bluff358 —8000мс. Это значения данной сборки, не предположение о другом L2-сервере.

| Навык | После обновления | Область |
| --- | --- | --- |
| Trance1394 | Native базовый reuse1000мс; отдельного bot-only3с таймера больше нет | Cardinal97 в Олимпиаде и тренировке; общий XML игроков не переписывается |
| Bluff358 | Базовый reuse8000 вместо16000мс | Игроки и боты, включая93/101/108 и native Chance101–115 |

Это базовые параметры навыков. Итоговый reuse рассчитывает ядро с обычными модификаторами персонажа. Cast time — отдельный параметр: у Trance hitTime3000мс не менялся; отмена3с AI-delay не превращает его в мгновенный каст и не разрешает прерывать текущий каст.

## Реализация

- `game/data/stats/skills/00300-00399.xml`: только `<reuseDelay>` у358 изменён16000→8000. Остальные навыки, эффекты, шансы и ветки заточки сохранены.
- `game/data/stats/skills/01300-01399.xml`: **не изменён**. Trance уже имеет родной reuse1000 после восстановления полного XML в обновлении35. Повреждавший документ коммит9b9a527f назывался «10s», но фактически оставил placeholder вместо XML; восстанавливать его содержимое нельзя. SHA256 текущего полного XML остаётся `40c7683cb8eb9d219c24336bb15d7bc50eebfb6b3315950eb61b1e5fe4f9c528`.
- `game/data/scripts/custom/fakeplayersai/BotTactics.java`: удалены поле `State.nextCardinalTrance`, его reset, проверка в выборе навыка, проверка в подходе и установка time+3000 после принятого запроса. И выбор Trance, и подход на его дистанцию теперь учитывают обычный `bot.isSkillDisabled(skill)`, ресурсы, conditions, состояние цели, LOS и прочие прежние ограничения, без второго таймера.
- `tests/fakeplayersai/DominatorPressureBluffTest.java`: обновлены ожидания Bluff8с; добавлены проверки Trance1с для базовых/Time/Chance уровней и вызов настоящего `act/poll` в обоих arena-режимах: native cooldown блокирует Trance, его истечение разрешает каст раньше удалённого3с AI-delay.
- `tests/fakeplayersai/dominator_pressure_bluff_tests.py`: XML-инвариант только358/reuse, исходный Trance1000 и отсутствие отдельного таймера, компиляция всех custom/effect scripts.
- `tests/fakeplayersai/cardinal_arena_parity_tests.py`: вместо требования дополнительного timer guard проверяется его отсутствие; native readiness/conditions и shared arena wiring сохранены.
- `structure/README.md`, `09-fake-players-ai.md`, `14-bluff-turn.md`, `53-bluff-reuse-and-dominator-ranged-pressure.md`, `56-native-debuff-chance-and-permanent-epic-jewelry.md`: обозначен актуальный отменённый override; предыдущие изменения описываются как история.

## Что не отменяется

Chance+30 там, где доступен, native+15 у Bluff; снятие25%/65% clamps; постоянные пять эпиков+6; приоритеты ротаций, родные иммунитеты/затраты/условия; запрет продления категории дебаффа; кайт/отход после успешного сна; Prominence10с; Dominator control/CP, Shadow Step, Escape Shackle, Soul Cleanse, саммоны и олимпийский90с timeout. Прежний failure-backoff отклонённого запроса и общий reaction/decision cadence AI также сохраняются: это не дополнительный персональный cooldown Trance.

## Проверки

Локальные тесты используют реальные SkillData, native disabled-skill map и production act/poll; только доставка cast/UI записывается тестовой фикстурой. Не воспроизводится весь scheduler живого каста и реальное течение cooldown в клиентском матче; истечение native disabled timestamp задаётся в тесте. Принятый запрос не считается доказательством успешного сна.

Команды:

```bash
python tests/fakeplayersai/dominator_pressure_bluff_tests.py
python tests/fakeplayersai/cardinal_arena_parity_tests.py
python tests/fakeplayersai/native_debuff_epic_tests.py
python tests/fakeplayersai/dominator_control_shadow_tests.py
```

Результаты: все перечисленные проверки прошли. `dominator_pressure_bluff_tests.py` —1890 утверждений (включая новые Trance checks); `cardinal_arena_parity_tests.py` —6062; `native_debuff_epic_tests.py` —1626; `dominator_control_shadow_tests.py` —271540 post-sleep/summon и2465 control/CP/Shadow Step. Все custom/effect scripts компилируются. Дополнительно: `git diff --check`, неизменность исходного `libs/GameServer.jar` и полного Trance XML. К БД/живому GameServer тесты не подключаются.

## Применение и откат

1. Остановить GameServer, сохранить резерв `BotTactics.java` и XML00300–00399. Обновить именно эти два runtime-файла. Не заменять полный XML01300–01399 историческим placeholder и не сбрасывать навыки/БД/registry/overrides.
2. Полный перезапуск штатным launcher с прежними core-патчами. Пересборка JAR, новый core-патч и перезапуск LoginServer не требуются.
3. Проверить Bluff игроком и ботом всех трёх dagger-классов; Cardinal97 в обоих режимах: cast не прерывается, native reuse соблюдается, нет лишнего3с AI lock; при уже действующем сне сохраняются прежние ограничения/retreat. Сравнивать базовый reuse и фактический с учётом modifier, не ожидать ровно1/8 секунд между всеми боевыми кастами.
4. Если до restart сохранённый timestamp старого Bluff ещё действует, его остаток не сбрасывается этим патчем вручную. Новые применения используют штатный расчёт; переустановки/очистки character skills не требуется.
5. Откат: остановить сервер, вернуть два runtime-файла из базы56 и перезапустить. Это возвращает16с Bluff и отдельный3с Trance AI guard, но не затрагивает Chance/эпик и прочие обновления.
