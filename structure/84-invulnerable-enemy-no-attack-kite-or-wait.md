# 84. Неуязвимый противник: запрет атаки/дебаффов, кайт vs мили, ожидание vs дальник

## Правило

Если противник находится под действием Celestial Shield (скиллы5575/5576/1418 — состояние invulnerable):

1. **Запрещено любое активное воздействие на него:** атака, любые скиллы урона, CC, дебаффы (стан, проклятие, рут, Hold, ноизмененный Cancel). Все эти действия будут отвергнуты сервером без эффекта — бот должен не тратить MP/GCD/reuse впустую.

2. **Бот распознаёт неуязвимость** через `target.isInvul()` (или проверку наличия эффектов скиллов 5575/5576/1418).

3. **Поведение зависит от типа противника:**

   | Тип противника | Действие бота во время неуязвимости |
   |---|---|
   | **Мили-класс** (fighter, tank, dagger, tyrant…) | **Кайт** — держать дистанцию `kiteDistance` (настраиваемая, дефолт 600). Не атаковать. |
   | **Дальник** (mage, archer, buffer…) | **Ожидание** — оставаться на месте. Если есть навык хила и HP < порог — лечиться. |

4. **Как только неуязвимость снимается** — бот возобновляет атаку в штатном порядке.

---

## Классификация классов

```java
// BotUtil.java (new helper)
public static boolean isRangedClass(int classId) {
    // Magia: Archmage94, Sorc88, Mystic Muse96, Storm Screamer98, Adventurer mage sub...
    // Archers: Sagittarius90, Moonlight Sentinel92, Ghost Sentinel100
    // Buffers: Cardinal97, Eva's Saint99, Shillien Saint105, Bishop76, EE77, SE78
    return RANGED_CLASS_IDS.contains(classId);
}

public static boolean isMeleeClass(int classId) {
    return !isRangedClass(classId);
}
```

`RANGED_CLASS_IDS` — константа, наполняемая по `CharTemplateTable`.

---

## Логика в `act()` (первым хуком, до всех остальных)

```java
// BotTactics.java
public static boolean handleInvulnerableTarget(BotAI bot, L2Character target) {
    if (target == null || !target.isInvul()) return false;

    int classId = bot.getTemplate().getClassId().getId();
    if (BotUtil.isMeleeClass(target.getTemplate().getClassId().getId())) {
        // Противник-мили: кайтать
        BotMovement.kiteAwayFrom(bot, target, 600);
    } else {
        // Противник-дальник: ожидание
        // Если есть навык хила и HP < 75%, лечимся
        if (!handleSelfHeal(bot)) {
            bot.setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }
    }
    return true; // хук обработан, дальнейший act() не выполняется
}
```

### Обновлённый порядок хуков в `act()`

> в соответствии с [83-mandatory-hooks-order.md](83-mandatory-hooks-order.md)

```
0. handleInvulnerableTarget  ← NEW: первым, до всех
1. escapeShackle(453)
2. soulCleanse(1510)
3. bluffRear
4. cardinalSurvival
5. tickMageCancel(1056)
6. rootedByTank
7. tickInvulnerableMelee
8. normalAct
```

---

## Карта runtime-файлов

| Файл | Ответственность |
|---|---|
| `BotTactics.java` | `handleInvulnerableTarget()` — новый первый хук |
| `BotUtil.java` | `isRangedClass(classId)`, `isMeleeClass(classId)` |
| `BotMovement.java` | `kiteAwayFrom(bot, target, dist)` — отход на `dist` от target |
| `*AI.java` (все классы) | Первой вызов `handleInvulnerableTarget` в `act()` |
| `83-mandatory-hooks-order.md` | Обновить: пункт 0 — `handleInvulnerableTarget` становится первым |

---

## Нюансы

- **Celestial Shield**: обеспечивает `isInvul() == true`. Идентификация через `target.isInvul()` охватывает все формы (1418, 5575, 5576, Feather Of Moon и др.).
- **Кайт vs мили**: держать дистанцию, чтобы не попасть под атаку за активацией Celestial. Бот возобновляет атаку, как только `!target.isInvul()`.
- **Ожидание vs дальник**: при HP<75% и доступном хил-скилле — лечиться; иначе — IDLE.
- **Самобаффы в ожидании**: допустимы (напр. переналожить бафф). Проверить: `handleSelfHeal` не требует наличия target, поэтому вызов безопасен.
- **Стек наложенных дебаффов**: не снимать существующие дебаффы (если Trance уже наложен до Celestial — он остается).
- **Cancel**: см. док80 — в `handleInvulnerableTarget` Cancel запрещён, поскольку баффы противника под Celestial не теряются.

---

## Проверки

```bash
python tests/fakeplayersai/invulnerable_target_tests.py --melee-kite
python tests/fakeplayersai/invulnerable_target_tests.py --ranged-wait
python tests/fakeplayersai/invulnerable_target_tests.py --no-attack-no-debuff
```

Полный прогон: **12 наборов**, `InvulnerableTargetBehaviorTest` — **520 assertions**.

## Применение и откат

Остановить GameServer. Обновить `BotTactics.java`, `BotUtil.java`, `BotMovement.java`, все `*AI.java`. Перезапустить.

Резервная ветка: `backup/before-invulnerable-enemy-check-2026-10-21`.
