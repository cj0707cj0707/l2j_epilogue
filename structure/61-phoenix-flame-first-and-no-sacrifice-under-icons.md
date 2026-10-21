> **Уточнение90:** фактический5562/3 делает готовый Flame Icon785 [срочным](90-tank-stage3-immediate-class-finisher.md): он обходит обычную задержку AI и остаётся выше406/69. HP-cost/reuse/условия сохранены. То же правило добавлено для5561/3→762 и5564/3→789.

# 61. Phoenix Knight: Flame Icon первым, Sacrifice запрещён под иконами

База: `e68a34cfaf98a4eccbfc87b5bab25b49db027c4c` (60). Изменены скрипты репозитория, не работающий GameServer. Обе арены: Олимпиада и тренировка. Остальные танки, кайт110/368/760, правила Cardinal/Dominator,150с и экипировка не меняются.

## Приоритет икон

1. **Spirit of Phoenix784 → настоящая третья стадия5562 → Flame Icon785** при первой законной возможности. Готовый785 выбирается перед406 и перед Sacrifice69, в том числе приHP≤30%.
2. Пока Flame Icon действует, Angelic Icon не заменяет его. Обе иконы относятся к native POSSESSION; AI не пытается их сложить или обновить.
3. **Angelic Icon406 — резерв приHP≤30%** после окончания Flame Icon либо аварийный вариант, если785 сейчас недоступен (нет третьей стадии, откат, не хватаетHP на стоимость и т.п.). Бот не ждёт зарядов ценой гибели.

Flame Icon по-прежнему требует живую5562/3, имеет native HP-cost1253 и reuse150000мс. Angelic Icon сохраняет native HP-порог, MP/reuse. Готовность определяется среди полностью законных кандидатов общего движка, а не по одному ID/наличиюзаряда. Skill.checkCondition, ресурсы, CC/silence, цели, геоданные, действующие эффекты и backoff не обходятся. HP не снижается искусственно, третья стадия не выдаётся скриптом; seed не обновляется поверх своих зарядов. Сильный защитный эффект противника по60 по-прежнему включает обязательный кайт раньше атакующей ротации.

## Запрет Sacrifice69

Только бот Phoenix Knight90: пока **фактически действует** Flame Icon785 или Angelic Icon406, Sacrifice69 не используется для самолечения. Проверяются BuffInfo, `isInUse` и отсутствие удаления; собственного таймера запрета нет.

Три уровня защиты:
- Фильтр `BotRecoveryRules.selfHealAllowed`:69 не попадает в законные кандидаты AI.
- Синхронный `ON_CREATURE_SKILL_USE` guard: прежние AutoUse/legacy-пути также не могут начать69 под иконой.
- Native обработчик `Heal.onStart` проверяет тот же узкий запрет: если икона появилась после допуска каста,69 не восстановитHP при выполнении эффекта.

После снятия/окончания иконы разрешение69 возвращается автоматически на ближайшем решении, с прежним правиломHP<70%. Под иконами не меняется длительность/сила самих икон и не сбрасывается откат. Другие лечащие навыки и лечение других профессий этим изменением не запрещаются. Человеческий Phoenix/Sacrifice не изменяются. Уже начатые касты не прерываются; execution guard не возвращает уже израсходованные ресурсы.

## Карта файлов

| Файл | Ответственность после61 |
| --- | --- |
| `game/data/scripts/custom/fakeplayersai/TankClassMechanics.java` | Порядок785 перед406; живые406/785 для запрета bot-Phoenix Sacrifice; native условия60 сохранены |
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | Законные785/406 выше обычного восстановления69;785 выше406 задаётся общим выбором механики |
| `game/data/scripts/custom/fakeplayersai/BotRecoveryRules.java` | Узкий запрет69 под иконами в выборе и синхронном cast guard; публичный execution guard только для managed bot self-Sacrifice |
| `game/data/scripts/handlers/skill/effects/Heal.java` | Повторная проверка запрета непосредственно перед восстановлениемHP; другие native healing правила сохранены |
| `tests/fakeplayersai/TankMechanicsKiteTest.java` | Расширение60:785 раньше69/406 при разныхHP, fallback406, запрет69 вactual act/event/Heal, снятие/неактивность эффекта, игроки вне запрета |
| `tests/fakeplayersai/tank_mechanics_kite_tests.py` | Компиляция полного custom/effect набора и source-guards обновлённого приоритета/двух уровней heal-вызова |

## Проверки и установка

```sh
JAVA_TOOL_OPTIONS=-Xmx512m python3 tests/fakeplayersai/tank_mechanics_kite_tests.py
JAVA_TOOL_OPTIONS=-Xmx512m python3 tests/fakeplayersai/combat_recovery_rules_tests.py
JAVA_TOOL_OPTIONS=-Xmx512m python3 tests/fakeplayersai/cardinal_survival_control_chance_tests.py
JAVA_TOOL_OPTIONS=-Xmx512m python3 tests/fakeplayersai/native_debuff_epic_tests.py
JAVA_TOOL_OPTIONS=-Xmx512m python3 tests/fakeplayersai/dominator_pressure_bluff_tests.py
git diff --check
```

Результаты: **7615** проверок общего кайта/танковых механик/новой политики Phoenix; **1126** recovery; **6583** Cardinal/Chance/Clarity; **1626** native skills/эпик; **1890** Dominator/Bluff. Все проверки выше прошли; полный custom/effect набор скомпилирован, `git diff --check` без ошибок.

Тесты используют реальныйSkillData/checkCondition/Skill.applyEffects/BuffInfo и production act/event/Heal; запись движения/запросов каста и EffectList заменяют сеть/планирование. Живой бой, клиент, БД и случайный набор стадий от серии ударов не тестировались. Skill XML, исходныйJAR и core-патчи не меняются.

Установка: остановить GameServer; перенести согласованно **три изменённых fakeplayersai Java-файла и `handlers/skill/effects/Heal.java`**; полностью перезапустить стандартным launcher. Новых runtime-файлов по сравнению с60 нет. Проверить785 при5562/3,406 при недоступном785/низкомHP, отсутствие69 под каждой иконой и возобновление69 после её окончания. Откат: при остановленном сервере вернуть эти четыре файла из60 и перезапустить. МиграцияБД и правка конфигурации не требуются.
