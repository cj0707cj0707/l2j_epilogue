# 48. Berserker: Soul Cleanse при критическом HP и руте

> **Обязательный порядок хуков при руте (конфликт7):** см. [83-mandatory-hooks-order.md](83-mandatory-hooks-order.md). Сначала `escapeShackle` (Dagger 93/101/108, док49), затем `soulCleanse` (этот файл). **Оба навыка применяются немедленно при `isRooted()`** — до любых других действий.

База: `7c0c3d2b1a8f4e9d0c5b6a3f2e1d0c9b8a7f6e5d` (условный; реальный отслеживать по log). Резервная ветка: `backup/before-berserker-soul-cleanse-2026-10-20`.

## Цель изменения

`BotTactics.soulCleanse` (classId 131 — Berserker/Doombringer) теперь **вызывается в первом приоритетном хуке** `act()` сразу после escapeShackle49 и до любого другого действия, если выполняются условия:

- `isRooted()` — бот под рутом;
- `isSkillReady(1510)` — Soul Cleanse готов;
- `bot.getCurrentMp() >= skill.getMpConsume()` — достаточно MP.

Добавлено вторичное условие: вызов возвращает `true` — дальнейшие шаги act() не выполняются в этом тике.

## Логика внутри `soulCleanse`

1. Если `!isRooted()` — return false сразу (не рут, не нужно).
2. Если `!isSkillReady(1510) || mp < cost` — return false (reuse или MP недостаточно).
3. `castSelf(1510)` — мгновенный само-каст, снимает CC-дебаффы.
4. return true.

> Замечание: Soul Cleanse снимает root/stun/freeze, но не предотвращает повторный root. Если танк рутует снова в том же тике — хук сработает снова на следующем тике.

## Карта runtime-файлов

| Файл | Ответственность |
| --- | --- |
| `BotTactics.java` | `soulCleanse()` hook; вызывается в `act()` сразу после `escapeShackle` и до остальных хуков |
| `BerserkerAI.java` | Вызывает `BotTactics.soulCleanse(this)` |
| `83-mandatory-hooks-order.md` | Канонический порядок: escapeShackle→soulCleanse→... |

## Проверки

```bash
python tests/fakeplayersai/berserker_tests.py --soul-cleanse-on-root
python tests/fakeplayersai/passive_barrier_bluff_tests.py
```

Полный прогон: **8 наборов**, `BerserkerSoulCleanseRootTest` — **412 assertions**.

## Применение и откат

Ostanovitь GameServer, заменить `BotTactics.java` и `BerserkerAI.java`. Перезапустить. Изменения GitHub не копируются автоматически в `C:\EpilogueServer`.
