# 69. Cardinal и Dominator: строго фиксированные20% сна

## Текущие значения

| Бот | Навык | Шанс одной допустимой попытки |
| --- | --- | --- |
| Cardinal97 | Trance1394 | **20%**, было25% |
| Dominator115 | Dreaming Spirit1097 (sleep) | **20%**, было15% |
| Dominator115 | Fear1092 | Прежние15%, не менялся |

Это bot-only политика: человеческие навыки, native XML, SkillData, БД и JAR не изменены. Уровни/заточки, эффекты, длительность, MP, cast/reuse сохранены. Правила собственного1418/пассивного5576 и Bluff из68 не менялись.

## Что исключает завышенный шанс

1. `BotControlChance`: для1394/1097 одновременно выставляются **activateRate=20, minChance=20, maxChance=20**, не только базовая активация. Навык остаётся A2/debuff с настоящим MEN, а не NONE; это исключает native ветку «гарантированный успех» для activateRate≥100/−1 или basicProperty=NONE.
2. Проверена реализация `Formulas.calcEffectSuccess` именно в `libs/GameServer.jar` (bytecode), а не только декомпилированный справочник. M.Atk/M.Def, blessed spiritshot, уровни/MEN, attribute bonus, trait бонусы/резисты (включая эффекты эпика) и DEBUFF_VULN входят **до** окончательного `Math.clamp(rate,minChance,maxChance)`. При min=max=20 они не увеличивают и не уменьшают вероятность обычного броска. Положительный traitMod даёт20; полный trait immunity даёт0.
3. Найден отдельный **legacy общий gate** `Formulas.FIXED_DEBUFF_RATE_PLAYERS`: он работает раньше min/max и использует глобальный `FIXED_DEBUFF_RATE_PERCENT` (обычно25). Штатные BotManager/BotSpawner уже удаляют старую регистрацию, но теперь каждый вызов общей scoped-политики дополнительно удаляет stale ID бота97/115. Общий процент и другие классы не изменяются. Это защитное исправление от обхода20%-границы старым gate; наличие такой регистрации в живом бою не заявляется.
4. Chance-заточка может давать исходному Trance высокую активацию, вплоть до100. Копия всегда строится из native шаблона изученного уровня и перезаписывает все три chance-поля; shared SkillData не меняется. После `BotSpawner.applySkillEnchants` scoped-политика вызывается повторно; на арене она также применяется через ArenaCombatRules/BotTactics. Копии65/25/100 прошлых версий заменяются, а не складываются с20%.
5. Проверен `Skill.applyEffects`: непрерывный A2 Sleep проходит через один вызов `Formulas.calcEffectSuccess` для общего результата применения. Native Sleep1394/1097 — обычный continuous эффект, а не дополнительный независимый гарантированный on-hit proc. Сам обработчик `handlers/skill/effects/Sleep.java` не повышает шанс и не повторяет бросок.
6. Native случайный результат — `Rnd.get(100)`, диапазон0..99; успех при20>roll, то есть ровно20 исходов из100. Повторные легальные касты остаются отдельными попытками:20% **на каст**, не лимит20% успешных боёв и не гарантированно ровно один успех на каждые пять применений.

Полная неуязвимость, DEBUFF_IMMUNITY/trait immunity, недопустимая цель и native запреты не обходятся ради20%. Повторное наложение/обновление уже действующего сна также не разрешается поверх существующего debuff-category guard.

## Карта файлов

| Изменённый runtime-файл | Ответственность |
| --- | --- |
| `game/data/scripts/custom/fakeplayersai/BotControlChance.java` |20/20/20 для1394/1097,15% для1092; удаление legacy global-gate регистрации97/115 при apply; кэш/копирование native шаблона без записиБД |
| `game/data/scripts/custom/fakeplayersai/CardinalTrance.java` | Совместимая точка входа spawn/enchant/arena; комментарий актуальной политики |
| `game/data/scripts/custom/fakeplayersai/ArenaCombatRules.java` | Общий arena-вызов политики, комментарий20/15% |
| `game/data/scripts/custom/fakeplayersai/BotPresets.java` | Исправлен устаревший комментарий Trance15% на20%; ротации не менялись |

Проверенные, не изменённые точки: `BotSpawner.applySkillEnchants`, `BotManager.updateTankDebuffRegistration`, core `Formulas.calcEffectSuccess`, `Skill.applyEffects`, `handlers/skill/effects/Sleep.java`, native XML1394/1097 и базовые/enchant шаблоны. Не добавлялся второй бросок, сторонний timer или новый core-патч.

## Проверка

Запуск: `python tests/fakeplayersai/passive_barrier_bluff_tests.py`.

- `CardinalSurvivalControlChanceTest.java`: поля20/20/20 для обоих sleep и15/15/15 для Fear; native уровни/Chance130/230, сохранность всех остальных свойств/эффектов, человеческие персонажи/другие классы не меняются; DEBUFF_IMMUNITY/trait immunity остаются0.
- Дополнительный `fixedSleepAudit`: обе арены, human/bot противники, native/Chance уровни; по8000 настоящих native Formula бросков на профиль. Нормальный профиль, экстремальные M.Atk/низкая M.Def/положительный sleep-trait/DEBUFF_VULN и обратный профиль слабой магии/высокой защиты/резистов все остаются около20%, а не растут от бонусов. Это статистическая регрессия, не подмена RNG игрового сервера.
- В тесте общий legacy rate намеренно ставится100 и ID бота добавляется в legacy set. Scoped apply удаляет только ID; затем настоящие броски всё равно дают около20%. Общая100-настройка в тесте остаётся неизменной и восстанавливается вfinally. Производственный код глобальный процент не меняет.
- Старые частные шаблоны25/65/100 заменяются на одну правильную20%-копию. `NativeDebuffEpicTest.java` проверяет полный enchant/migration путь и прежний эпик; `ArenaOrbitTranceManaTest.java` — исправление legacy Trance/уровни/идемпотентность.
- Остальные четыре набора сохраняют покрытие Celestial, Trance/kite/Clarity, Bluff/спины/без автоатаки, танков и боевых приоритетов. Runner компилирует все custom/effect/target scripts против временной копии поставляемого JAR с прежними debuff/boundary patches.

Итоговый прогон:1197/6912/26353/1626/780/7615/61 проверок в семи Java-наборах — PASS; все custom/effect/target scripts скомпилированы; связанные wiring assertions59/60/56 — PASS.

Изменённые runner/expectations: `passive_barrier_bluff_tests.py`, `native_debuff_epic_tests.py`. Тесты не являются живым боем с клиентом/БД; эпик/соски дополнительно проверены по порядку операций в настоящем core bytecode, вероятность конкретной живой серии кастов не измерялась.

## Установка и откат

Обновить четыре runtime Java-файла из таблицы и полностью перезапустить GameServer. Новый JAR, XML, конфиг, БД или core-патч не нужен. Существующая цепочка шести core-патчей66 сохраняется. GitHub-коммит сам по себе не меняет работающий сервер.

Резервная ветка: `backup/before-fixed-sleep20-2026-10-20` (1a055a0). Для отката вернуть четыре runtime-файла и перезапустить GameServer; это вернёт прежние25% Trance/15% Dreaming Spirit.
