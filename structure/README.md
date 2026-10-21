# structure/

Справочник файлов структуры сервера Lineage 2 Epilogue (l2j_epilogue).

## Последнее обновление

- [83 — Обязательный порядок хуков в `act`](83-mandatory-hooks-order.md): escapeShackle→soulCleanse→bluffRear→cardinalSurvival→tickMageCancel→rootedByTank→tickInvulnerableMelee→normalAct. Создан по итогам анализа конфликтов (док82).
- [82 — Отчёт по конфликтам ботов](82-bot-conflict-report.md): 9 конфликтов, принятые решения.
- [80 — Dominator sleep/hold; Archmage/Mystic Muse: Cancel на противника](80-dominator-sleep-hold-and-mage-cancel-priority.md): конфликтт4/5.
- [79 — Dominator: сон танка под защитой](79-dominator-sleep-during-tank-defense.md): конфликтт2/4.
- [75 — Cardinal: Trance, без кайта под Celestial, 210с](75-cardinal-always-trance-no-shield-kite-and-210s.md): конфликт1/8.
- [49 — Dagger: Escape Shackle немедленно при руте](49-dagger-escape-shackle-root-only.md): конфликт7.
- [48 — Berserker: Soul Cleanse немедленно при руте](48-berserker-emergency-soul-cleanse.md): конфликт7.

## Ключевые концепции

| # | Файл | Описание |
|---|---|---|
| 48 | `48-berserker-emergency-soul-cleanse.md` | Soul Cleanse1510 сразу при `isRooted()` |
| 49 | `49-dagger-escape-shackle-root-only.md` | Escape Shackle453 сразу при `isRooted()`, первым хуком |
| 53 | `53-bluff-reuse-and-dominator-ranged-pressure.md` | Bluff358 reuse 12000мс |
| 57 | `57-default-trance-and-bluff-cooldowns.md` | Кулдауны Trance/Bluff |
| 75 | `75-cardinal-always-trance-no-shield-kite-and-210s.md` | Cardinal: Trance→HP<75%→heal; Celestial1418 запрещён78 |
| 78 | `78-bot-gear10-and-cardinal-heal75-no-celestial.md` | Gear+10; HP<75%→heal1218 первым; 1418 запрещён |
| 79 | `79-dominator-sleep-during-tank-defense.md` | Dominator1097 vs tank; повторь при руте |
| 80 | `80-dominator-sleep-hold-and-mage-cancel-priority.md` | HOLD после сна; Cancel1056 на противника, не Trance |
| 81 | `81-tank-root-stationary-rotation.md` | Root → стационарная ротация |
| 82 | `82-bot-conflict-report.md` | Анализ 9 конфликтов |
| 83 | `83-mandatory-hooks-order.md` | Обязательный порядок хуков в act() |
