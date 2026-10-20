# 76. Cardinal: готовый Prominence при HP строго выше90%

## Поведение

Для arena-бота Cardinal97 (олимпиада и тренировка, против игроков и ботов) добавлено явное исключение из приоритета Trance75:

1. Если **HP Cardinal >90%**, противник **не спит и не неуязвим**, изученный Prominence1230 доступен по native условиям и10-секундному AI-интервалу — применить **Prominence первым**, до готового Trance, лечения, wake-комбо и кайта.
2. **Ровно90% и ниже — Prominence запрещён.** Сравнение использует реальные текущие/максимальныеHP без округления отображаемого процента.
3. **Clarity3282 на цели не запрещает Prominence.** Больше не требуется предшествующий успешный Trance или фаза wake-Prominence. Правило действует против всех профессий, в том числе Sagittarius.
4. Интервал между принятыми запросами Prominence остаётся **10000мс**, как раньше в `TacticsPolicy.CardinalProminence`:9999мс ещё рано,10000мс уже допустимо. Реальный native reuse также проверяется, а не заменяется AI-таймером. Отказ запроса не расходует10-секундный интервал; обычный failure-backoff сохраняется.
5. При недоступном Prominence действует прежний приоритет Trance75 и законный fallback. Для готового Trance не требуется ждать отката Prominence. При сне/неуязвимости цели запрещён и сам новый Prominence-запрос; запрет сна от74 сохранён.

Правило не обходит настоящий уже идущий каст, pending, silence/CC, MP/HP/ресурсы, native условия, range/LOS и arena-authority. Изученный1230 добавляется в кандидаты даже при сохранённой ротации без этого ID. Готовый нук останавливает кайт, но не прерывает уже начатое заклинание. Под собственным/пассивным Celestial действует тот же приоритет приHP>90%, без кайта; native отмена собственного1418 при действии по-прежнему не изменена.

## Карта изменённых runtime-файлов

| Файл | Ответственность |
| --- | --- |
| `game/data/scripts/custom/fakeplayersai/CardinalSurvival.java` | Общий `prominenceAllowed`: для бота97 строго HP>90%, бодрствующая уязвимая цель; Clarity не учитывается. Игроки и другие классы этим новым ограничением не затрагиваются |
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | `cardinalProminenceReady`: native доступность,10с, ресурсы, range/LOS; принудительный learned-кандидат1230, выбор раньше1394/лечения/wake-комбо, остановка кайта. Снята привязка1230 к старому wake-окну Clarity; общий HP/target-фильтр запрещает обход через обычный scoring |
| `game/data/scripts/custom/fakeplayersai/BotSpawner.java` | Shared candidate guard не разрешает1230 приHP<=90%, сне или invul цели |
| `game/data/scripts/custom/fakeplayersai/BotRecoveryRules.java` | Синхронный native `OnCreatureSkillUse` guard повторяет HP/target-запрет для управляемого arena-бота97, защищая от альтернативного cast-пути и изменения состояния между решением и запросом |

`TacticsPolicy.CardinalProminence` и обработка `accepted(time)` не изменялись:10-секундный интервал сохраняется, не сокращается до native XML reuse. XML1230, урон, cast time, native расход и шанс не менялись. Изменений ядра/JAR/БД нет.

Также не менялись: лимит олимпийского боя210с, запрет кайта Cardinal под обоими Celestial, фиксированные20% сна Cardinal/Dominator,40% Bluff-stun,12с reuse Bluff, rear-only атаки кинжальщиков, экипировка и локальный IP.

## Тесты

```bash
python tests/fakeplayersai/passive_barrier_bluff_tests.py --prominence-only
python tests/fakeplayersai/passive_barrier_bluff_tests.py
```

- Новый `tests/fakeplayersai/CardinalProminencePriorityTest.java`: матрица всех19 профессий пула, обе арены, human/bot, Clarity/no-Clarity, без щита/1418/5576/оба;HP91%,90%,90.01%; override без1230; Prominence раньше ready-Trance/хила, затем1394 в10с-окне; pending cast, точные границы9999/10000мс, reuse/silence, invul/sleep, shared/native admission guard и wake-приоритет.
- Старые sleep/kite/wake тесты обновлены **только там, где прежний приоритет противоречит76**: в сценариях, проверяющих именно Trance или Mana Burn,1230 явно стоит на native reuse; перед проверкой fallback-Prominence снимается эта блокировка. Матрица низкогоHP из75 продолжает проверять Trance; healthy wake теперь проверяет новый1230-приоритет.
- `passive_barrier_bluff_tests.py`: отдельный `--prominence-only`, общий compile всех custom/effect/target Java-скриптов и12 боевых регрессионных наборов.

Общий прогон всех12 наборов завершён успешно:1327 /6942 /26353 /1628 /4068 /7615 /61 /8382 /8422 /271534 /16264 /9772 проверок соответственно. Новый набор76 дал9772 проверки; все custom/effect/target Java-скрипты скомпилированы. Число native random-roll проверок Bluff может немного меняться между запусками при неизменных критериях вероятности. Проверки изолированные: используются настоящие SkillData/Formulas/effects и AI-код, но движение/касты/сеть записываются вместо исполнения в живом мире. GameServer пользователя с клиентом/БД не запускался.

## Установка и откат

Обновить **все четыре runtime Java-файла** из таблицы с сохранением путей, полностью перезапустить GameServer. Перезапуск одних ботов не заменяет перекомпиляцию скриптов. Предыдущие согласованные изменения75 должны быть установлены. Публикация в GitHub сама не копирует файлы в `C:\EpilogueServer`.

Резервная ветка перед76: `backup/before-cardinal-healthy-prominence-priority-2026-10-20`, исходный коммит `c2c984e6f1626a967069594a4a4aead2d45db570`. Для отката вернуть четыре runtime-файла вместе и перезапустить GameServer.
