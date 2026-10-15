# Фикс Blink (скилл 1448, класс Soul Hound): телепортация при пустом списке целей

> Правка от 13.09.2026. Задача: скилл **Blink** (id 1448, Soul Hound) проигрывал анимацию,
> но телепортация кастера назад не происходила.
> **Статус: установлен** в `libs\GameServer.jar` (13.09.2026, при остановленном GS).

## Причина

- Скилл 1448 Blink (`game\data\stats\skills\01400-01499.xml`) — `targetType AURA`,
  а сам телепорт реализован как **self-эффект**: `<selfEffects><effect name="Blink" /></selfEffects>`
  (хендлер — скрипт `game\data\scripts\handlers\skill\effects\Blink.java`, читает
  `flyRadius=400` и `flyCourse=0` — телепорт назад).
- Цепочка каста: `Creature.onMagicHitTimer` → `Creature.callSkill` → `Skill.activateSkill`.
  Сам блок, применяющий self-эффекты, находится в `Skill.activateSkill`.
- В ядре `Creature.callSkill` стоял ранний выход: `if (targets == null || targets.isEmpty()) return;`.
  Тип цели **AURA входит в список типов, для которых пустой список целей НЕ прерывает каст**
  (`Creature.onMagicLaunchedTimer`: AURA/AURA_CORPSE_MOB/FRONT_AURA/BEHIND_AURA/AURA_FRIENDLY),
  поэтому при отсутствии валидных целей рядом каст спокойно доигрывал анимацию,
  но в `callSkill` завершался до применения self-эффектов. Итог: **анимация есть, телепорта нет**.
  Если же рядом были валидные цели — телепорт срабатывал штатно (self-эффект применялся).

## Правка (один класс)

`org/l2jmobius/gameserver/entity/actor/Creature.java` — метод `callSkill`: при пустом
(но не null) списке целей теперь применяются self-эффекты скилла до выхода (логика 1:1
повторяет self-блок `Skill.activateSkill`: стоп существующего эффекта → `applyEffects(SELF, instant)`):

```java
if (targets == null || targets.isEmpty()) {
   if (targets != null && skill.hasEffects(EffectScope.SELF)) {
      if (this.isAffectedBySkill(skill.getId())) {
         this.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
      }
      skill.applyEffects(this, this, true, false, true, 0);
   }
   return;
}
```

Попутно (техническая необходимость компиляции декомпилированного класса) пять
`switch` по enum-ординалам (`isInsideZone`, `doAttack`, `doCast`, `calculateReuseTime`,
`onMagicLaunchedTimer`) переписаны на эквивалентные if-else цепочки — семантика не менялась
(сверено round-trip-декомпиляцией до/после, все 439 сигнатур методов совпадают).

## Затронутые скиллы (AURA-семейство + selfEffects)

Фикс затрагивает все скиллы, у которых self-эффекты могли не применяться без целей:
1448 **Blink** (основная цель правки), 885 **Air Blink**, 368 **Vengeance**,
8240 **Obliteration Storm**, 7078 Test - Blink. Скиллы с типами ONE/PARTY/CORPSE_MOB
не затронуты (у них пустой список целей и так прерывает каст раньше).

## Установка и откат

- Всё в `_fix_blink\`: `Creature.java` (исходник патча), `apply-blink.cmd` (установщик:
  бэкап → javac против `libs\GameServer.jar` (LibericaJDK 25, флаги `-g -parameters`) → `jar uf`),
  бэкап `GameServer.jar.pre-blink`, `LinkCheck.java` (проверка линковки классов без запуска GS).
- Откат: `copy /Y "_fix_blink\GameServer.jar.pre-blink" "libs\GameServer.jar"` (GS остановлен).
- Повторное применение: `apply-blink.cmd` (пересоберёт класс из `Creature.java`).

## Проверка в игре (за администратором)

1. Перезапустить GameServer.
2. Персонаж Soul Hound: каст Blink **без врагов рядом** → персонаж должен телепортироваться
   назад на 400 (FlyToLocation + ValidateLocation), откат 15 с, расход MP как обычно.
3. Каст Blink **с врагами рядом** → враги получают стан/урон (PhysicalSoulDamage + Stun),
   кастер телепортируется назад — поведение не изменилось.
4. Blink с аутент-заточкой Distance (уровни 201+) — радиус растёт по таблице `#ench2Radius`.
5. Регресс-проверки общего боя: автоатака (doAttack), передвижение (MoveData/ValidatePosition),
   чардж-скиллы (Rush Impact и т.п.), олимпиада (боты), `isInsideZone` (PvP/peace зоны) —
   переписанные if-else эквивалентны исходным switch.
