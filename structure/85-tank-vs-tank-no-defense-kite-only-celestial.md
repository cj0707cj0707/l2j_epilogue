# 85. Танк vs танк: отмена кайта по 110/368/760, кайт только при Celestial

> **Актуализация60:** Предыдущий doc60 установил кайт **всех 19 классов** при BuffInfo 110/368/760 на противнике-танке. Это правило отменяется только для matchup **танк vs танк**: бот-танк кайтит бота/игрока-танка исключительно при `target.isInvul()` (Celestial Shield, скиллы 5575/5576/1418). UD/Vengeance/AMA на противнике-танке кайт бота-танка **не вызывают**. Для всех прочих классов (не танков) поведение doc60 сохранено без изменений.

База: `e90b9a5ce4f5257d2bc698afb0573040928847f2` (BotTactics.java). Изменения в скриптах репозитория; на работающем GameServer не устанавливались.

## Мотивация

В зеркальном бою танк vs танк кайт при 110/368/760 делает поединок бессодержательным: оба танка постоянно отступают от защиты друг друга. Танки должны давить друг на друга даже под UD/Vengeance/AMA. Отступить имеет смысл только от реальной неуязвимости (Celestial).

## Где находится реальная точка кайта

Метод `shouldKiteDefendedTank` в коде **не существует**. Кайт управляется в `tickInvulnerableMelee` через переменную `required`:

```java
// BotTactics.java — tickInvulnerableMelee (ДО правки doc85)
final boolean required = (enemy.isInvul() && meleeOpponent(enemy))
    || TankClassMechanics.defendedTank(enemy);
```

`TankClassMechanics.defendedTank(enemy)` возвращает `true`, когда enemy — танк с активным 110/368/760. Это условие срабатывает для **всех** ботов, включая самих танков, и запускает аренд-кайт `invulnerableKite`. Именно это вызывает взаимный кайт в дуэли танков.

## Новое правило

| Self | Target | 110/368/760 на target | `target.isInvul()` | Кайт? |
|---|---|---|---|---|
| **Танк** | **Танк** | да | нет | ❌ **НЕТ** — продолжать бой |
| **Танк** | **Танк** | нет | да | ✅ ДА — кайт |
| **Танк** | **Танк** | да | да | ✅ ДА — кайт (invul приоритетнее) |
| Не-танк | Танк | да | нет | ✅ ДА — прежнее поведение doc60 |
| Не-танк | Танк | нет | да | ✅ ДА — кайт |
| Любой | Не-танк | — | — | Без изменений |

## Изменение в `BotTactics.java` — `tickInvulnerableMelee`

### До (doc60):
```java
final boolean required = (enemy.isInvul() && meleeOpponent(enemy))
    || TankClassMechanics.defendedTank(enemy);
```

### После (doc85):
```java
final boolean selfTank = TankClassMechanics.tank(bot);
final boolean required = (enemy.isInvul() && meleeOpponent(enemy))
    || (TankClassMechanics.defendedTank(enemy) && !selfTank);
```

**Логика:**
- `selfTank = true` (бот — танк) → `(defendedTank && !true)` = `false` → `required = false` при 110/368/760 → кайт не запускается → **атака продолжается**.
- Celestial (`enemy.isInvul()`) → первая ветка `required` срабатывает независимо от `selfTank` → кайт по-прежнему есть.
- `selfTank = false` (не-танк) → поведение без изменений: 110/368/760 → кайт (как в doc60).

### Взаимодействие с `invulnerableMelee`

`invulnerableKite.tick` вызывается внутри `invulnerableMelee`, который вызывается только когда `required = true`. После правки:
- Танк vs танк + 110/368/760: `required = false` → `tickInvulnerableMelee` возвращает `false` → кайт не запускается.
- Танк vs танк + Celestial: `required = true` → кайт-аренд активируется (doc84).

## Карта файлов

| Файл | Изменение |
|---|---|
| `BotTactics.java` | `tickInvulnerableMelee`: добавлена переменная `selfTank`; условие `TankClassMechanics.defendedTank(enemy)` заменено на `TankClassMechanics.defendedTank(enemy) && !selfTank` |
| `TankClassMechanics.java` | Без изменений; `tank(Player)` уже существует |
| `InvulnerableMeleeKite.java` | Без изменений |

## Тест-сценарии

```java
// Сценарий 1: selfTank=true, enemy под UD110 → required=false → кайт не запускается
// (false && meleeOpponent) || (defendedTank && !true) = false ✓

// Сценарий 2: selfTank=true, enemy.isInvul()=true (Celestial) → required=true → кайт
// (true && meleeOpponent(tank)=true) = true ✓

// Сценарий 3: selfTank=false (Cardinal97), enemy под UD110 → required=true → кайт сохранён
// false || (true && !false) = true ✓

// Сценарий 4: selfTank=true, enemy — не-танк → defendedTank=false → required=false (без изменений)
// false || (false && !true) = false ✓
```

```bash
python3 tests/fakeplayersai/invulnerable_target_tests.py  # doc84: 520 assertions
python3 tests/fakeplayersai/tank_mechanics_kite_tests.py  # doc60: 7515 assertions
```

`tank_mechanics_kite_tests.py` затрагивает кайт танков — проверить, что ни один existing тест не ожидает кайта бота-танка при 110/368/760 на противнике-танке. Если такие тесты есть — обновить assertions согласно новому правилу.

## Установка

1. Остановить GameServer; сохранить `BotTactics.java`.
2. В `tickInvulnerableMelee` заменить строку с `required` согласно разделу «После».
3. Перезапустить GameServer.
4. Проверить в бою: Phoenix Knight90 vs Phoenix Knight90 — при наличии UD110 на противнике бот атакует, не отступает; при Celestial — кайт.
5. Проверить: Cardinal97 vs Phoenix Knight90 с UD — Cardinal по-прежнему кайтит (поведение doc60 сохранено).

**Откат:** при остановленном GameServer вернуть `BotTactics.java` из резервной ветки, перезапустить.

Резервная ветка: `backup/before-tank-vs-tank-no-defense-kite-2026-10-21`.
