# 12. Фикс Blink: телепортация скилла 1448 (Soul Hound) без целей

> Правка от 13.09.2026. Задача: скилл **Blink** класса Soul Hound проигрывал анимацию,
> но не телепортировал персонажа назад.
> **Статус: установлен** в `libs\GameServer.jar` (13.09.2026, при остановленном GS).

## Суть

- Blink — скилл **1448** (`game\data\stats\skills\01400-01499.xml`): `targetType AURA`,
  телепорт реализован как self-эффект `<selfEffects><effect name="Blink" /></selfEffects>`
  (`flyRadius=400`, `flyCourse=0` — назад). Хендлер эффекта — скрипт
  `game\data\scripts\handlers\skill\effects\Blink.java` (зарегистрирован в `EffectMasterHandler`,
  в логах «Loaded 164 effect handlers» — регистрация в порядке).
- Причина бага была **не в эффекте, а в ядре**: `Creature.callSkill` выходил при пустом
  списке целей (`targets.isEmpty() → return`) **до** применения self-эффектов
  (они применяются в `Skill.activateSkill`, вызываемом из `callSkill` уже после этого выхода).
  Тип AURA — один из немногих, для которых пустой список целей не прерывает каст
  (`onMagicLaunchedTimer`), поэтому каст доигрывался до конца (анимация, расход MP),
  но телепорт не происходил. При наличии валидных целей рядом телепорт работал.

## Правка (один файл в ядре)

`org/l2jmobius/gameserver/entity/actor/Creature.java` — метод `callSkill`: при пустом
списке целей скиллы с self-эффектами применяют их перед выходом (тот же порядок, что в
self-блоке `Skill.activateSkill`). Детали, список затронутых скиллов, установка/откат и
план игровой проверки — `_fix_blink\README.md`.

## Границы/замечания

- Правка ядра (JAR) — требуется перезапуск GameServer.
- Затронуты все скиллы с `selfEffects` и AURA-семейством целей: 1448 Blink, 885 Air Blink,
  368 Vengeance, 8240 Obliteration Storm, 7078 Test - Blink — у них self-эффекты теперь
  применяются и без врагов рядом (как в ритейле).
- Исходники патча и бэкап JAR — `_fix_blink\`; откат возвратом `GameServer.jar.pre-blink`.
