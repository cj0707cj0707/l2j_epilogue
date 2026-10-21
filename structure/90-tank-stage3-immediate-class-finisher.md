# 90. Танки: немедленный классовый навык при третьей стадии

База: `27417502` (89). Изменения89 Dominator и прежние правила танков60/61/86/88 сохранены. Это описание текущего кода, а не проверка на запущенном сервере.

## Поведение

Для clientless-ботов Phoenix Knight90, Hell Knight91 и Shillien Templar106 в активной Олимпиаде и тренировочной арене используется только фактическая нативная третья стадия:

| Класс | Подготовка | Живая стадия3 | Срочный классовый навык |
|---|---|---|---|
| Phoenix Knight90 | Spirit of Phoenix784 | Spirit of Phoenix5562/3 | Flame Icon785 |
| Hell Knight91 | Seed of Revenge761 | Seed of Revenge5561/3 | Insane Crusher762 |
| Shillien Templar106 | Pain of Shilen788 | Pain of Shilen5564/3 | Spirit of Shilen789 |

Как только существует текущий `BuffInfo` третьего уровня и соответствующий изученный finisher законно готов, обычные `nextDecision` и reaction-delay больше не задерживают расход окна. Finisher выбирается выше лечения, защиты и обычной ротации. Это формализует «незамедлительно» на следующем частом AI-tick, но не прерывает уже начатый настоящий каст.

Нативные ограничения сохраняются:

- 785/789 требуют родную стадию3, достаточный HP для стоимости1253, reuse и корректный PARTY target list; противник не получает групповой баф;
- 762 остаётся атакующей AURA радиусом200, а не self-buff: нужны родная стадия3, SWORD/BLUNT, MP, цель в радиусе и остальные условия;
- stun/silence/другой CC, disabled skill, invulnerability цели для атаки, LOS/range и engine `checkCondition` не обходятся;
- стадии1/2 не срочные; AI не создаёт, не повышает и не удаляет заряды вручную — proc, срок эффекта и фактическое потребление принадлежат нативному движку;
- если finisher сейчас незаконен, AI не подменяет его бесплатным кастом. Для Phoenix сохраняется законный аварийный Angelic Icon406 при низком HP, когда Flame Icon нельзя оплатить.

Приоритет относится только к трём перечисленным цепочкам. Eva's Templar99 по-прежнему не имеет bot-пресета; native786→5563→787 не изменена.

## Изменённые файлы

| Файл | Назначение изменения |
|---|---|
| `game/data/scripts/custom/fakeplayersai/TankClassMechanics.java` | Определяет готовый native finisher по классу, живой stage3, reuse и `checkCondition` |
| `game/data/scripts/custom/fakeplayersai/BotTactics.java` | Обходит обычную задержку решения при готовом finisher и выбирает его первым |
| `tests/fakeplayersai/TankMechanicsKiteTest.java` | Для всех трёх классов устанавливает настоящий stage3, блокирует обычный decision clock и подтверждает немедленный785/762/789; reuse остаётся обязательным |
| `tests/fakeplayersai/tank_mechanics_kite_tests.py` | Проверяет source-wiring приоритета и компилирует custom/effect scripts |

Skill XML, JAR, SQL, proc chance, длительность зарядов и конфигурация не менялись.

## Проверки

```bash
python tests/fakeplayersai/tank_mechanics_kite_tests.py
```

Результат: **7771 проверка PASS**, все custom/effect scripts компилируются на Java25. Тесты используют настоящие SkillData, effects, conditions, weapon и disabled-skill state; cast/movement/UI/БД — recording seams. Это не live-проверка планировщика, geodata или пинга.

## Установка и откат

Остановить GameServer, согласованно обновить `TankClassMechanics.java` и `BotTactics.java`, затем штатно запустить GameServer. LoginServer, JAR, SQL и skill XML менять не нужно. Изменение в GitHub само по себе не установлено на работающий сервер.

Для отката вернуть эти два файла из базы89 и перезапустить GameServer. Резервная ветка: `backup/before-tank-charge-finisher-90-2026-10-21`, указывает на базу89.
