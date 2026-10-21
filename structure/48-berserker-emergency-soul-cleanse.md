# 48. Berserker: Soul Cleanse при root или физическом молчании

Уточнено аудитом87: прежний текст ошибочно называл1510 мгновенным root-only навыком, ссылался на условный commit, отсутствующие `BerserkerAI.java`, `berserker_tests.py` и412 assertions. Эти утверждения удалены; баланс навыка в87 не менялся.

## Фактическое поведение

Для clientless Berserker/Doombringer131 в разрешённой аренной паре реально изученный **Soul Cleanse1510** проверяется при `isRooted() || isPhysicalMuted()`, независимо отHP. Это targeted **магический** навык `TargetType.ONE`: бот временно выбирает себя, делает обычный `useMagic(skill,true,true)`, сохраняет self-target на время pending и затем восстанавливает противника.

Триггер означает приоритет проверки, а не гарантированное снятие любого контроля. Настоящий/одновременный каст, чужой pending, out-of-control и all-skills lock не прерываются. Сохраняются native reuse, magic silence,MP, target/race/checkCondition, Olympiad restrictions и failure backoff. Root/одно физическое молчание сами по себе не запрещают магический1510. Неизученный навык не выдаётся.

В `act` проверка идёт после `escapeShackle453` и до Bluff-движения/обычной ротации; классы131 и93/101/108 не пересекаются. В ranked Berserker-драйвере имеется отдельный обязательный `tickSoulCleanse`, чтобы фазовый Rush/retreat не ретаргетировал ONE-cast. В фазе неуязвимого дальника87 проверка1510 предшествует движению. Фактические уровни вызовов — [83](83-mandatory-hooks-order.md).

## Native параметры — без изменения

`game/data/stats/skills/01500-01599.xml`, skill1510: **2000мс cast,15000мс reuse,25MP +7 initialMP**, magic/ONE. Эффект снимает подходящие дебаффы по родным правилам; ИИ не удаляет эффекты вручную, не сбрасывает reuse и не выдаётMP. Повторный root не даёт права обойти откат; повтор возможен после штатной готовности.

## Карта файлов

| Файл | Ответственность |
|---|---|
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | `soulCleanse`, публичный `tickSoulCleanse`, self-target lease/pending, native admission/failure |
| `game/data/scripts/custom/fakeplayersai/BotManager.java` | Вызывает обязательные аренные фазы и `tickSoulCleanse` в Berserker-драйвере |
| `game/data/stats/skills/01500-01599.xml` | Параметры/эффекты1510; в87 не изменены |
| `tests/fakeplayersai/BerserkerSoulCleanseTest.java` | Настоящие Player/Skill и helper/act/pending с записью cast-запросов |
| `tests/fakeplayersai/berserker_soul_cleanse_tests.py` | Компилирует скрипты и запускает фикстуру |

## Проверки

```bash
python tests/fakeplayersai/berserker_soul_cleanse_tests.py
python tests/fakeplayersai/invulnerable_target_tests.py
python tests/fakeplayersai/passive_barrier_bluff_tests.py
```

Это изолированные запросы/условия, не подтверждение live cast/scheduler или снятия дебаффа на работающем GameServer. Текущий прогон87 приведён в87; старые вымышленные цифры не используются.

## Установка

Для текущих изменений переносить весь runtime-набор87 согласованно при остановленном GameServer, затем полный перезапуск. GitHub не обновляет автоматически установленный сервер. Отдельного `BerserkerAI.java` для копирования нет; JAR иXML в87 не менялись.
