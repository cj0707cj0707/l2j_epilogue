# Документация по структуре сервера Lineage 2 Gracia Epilogue (L2J-Mobius)

> Назначение этой папки — справочник по структуре и устройству сервера в `C:\EpilogueServer`.
> При любых последующих манипуляциях с сервером (правка конфигов, датапака, скриптов, БД) сначала сверяйся с этими документами.

## Что это за сервер

- **Проект:** L2J-Mobius, хроника **The 2nd Throne: Gracia Epilogue** (CT 2.6).
- **Тип:** развёрнутая (скомпилированная) сборка — ядро в JAR, датапак и скрипты в исходниках (`game/data/scripts/**/*.java`, компилируются сервером при старте).
- **Java:** OpenJDK 25 (аргументы запуска — в `game/java.cfg`, ZGC, heap 2–4 ГБ).
- **БД:** MySQL/MariaDB, схема **`l2jmobiusepilogue`** (подключение — `game/config/Database.ini`, `login/config/Database.ini`, пользователь `root`, без пароля).
- **Язык скриптов:** Java (квесты, ИИ, инстансы, события, хендлеры) + HTML-диалоги + XML-данные.

## Архитектура в двух словах

```
Клиент L2 (Gracia Epilogue)
      │  порт 2106 (протокол логина)        порт 7777 (игровой)
      ▼                                        ▼
LoginServer (login/)  ──регистрация──▶  GameServer (game/)
      │                                        │
      └──────────── MySQL: l2jmobiusepilogue ──┘
```

- **LoginServer** — авторизация аккаунтов, выдача hexid, список серверов. Таблицы БД: `accounts`, `gameservers` и др.
- **GameServer** — вся игровая логика: мир, NPC, квесты, инстансы, осады, олимпиада и т.д. Таблицы БД: 116+ (`characters`, `items`, `clan_data`, ...).
- Связка Login↔Game регистрируется один раз (файл `game/config/hexid.txt`); перерегистрация — `login/GameServerRegister`.

## Последнее обновление

[63. Cardinal: Clarity/Trance; кинжальщики: Bluff → спина → удары](63-cardinal-clarity-trance-and-dagger-bluff-rear.md) — survival безProminence/Backfire, Mana Burn только после фактического окончанияTrance; собственный358-STUN требует реального обхода до ударов/531, без автоатаки даже при0MP. Актуальная карта файлов и установка.

[62. Приоритет выживания и регрессии аудита](62-combat-survival-priorities-and-audit-regressions.md) — исправлены только1/3/4/5/9/12: спасательные self-касты перед движением, rooted131, ограниченные попытки115, полный тестовый агрегатор и актуальная карта файлов.

[61. Phoenix: Flame Icon первым, Sacrifice запрещён под иконами](61-phoenix-flame-first-and-no-sacrifice-under-icons.md) — готовый785 раньше406/69;406 резерв lowHP; под785/406 запрет69 вAI,cast guard иHeal execution. Карта файлов, проверки и установка.

[60. Кайт защиты танков и native классовые механики](60-tank-defense-kiting-and-native-class-mechanics.md) — все19 классов кайтят танка под110/368/760; Phoenix:406 при lowHP и784→5562/3→785, Hell:761→5561/3→762, Shillien:788→5564/3→789. Родные условия/ресурсы, карта файлов и тесты.

[59. Cardinal: выживание против Clarity; три bot-only дебаффа15%](59-cardinal-clarity-survival-and-fixed-control-chance.md) — пока на противнике действующий3282, только Trance/Celestial Shield/Prominence/Greater Battle Heal с приоритетом защиты. Trance97, Dreaming Spirit115 иFear115: фикс15%, без изменения навыков игроков; карта файлов, тесты, установка. Это исключение к nativeChance из56/58.

[58. Олимпиада150с, кайт invul-мили, штатная MP и Maximum Clarity](58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md) — все19 классов; Cardinal infinite MP сохранена,−90% regen и HP101 отменены; противник97 получает native3282 на60с от старта, без повторного обновления. Карта файлов, тесты и установка.

[57. Штатные откаты Trance и Bluff](57-default-trance-and-bluff-cooldowns.md) — Bluff8000мс вместо16000 для игроков/ботов; убран отдельный3с AI-delay Trance, native reuse1000мс сохранён. Chance/эпик и остальные механики без изменений.

[56. Родные дебаффы/Chance и постоянный эпик+6](56-native-debuff-chance-and-permanent-epic-jewelry.md) — убраны tank25% и private Trance65%; реальные Chance+30/+15, полный список навыков19 классов; пять эпиков всем ботам, безопасная миграция и карта файлов.

