# 83. Обязательный порядок хуков в `act()`

Создан по итогам анализа конфликтов (doc82). Обновлён: добавлен хук 0 (doc84).

## Полный порядок

| № | Хук | Классы | Док |
|---|---|---|---|
| **0** | `handleInvulnerableTarget` | все | [84](84-invulnerable-enemy-no-attack-kite-or-wait.md) |
| 1 | `escapeShackle(453)` | Dagger 93/101/108 | [49](49-dagger-escape-shackle-root-only.md) |
| 2 | `soulCleanse(1510)` | Berserker 131 | [48](48-berserker-emergency-soul-cleanse.md) |
| 3 | `bluffRear` | Dagger 93/101/108 | [53](53-bluff-reuse-and-dominator-ranged-pressure.md) |
| 4 | `cardinalSurvival` | Cardinal 97 | [75](75-cardinal-always-trance-no-shield-kite-and-210s.md) |
| 5 | `tickMageCancel(1056)` | Archmage 94, Mystic Muse 96 | [80](80-dominator-sleep-hold-and-mage-cancel-priority.md) |
| 6 | `rootedByTank` | все vs танк | [81](81-tank-root-stationary-rotation.md) |
| 7 | `tickInvulnerableMelee` | все vs танк | [75](75-cardinal-always-trance-no-shield-kite-and-210s.md) |
| 8 | `normalAct` | все | — |

## Правило

Каждый хук возвращает `true` (обработал) или `false` (пропустить). При `true` — остальные хуки в этом тике не выполняются.

**Хук 0** — самый первый: если `target.isInvul()`, вся атака/дебаффы блокируются сразу. Бот либо кайтит (мили-противник), либо ожидает/лечится (дальник-противник).

```java
// Типовая структура act() всех *AI.java
protected void runAct() {
    L2Character target = getTarget();
    if (handleInvulnerableTarget(this, target))  return; // 0 — FIRST
    if (escapeShackle(this))                     return; // 1
    if (soulCleanse(this))                       return; // 2
    if (bluffRear(this, target))                 return; // 3
    if (cardinalSurvival(this, target))          return; // 4
    if (tickMageCancel(this, target))            return; // 5
    if (rootedByTank(this, target))              return; // 6
    if (tickInvulnerableMelee(this, target))     return; // 7
    normalAct(this, target);                             // 8
}
```
