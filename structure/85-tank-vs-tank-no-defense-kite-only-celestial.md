# 85. Танк vs танк: отмена кайта по 110/368/760, кайт только при Celestial

> **Актуализация60:** Предыдущий doc60 установил кайт **всех 19 классов** при BuffInfo 110/368/760 на противнике-танке. Это правило отменяется только для matchup **танк vs танк**: бот-танк кайтит бота/игрока-танка исключительно при `target.isInvul()` (Celestial Shield, скиллы 5575/5576/1418). UD/Vengeance/AMA на противнике-танке кайт бота-танка **не вызывают**. Для всех прочих классов (не танков) поведение doc60 сохранено без изменений.

База: `db2b48de9c2a05bbe650492cde8488680b94645c` (84). Изменения в скриптах репозитория; на работающем GameServer не устанавливались.

## Мотивация

В зеркальном бою танк vs танк кайт при 110/368/760 делает поединок бессодержательным: оба танка постоянно отступают от защиты друг друга и ничего не делают. Танки должны давить друг на друга даже под UD/Vengeance/AMA — так же, как это делает живой игрок. Отступить имеет смысл только от реальной неуязвимости (Celestial).

## Новое правило

| Self | Target | BuffInfo 110/368/760 на target | `target.isInvul()` | Кайт? |
|---|---|---|---|---|
| **Танк** | **Танк** | да | нет | ❌ **НЕТ** — продолжать бой |
| **Танк** | **Танк** | нет | да | ✅ ДА — кайт (doc84) |
| **Танк** | **Танк** | да | да | ✅ ДА — кайт (doc84, invul приоритетнее) |
| Не-танк | Танк | да | нет | ✅ ДА — прежнее поведение doc60 |
| Не-танк | Танк | нет | да | ✅ ДА — кайт (doc84) |
| Любой | Не-танк | — | — | Без изменений |

## Определение «бот является танком»

Применяется тот же реестр классов, что и в `TankClassMechanics.java` (doc60):

```java
// TankClassMechanics.java — уже существует
public static boolean isTankClass(int classId) {
    // Phoenix Knight 90, Hell Knight 91, Eva's Templar 99 (нет пресета),
    // Shillien Templar 106 — все родные ветки Paladin/DA/TK/SK + третьи профессии
    return TANK_CLASS_IDS.contains(classId);
}
```

`isTankClass` уже используется в doc60 для определения противника-танка. Теперь добавляется симметричная проверка **себя**.

## Изменение в `BotTactics.java`

### До (doc60):
```java
// Кайт при защите танка — для всех 19 классов
private static boolean shouldKiteDefendedTank(BotAI bot, L2Character target) {
    if (!TankClassMechanics.isTankClass(target.getTemplate().getClassId().getId())) {
        return false;
    }
    return target.isAffectedBySkill(110) // Ultimate Defense
        || target.isAffectedBySkill(368) // Vengeance
        || target.isAffectedBySkill(760); // Anti Magic Armor
}
```

### После (doc85):
```java
// Кайт при защите танка — с исключением для зеркала танк vs танк
private static boolean shouldKiteDefendedTank(BotAI bot, L2Character target) {
    if (!TankClassMechanics.isTankClass(target.getTemplate().getClassId().getId())) {
        return false; // target не танк — правило не применяется
    }
    // Если сам бот — тоже танк: кайтить только при invul; 110/368/760 кайт не вызывают
    int selfClassId = bot.getActiveChar().getTemplate().getClassId().getId();
    if (TankClassMechanics.isTankClass(selfClassId)) {
        return false; // invul обрабатывается хуком 0 (doc84)
    }
    // Бот не-танк vs танк: прежнее поведение doc60
    return target.isAffectedBySkill(110)
        || target.isAffectedBySkill(368)
        || target.isAffectedBySkill(760);
}
```

### Взаимодействие с doc84 (`handleInvulnerableTarget`)

`handleInvulnerableTarget` стоит хуком 0 и выполняется **до** `shouldKiteDefendedTank`. Порядок хуков не меняется:

```
0. handleInvulnerableTarget   ← при isInvul() → кайт (работает для всех классов, включая танков)
1. escapeShackle(453)
2. soulCleanse(1510)
3. bluffRear
4. cardinalSurvival
5. tickMageCancel(1056)
6. rootedByTank
7. tickInvulnerableMelee      ← shouldKiteDefendedTank вызывается здесь
8. normalAct
```

Таким образом: при `target.isInvul()` хук 0 перехватывает управление раньше, и `shouldKiteDefendedTank` (хук 7) до вызова не доходит. Поведение при Celestial не меняется. При 110/368/760 без Celestial хук 0 не срабатывает → хук 7 вызывается → новый guard возвращает `false` для бота-танка → атака продолжается.

## Поведение бота-танка против танка с активной защитой

