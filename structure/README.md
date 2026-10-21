# structure/

Справочник файлов структуры сервера Lineage 2 Epilogue (l2j_epilogue).

## Последнее обновление

- [90 — танки: немедленный навык при stage3](90-tank-stage3-immediate-class-finisher.md): готовые785/762/789 обходят обычную задержку решения и выбираются первыми.
- [89 — Dominator: Steal до окончания собственного Silence](89-dominator-seal-steal-kill-window.md): после успешного собственного1246 — только1245 и немедленный pressure на нативной дальности; проверки и установка.
- [88 — классовые ротации и защита танков после сближения](88-dominator-ranged-cycle-charge-melee-and-tank-contact.md): базовый Steal/Sleep/CP/Silence115, зарядники345/346 в конце, melee88/114 и UD после контакта.

- [87 — Неуязвимый дальник, повторный сон под root и root-снятие раньше Bluff](87-invulnerable-ranged-root-priorities-and-doc-audit.md): реализованные правила, реальная карта файлов, проверки и исправление недостоверной документации.

- [86 — Танк против танка без кайта по UD/Vengeance; броня, оружие и бижутерия +10](86-tank-mirror-no-defense-kite-and-all-gear10.md): реализованы все три точки проверки, актуальный конфиг, регрессии и установка.
- [85 — Танк vs танк: правило исключения защиты](85-tank-vs-tank-no-defense-kite-only-celestial.md): актуализировано по фактическому коду86.

- [84 — Неуязвимый противник: запрет атаки/дебаффов, поведение по роли против неуязвимой цели](84-invulnerable-enemy-no-attack-kite-or-wait.md)
- [83 — Обязательный порядок хуков в `act`](83-mandatory-hooks-order.md): реальные уровни BotManager/tick/act, без вымышленных обработчиков
- [82 — Отчёт по конфликтам ботов](82-bot-conflict-report.md): 9 конфликтов, принятые решения
- [80 — Dominator sleep/hold; Cancel на противника](80-dominator-sleep-hold-and-mage-cancel-priority.md)
- [79 — Dominator: сон танка под защитой](79-dominator-sleep-during-tank-defense.md)
- [75 — Cardinal: Trance, без кайта под Celestial, 210с](75-cardinal-always-trance-no-shield-kite-and-210s.md)
- [49 — Dagger: Escape Shackle немедленно при руте](49-dagger-escape-shackle-root-only.md)
- [48 — Berserker: Soul Cleanse при root/physical mute](48-berserker-emergency-soul-cleanse.md)

## Ключевые концепции

| # | Файл | Описание |
|---|---|---|
| 48 | `48-berserker-emergency-soul-cleanse.md` | Soul Cleanse1510 при root или physical mute, native cast |
| 49 | `49-dagger-escape-shackle-root-only.md` | Escape Shackle453 при root, в том числе раньше собственного Bluff |
| 53 | `53-bluff-reuse-and-dominator-ranged-pressure.md` | Bluff358 reuse 12000мс |
| 57 | `57-default-trance-and-bluff-cooldowns.md` | Кулдауны Trance/Bluff |
| 75 | `75-cardinal-always-trance-no-shield-kite-and-210s.md` | Cardinal: доступный heal приHP<75% выше Trance с78; автокаст1418 запрещён |
| 78 | `78-bot-gear10-and-cardinal-heal75-no-celestial.md` | Броня/бижутерия+10, оружие+10 с86; HP<75%→heal1218 первым; 1418 запрещён |
| 79 | `79-dominator-sleep-during-tank-defense.md` | Dominator1097 vs tank; повторь при руте |
| 80 | `80-dominator-sleep-hold-and-mage-cancel-priority.md` | HOLD после сна; Cancel1056 на противника, не Trance |
| 81 | `81-tank-root-stationary-rotation.md` | Root → стационарная ротация |
| 82 | `82-bot-conflict-report.md` | Анализ 9 конфликтов |
| 83 | `83-mandatory-hooks-order.md` | Реальные уровни обязательных обработчиков и приоритетов |
| 84 | `84-invulnerable-enemy-no-attack-kite-or-wait.md` | Запрет атак по invul; против дальника мили подходит/ждёт, дальник кайтит |
| 85 | `85-tank-vs-tank-no-defense-kite-only-celestial.md` | Исключение танк vs танк, реализовано86 во всех путях |
| 86 | `86-tank-mirror-no-defense-kite-and-all-gear10.md` | Единая политика кайта, вся экипировка+10, карта файлов/тесты/установка |
| 87 | `87-invulnerable-ranged-root-priorities-and-doc-audit.md` | Invul-дальник: подход/ожидание мили, кайт дальника; rooted1097;453 раньше Bluff; аудит docs |
| 90 | `90-tank-stage3-immediate-class-finisher.md` | Текущая срочность stage3: немедленный Flame Icon / Insane Crusher / Spirit of Shilen |
| 89 | `89-dominator-seal-steal-kill-window.md` | Текущий kill-window115: только Steal Essence, пока действует собственный Seal of Silence |
| 88 | `88-dominator-ranged-cycle-charge-melee-and-tank-contact.md` | Базовые ротации115, нативные заряды/melee88/114, защита танков только после сближения |
