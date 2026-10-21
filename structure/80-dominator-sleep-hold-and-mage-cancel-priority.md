# 80. Dominator: отход и ожидание сна; Archmage/Mystic Muse: приоритет Cancel

> **Актуализация87:** [Повторный1097 после пробуждения танка теперь реализован и в rooted-фазе](87-invulnerable-ranged-root-priorities-and-doc-audit.md): реально изученный готовый сон не зависит от старого rotation override и generic nextDecision/reaction. Сохраняются20% успеха,MP/reuse/LOS/range/silence/CC, pending и HOLD спящей цели. При недоступном сне продолжаются допустимые действия с места.


> **Актуализация81:** [Root от танка временно прерывает кайт и включает обычную ротацию с места; после снятия root кайт возвращается только при необходимости](81-tank-root-stationary-rotation.md). Native дальность/LOS/MP/reuse/CC, запреты движения и HOLD спящей цели сохранены.

База: `d91f637422f8bb8dfb0245a4db959a793946035d` (79). Резервная ветка: `backup/before-dominator-sleep-hold-mage-cancel-2026-10-20`.

## 1. Dominator против танка под усиленной защитой

**Живой активный BuffInfo110/368/760** на танке одной из четырёх веток, включая Eva's Templar99.

Текущий цикл:

1. **Цель бодрствует:** прежний защитный кайт + законно доступный Dreaming Spirit1097 + доступная поддержка/CP. При готовом сне останавливается старое движение и выполняется обычный native cast-запрос, как в79.
2. **Сон действительно подействовал:** свой Sleep BuffInfo запускает один отход ~300, без телепорта.
3. **После отхода — HOLD до фактического пробуждения.** Нет кругового кайта, автоатаки. Доступная self-поддержка/CP может применяться на месте.
4. **Цель проснулась или сон снят:** если защита танка ещё действует — снова кайт/сон/CP. **Если Dominator зарутован в момент пробуждения** — правило81: стационарная ротация с места, включая немедленную попытку1097 (без отхода); отход ~300 возобновляется только после снятия рута.
5. **Защитные усиления закончились:** управление возвращается обычной ротации.

Срочные Soul Cleanse/Escape Shackle остаются выше этой фазы. Шанс1097 — **20/20/20**.

## 2. Archmage94 и Mystic Muse103: готовый Cancel первым

**Механика Cancel (конфликт5 — разъяснение):** Cancellation1056 применяется **на противника** с целью снятия его **положительных баффов** (`slot=buff`). Это наступательный навык, а не самоочищение. Trance1394 — это **дебафф** (CC), наложенный Cardinal на противника. Он не является положительным баффом противника, поэтому **Cancel не может снять Trance**. Цикл «Cancel снял Trance → Cardinal перекладывает → Cancel снова» невозможен по механике — навыки воздействуют на разные категории эффектов.

Для clientless-ботов Archmage94 и Mystic Muse103 реально изученный **Cancellation1056** получает приоритет:

- против мили-противников (включая танков, кинжальщиков, Eva's Templar) и **дополнительно против Cardinal97**;
- при каждом восстановлении native-готовности;
- перед обычными nukes/debuffs и следующим шагом кайта.

Сама Cancellation не изменялась: родной XML, range600/reuse30000ms, эффект `DispelByCategory(slot=buff, rate=25, max=5)`. Тактическая цель Cancel — лишить противника реальных баффов (усилений, защитных эффектов). Дебаффы на противнике (Trance, stun, fear) через Cancel не снимаются.

Добавлен обязательный публичный `BotTactics.tickMageCancel`, вызываемый после Bluff/Cardinal hooks и до общего kite-hook в обоих драйверах `BotManager` и в `BotTactics.tick` тренировки.

## Карта файлов

| Файл | Изменение |
| --- | --- |
| `BotTactics.java` | `State.tankSleepRetreat`, `dominatorTankSleepHold`; при root при пробуждении — стационарная ротация81; `tickMageCancel` для Cancel на противнике. |
| `SleepRetreat.java` | `clear(Player)` останавливает только принадлежащий helper маршрут. |
| `BotManager.java` | Два обязательных вызова `tickMageCancel` до универсального кайта. |
| `01000-01099.xml`, `DispelByCategory.java` | Без изменения. Cancel снимает `slot=buff` противника, Trance (slot=debuff) не затрагивает. |
| `DominatorHoldMageCancel80Test.java` | Public hooks, цикл sleep-run-hold-wake; оба мага против 19 пресетов+Eva. |

## Проверки

```bash
python tests/fakeplayersai/passive_barrier_bluff_tests.py --hold-cancel-only
python tests/fakeplayersai/passive_barrier_bluff_tests.py
```

Полный прогон: **16 наборов**, `DominatorHoldMageCancel80Test` — **3206 assertions**.

## Применение / откат

При остановленном GameServer заменить **вместе** `BotTactics.java`, `BotManager.java`, `SleepRetreat.java`. Проверка: танк с110/368/760 → сон → отход/HOLD без атаки → пробуждение. У архмагов/Mystic Muse против мили и Cardinal — Cancel1056 опережает nuke/кайт (снимает их баффы, Trance не снимает).