[55. Dominator: root/stun→200→Steal/CP; Shadow Step; Олимпиада90с](55-dominator-melee-control-shadow-step-and-90s-olympiad.md) — scoped CP до>85%,821 первым против кастеров, пятый core-патч переполнения таймера и обновлённая установка; лимит90с заменён150с в58.

[54. Отход после сна и команда живому саммону раз в3с](54-post-sleep-retreat-and-summon-attack-pulse.md) — Cardinal до дальности Mana Burn внутри круга450, Dominator до300; фактический собственный сон, без повторного призыва.

[52. GM: вход без invul/invisible и очистка после просмотра Олимпиады](52-gm-login-and-olympiad-observer-cleanup.md) — отключены все startup-пути автоскрытия; native выход из просмотра снимает оба флага и у GM. Четвёртый проверяемый core-патч, назначение файлов и установка.

[51. Cardinal/Bishop: восстановление и ник противника на Олимпиаде](51-cardinal-recovery-and-olympiad-opponent-notice.md) — покрытие19 профессий при старом/заполненном лимите, безопасный поиск потерянного офлайн-бота в БД, личное сообщение при прибытии на арену; карта файлов, тесты и установка.

[50. Dominator: обязательный серверный Victory of Pa'agrio](50-dominator-mandatory-server-victory.md) — дополнительный1414 перед Олимпиадой/тренировкой независимо от overrides/изученности, learned enchant или native base, только115, без бесплатного rebuff в бою.

[49. Кинжальщики: Escape Shackle только при руте](49-dagger-escape-shackle-root-only.md) — приоритетный native453 для93/101/108, обычная ротация во время отката, повтор при сохраняющемся root; исключение из случайных auto/tactical-пулов, карта файлов и установка.

[48. Берсеркер: срочный Soul Cleanse](48-berserker-emergency-soul-cleanse.md) — магический self-cast1510 при ROOT/PhysicalMute до Rush/movement/reaction-ограничений, удержание своей цели до завершения, native ресурсы и откат.

[47. Кинжальщики: попытка захода за спину раз в две секунды](47-dagger-rear-approach-two-seconds.md) — восстановлен отдельный rear-approach для93/101/108 в обоих arena-режимах,2000мс между попытками, без generic recovery-отходов; карта файлов, регрессии и установка.

[41. Trance65%, круговой кайт, граница бот–бот и MP магов](41-fixed-trance-orbit-boundary-and-mage-mana.md) — исторический private65%-ный Trance (отменён в56), движение по окружности450, core-ограничитель фактических координат460 только для бот–бот и исторический HP101 при истощении магов (отменён в58); обновление запуска/установщика.

[40. Soultaker, защита танков и самолечение](40-soultaker-tank-recovery-and-invulnerability.md) — кайт95, Anti Magic Armor перед Vengeance против магов, bot-only Sacrifice +30 Power без отката ниже70%, запрет активного самолечения под invul и Body Reconstruction при≤40% в обеих аренах.

[39. Физические боты: автоатака без MP](39-low-mana-physical-autoattack.md) — fallback для немагических классов, включая ножей/лучников/берсеркера; без физического MP floor и без изменений Cardinal.

[38. Кардинал: неисчерпаемая мана в обоих режимах](38-cardinal-unlimited-arena-mana.md) — персональное восстановление MP в общем боевом тике после подтверждения каста; без изменения общих навыков и MP противника.

[37. Кардинал: Олимпиада как тренировка](37-cardinal-olympiad-training-parity.md) — владение целью, сближение под Trance, исторический regMp×0.10 (снят в58) и запрет олимпийского full-MP floor в паре с ботом97; безопасная очистка.

[36. Кардинал: имя без цифр и дополнительный Prominence](36-cardinal-name-and-prominence.md) — стабильное уникальное буквенное имя, bot-only1230/28, приоритет законного каста через10с и согласованные фильтры сна.

[35. Вход персонажей: восстановление XML навыков](35-character-login-skill-xml-recovery.md) — исправлен placeholder вместо99 определений1300–1399; восстановлены1320/1322, добавлены XML/XSD и real-core регрессии общей цепочки входа/спавна.

[34. Доминатор: Victory/CP; Титан: давление; Vengeance против всех](34-dominator-cp-titan-pressure-vengeance-all.md) — классовое исключение1414, реальные CP-самокасты около50%, отсутствие отхода Титана из-за откатов и разрешение368 против магов/лучников.

[33. Танки: без самообездвиживания против дальников](33-tank-no-immobile-defense-vs-ranged.md) — общий запрет УД/Vengeance и корневой self-защиты против лучников/магов на любой дистанции, включая overrides.

[32. Танки: сближение, рут/стан и Vengeance перед УД](32-tank-close-control-and-vengeance-first.md) — постоянная ближняя дистанция, приоритет контроля, строгий подход и защита без остановки дальнего преследования.

[31. Циклический Rush Impact до окончания исходного стана](31-cyclic-rush-impact-until-original-stun-ends.md) — обработка точного нулевого приземления, повторный ограниченный отход и сброс после эффекта.

[30. Исправление установщика и Windows-запуска](30-debuff-installer-windows-startup.md) — закрытие дескрипторов целевого JAR, независимый classpath, журнал точной ошибки; игровые механики не изменены.

[26. Откаты до таймера селфбафов и тренировочные Frenzy/Zealot](26-training-prep-cooldowns-and-orc-buffs.md) — ранний reset обоих; финальная секунда восстанавливает только ресурсы.
[27. Бафы игрока-орка и классовые ресурсы](27-training-orc-player-buffs-and-resources.md) — одинаковый Orc-набор для человека/бота, 70% HP / 15% CP / 100% MP для113/114.
[28. Дебафы без замены/продления категории](28-debuff-category-no-refresh.md) — глобальная проверка ядра, установка при запуске, резерв и откат.
[29. Защита танков, цикл Rush и входной хил](29-tank-defense-rush-loop-and-entry-heal.md) — защита после5с, непродлеваемый собственный стан, полный хил всех после телепорта.

[25. Берсеркер: простой после Rush Impact на тренировке](25-training-berserker-post-rush.md) — отличие от олимпийского драйвера, серия навыков и обычное давление между откатами.

[24. Тренировка: вход, подготовка, восстановление и возврат](24-training-preparation-location-and-return.md) — без порога ресурсов, очистка прежних эффектов, реальные селфкасты бота, финальный reset обоих, порт Гирана и устранение повторных остановок.

[23. Ограниченное движение под страхом](23-bounded-fear-movement.md) — случайные короткие шаги вокруг места наложения; общий Fear handler, радиус100, установка и откат.


[22. Навыки классов, усиления до старта, ворота и результаты тренировок](22-class-skills-openers-and-training-results.md) — карта файлов, установка, проверки и откат. История предыдущего обновления; актуальный порядок тренировки — документ24.

## Состав документации

| Файл | Содержание |
|---|---|
| [62-combat-survival-priorities-and-audit-regressions.md](62-combat-survival-priorities-and-audit-regressions.md) | Текущие приоритеты выживания, root131, бюджет контроля115, восстановленные регрессии и установка |
| [58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md](58-olympiad-150s-invulnerability-kite-native-mana-and-clarity.md) | Текущее:150с, всеобщий invul-кайт мили, обычнаяMP, native3282/60с, карта и установка |
| [57-default-trance-and-bluff-cooldowns.md](57-default-trance-and-bluff-cooldowns.md) | Отмена увеличенного Bluff reuse и отдельного Cardinal Trance timer; штатные параметры, тесты и установка |
| [56-native-debuff-chance-and-permanent-epic-jewelry.md](56-native-debuff-chance-and-permanent-epic-jewelry.md) | Native дебаффы вместо25%/65%, Chance+30/+15 по19 классам и постоянные пять эпиков+6 |
| [48-berserker-emergency-soul-cleanse.md](48-berserker-emergency-soul-cleanse.md) | Срочное магическое самоочищение берсеркера131 при руте/запрете физических навыков, приоритеты и ownership |
| [47-dagger-rear-approach-two-seconds.md](47-dagger-rear-approach-two-seconds.md) | Заход кинжальщиков за спину: реальное движение, интервал2000мс, приоритеты, geodata и отличие от recovery |
| [39-low-mana-physical-autoattack.md](39-low-mana-physical-autoattack.md) | Оружейная автоатака при нехватке MP, исключения магов/Cardinal, временный MP-расход обычного выстрела |
| [38-cardinal-unlimited-arena-mana.md](38-cardinal-unlimited-arena-mana.md) | Кардинал: неисчерпаемая MP только боту в активной тренировке/Олимпиаде, сохранение regMp противника |
| [37-cardinal-olympiad-training-parity.md](37-cardinal-olympiad-training-parity.md) | Кардинал: отличия родительских драйверов, Trance-дальность и историческое -90% regMp (отменено в58), lifecycle эффекта |
| [36-cardinal-name-and-prominence.md](36-cardinal-name-and-prominence.md) | Кардинал: безопасное имя без цифр, ручная выдача Prominence только боту и тактический интервал10с |
| [35-character-login-skill-xml-recovery.md](35-character-login-skill-xml-recovery.md) | Вход после выбора персонажа: повреждённый XML1300–1399, восстановление, карта файлов и регрессии |
| [01-root-structure.md](01-root-structure.md) | Корневые папки сервера: `game`, `login`, `libs`, `db_installer`, `backup`, `images` |
| [02-launching.md](02-launching.md) | Запуск/остановка серверов, `java.cfg`, логи, консольные скрипты |
| [03-game-config.md](03-game-config.md) | `game/config` — все конфиги GameServer (+ `Custom/` — кастомные фичи) |
| [04-login-server.md](04-login-server.md) | `login/` — устройство LoginServer, его конфиги и данные |
| [05-datapack-data.md](05-datapack-data.md) | `game/data` — датапак: XML-данные, geodata, зоны, спавны, мультиселлы, статы |
| [06-scripts-and-quests.md](06-scripts-and-quests.md) | `game/data/scripts` — Java-скрипты: квесты, ИИ, инстансы, события, хендлеры |
| [07-database.md](07-database.md) | `db_installer` — схема БД, установщик, бэкапы-дампы |
| [08-admin-and-mods.md](08-admin-and-mods.md) | Админ-команды, кастомные моды, типовые задачи администратора |
| [09-fake-players-ai.md](09-fake-players-ai.md) | Система боевых ботов FakePlayersAI: устройство, олимпиада, конфиг, `//bot`, журнал правок |
| [10-stun-visual-and-olympiad-ui.md](10-stun-visual-and-olympiad-ui.md) | Визуал стана (звёзды над головой) и панель противника на олимпиаде (HP/CP + бафы/дебафы), журнал правок ядра и скриптов |
| [11-line-of-sight-fix.md](11-line-of-sight-fix.md) | Фикс 11.09.2026: атака/касты/чарджи сквозь стены, двери и заборы (`GeoEngine.canSeeTarget`/`canMoveToTarget`), журнал правок ядра |
| [12-blink-fix.md](12-blink-fix.md) | Фикс 13.09.2026: Blink (скилл 1448 Soul Hound) — self-эффекты применяются даже при пустом списке целей, журнал правок ядра |
| [13-oly-remote-registration.md](13-oly-remote-registration.md) | Команда 14.09.2026: `/oly`, `//oly`, `.oly` — дистанционная регистрация на бесклассовую олимпиаду без менеджера; патч гейта AdminCommandHandler (команды с accessLevel=0 доступны игрокам) |
| [14-bluff-turn.md](14-bluff-turn.md) | Фикс 18-19.09.2026: Bluff / Blinding Blow разворачивают противника спиной (ValidateLocation окружающим + «нулевой шаг» MoveToLocation/StopMove собственному клиенту жертвы — без телепорта и затемнения экрана) |
| [15-dominator-enlightenment.md](15-dominator-enlightenment.md) | Фикс 03.10.2026: Enlightenment у Dominator — «волшебный» вариант 1532 (+40% M.Atk) вместо «хилерского» 1533 (+10% M.Atk/+40% Heal), как у всех магических классов; то же в бот-пресете |
| [16-giran-adena-giver.md](16-giran-adena-giver.md) | NPC Adena Giver в Гиране: 100 000 000 адены за нажатие, файлы, проверки и установка |
| [17-olympiad-predictive-chase.md](17-olympiad-predictive-chase.md) | Олимпийские боты: чтение направления движения противника, прогноз перехвата и сокращение дистанции |
| [18-bot-combat-fixes-and-rollback.md](18-bot-combat-fixes-and-rollback.md) | Исправление ложных кастов ботов, улучшения боя и резервная точка/порядок отката |
| [19-pursuit-movement-ownership.md](19-pursuit-movement-ownership.md) | Перехват v2: конфликт CAST-подхода/автокаста, приоритет движения, диагностика, установка и отдельный откат |
| [20-olympiad-suite-and-giran-services.md](20-olympiad-suite-and-giran-services.md) | Тактика/сложности, изолированные тренировки, аналитика, четыре NPC Гирана, инструкция, восстановление и безопасный откат |
| [21-strong-tempo-and-training-fixes.md](21-strong-tempo-and-training-fixes.md) | Strong/100ms, атака вместо частого кайта, исправление NPC-кнопок, олимпийский бафф и30сек. на селфбафф |
| [22-class-skills-openers-and-training-results.md](22-class-skills-openers-and-training-results.md) | Классовые навыки, предбоевые усиления, ворота и результаты; старые правила тренировки уточняет24 |
| [23-bounded-fear-movement.md](23-bounded-fear-movement.md) | Короткие шаги страха вокруг исходной точки, общий handler и регрессии |
| [24-training-preparation-location-and-return.md](24-training-preparation-location-and-return.md) | Актуальные правила/файлы тренировки, scoped PeaceZone, подготовка/восстановление, возврат без повторного стопа |
| [25-training-berserker-post-rush.md](25-training-berserker-post-rush.md) | Берсеркер на тренировке: приоритет серии после Rush и устранение простоя |
| [26-training-prep-cooldowns-and-orc-buffs.md](26-training-prep-cooldowns-and-orc-buffs.md) | Сброс откатов до подготовки, тренировочные бафы Titan/Grand Khavatari |

## Последнее изменение

- [55: Контроль мили-цели, Shadow Step и90с](55-dominator-melee-control-shadow-step-and-90s-olympiad.md): карта файлов, таймер/установка, проверки и откат.

- [54: Отход после сна; повторная атака саммона](54-post-sleep-retreat-and-summon-attack-pulse.md): маршрут внутри450, Cardinal — реальная дальность1398, Dominator —300; общий3000мс native attack pulse без resummon.

- [53: Bluff ×2 reuse и Dominator против дальнего боя](53-bluff-reuse-and-dominator-ranged-pressure.md): исторический358 —16с (в57 возвращён8с); бот115 — сближение → сон/стан → проклятия → атака с поддержкой CP.

## Куда смотреть при типовых задачах

| Задача | Документ |
|---|---|
| Не входит после выбора персонажа; Skill1322/1320 отсутствует; NPE getAllAvailableSkills | `35-character-login-skill-xml-recovery.md` |
| Поменять рейты (XP/SP/дроп/адена) | `03-game-config.md` → `Rates.ini` |
| Изменить порт/IP/сетевые настройки | `03-game-config.md` → `Network.ini`, `Server.ini`; `04-login-server.md` |
| Включить/настроить офлайн-трейд, баффера и т.п. | `03-game-config.md` → `Custom/*.ini` |
| Добавить/поправить квест или ИИ NPC | `06-scripts-and-quests.md` |
| Поправить диалог NPC | `05-datapack-data.md` → `html/` |
| Изменить спавны/добавить мобов | `05-datapack-data.md` → `spawns/` |
| NPC для выдачи 100 миллионов адены в Гиране | `16-giran-adena-giver.md` |
| Добавить предмет в магазин | `05-datapack-data.md` → `buylists/`, `multisell/` |
| Проблемы с БД, восстановление | `07-database.md` |
| Аккаунты, баны, права | `08-admin-and-mods.md` |
| Боты FakePlayersAI (спавн, ротации, олимпиада) | `09-fake-players-ai.md`; новая тактика/тренировки — `20-olympiad-suite-and-giran-services.md` |
| Перехват убегающего противника ботами на олимпиаде | `17-olympiad-predictive-chase.md`; актуальный фикс/диагностика — `19-pursuit-movement-ownership.md` |
| Визуальные эффекты (стан/звёзды), панель противника олимпиады | `10-stun-visual-and-olympiad-ui.md` |
| Неработающие скиллы (Blink и т.п.) — что смотреть в цепочке каста | `12-blink-fix.md` |
| Bluff / Blinding Blow не разворачивают противника | `14-bluff-turn.md` |
| Дистанционная регистрация на олимпиаду (`/oly`, `//oly`, `.oly`) | `13-oly-remote-registration.md` |

## Важные файлы-«точки входа»

| Файл | Роль |
|---|---|
| `game/GameServer.vbs` / `.sh` | Запуск GameServer (Windows/Linux) |
| `login/LoginServer.vbs` / `.sh` | Запуск LoginServer |
| `game/java.cfg`, `login/java.cfg` | Аргументы JVM (память, GC, кодировка) |
| `game/config/Database.ini` | Подключение GameServer к MySQL |
| `login/config/Database.ini` | Подключение LoginServer к MySQL |
| `game/config/hexid.txt` | Идентификатор зарегистрированного GameServer |
| `game/config/Server.ini` | Имя сервера, порт 7777, макс. игроков |
| `game/config/Scripts.xml` | Управление загрузкой скриптов (exclude/include) |