При наличии UD/Vengeance/AMA на противнике-танке бот-танк:

1. **Продолжает классовую ротацию** (doc32, doc29, doc34): применяет собственный пул защиты (Vengeance368 первым, затем UD110), использует Rush Impact793 при стане, стационарную ротацию при руте (doc81).
2. **Не отступает** и не делает shorten-шагов от doc60.
3. **Работает хук 1 (escapeShackle)** — срочный root-снос сохраняется.
4. **Cyclic Rush продолжается** при наличии своего стана (doc31).
5. При появлении `target.isInvul()` во время боя — хук 0 немедленно переключает на кайт/ожидание (doc84).

## Карта файлов

| Файл | Изменение |
|---|---|
| `BotTactics.java` | `shouldKiteDefendedTank`: добавлен self-tank guard — возвращает `false` если `TankClassMechanics.isTankClass(selfClassId)` |
| `TankClassMechanics.java` | Без изменений; `isTankClass()` уже существует |
| `InvulnerableMeleeKite.java` | Без изменений; алгоритм не менялся |
| `TankDefenseSequence.java` | Без изменений; очередь 110/368/760 своей защиты сохранена |
| `TacticsPolicy.java` | Без изменений |
| `tests/fakeplayersai/TankVsTankNoDefenseKiteTest.java` (новый) | Матчапы tank-vs-tank: 110/368/760 → атака продолжается; isInvul → кайт; non-tank vs tank → кайт сохранён |
| `tests/fakeplayersai/tank_vs_tank_kite_tests.py` (новый) | Python-обёртка Java-теста |

## Тест-сценарии

```java
// Сценарий 1: Танк vs танк, target под UD110 — атака должна продолжаться
assertFalse(shouldKiteDefendedTank(phoenixKnight90, targetWithUD110));

// Сценарий 2: Танк vs танк, target под Vengeance368 — атака должна продолжаться
assertFalse(shouldKiteDefendedTank(hellKnight91, targetWithVengeance368));

// Сценарий 3: Танк vs танк, target под AMA760 — атака должна продолжаться
assertFalse(shouldKiteDefendedTank(phoenixKnight90, targetWithAMA760));

// Сценарий 4: Танк vs танк, target.isInvul() — обрабатывается хуком 0,
// shouldKiteDefendedTank до вызова не доходит (проверяется косвенно через act())

// Сценарий 5: Не-танк (Cardinal97) vs танк, target под UD110 — кайт должен быть
assertTrue(shouldKiteDefendedTank(cardinal97, targetWithUD110));

// Сценарий 6: Не-танк (Archmage94) vs танк, target под Vengeance368 — кайт должен быть
assertTrue(shouldKiteDefendedTank(archmage94, targetWithVengeance368));

// Сценарий 7: Танк vs не-танк — правило не применяется (target не танк)
assertFalse(shouldKiteDefendedTank(phoenixKnight90, targetCardinal97));
```

```bash
python3 tests/fakeplayersai/tank_vs_tank_kite_tests.py
```

Ожидаемый результат: **≥ 280 assertions**, включая все комбинации (90/91/106 vs 90/91/106/99), три буффа защиты, invul-сценарии через хук 0, и не-танки (94/96/97/115 и др.) vs танк.

## Регрессии

Повторно запустить после установки:

```bash
python3 tests/fakeplayersai/tank_mechanics_kite_tests.py          # doc60: 7515 assertions
python3 tests/fakeplayersai/combat_recovery_rules_tests.py        # doc40: 1126
python3 tests/fakeplayersai/invulnerable_target_tests.py          # doc84: 520
python3 tests/fakeplayersai/dominator_pressure_bluff_tests.py     # doc53: 1890
python3 tests/fakeplayersai/cardinal_arena_parity_tests.py        # doc37: 6062
```

`tank_mechanics_kite_tests.py` затрагивает кайт танков — проверить, что ни один existing тест не ожидает кайта бота-танка при 110/368/760. Если такие тесты есть — обновить assertions согласно новому правилу.

## Установка

1. Остановить GameServer; сохранить `BotTactics.java`.
2. Применить изменение `shouldKiteDefendedTank` в `BotTactics.java`.
3. Скопировать новые тестовые файлы в `tests/fakeplayersai/`.
4. Перезапустить GameServer.
5. Проверить в бою: Phoenix Knight90 vs Phoenix Knight90 — при наличии UD110 на противнике бот атакует, не отступает; при Celestial — кайт.
6. Проверить: Cardinal97 vs Phoenix Knight90 с UD — Cardinal по-прежнему кайтит (поведение doc60 сохранено).

**Откат:** при остановленном GameServer вернуть `BotTactics.java` из резервной ветки, перезапустить.

Резервная ветка: `backup/before-tank-vs-tank-no-defense-kite-2026-10-21`.
