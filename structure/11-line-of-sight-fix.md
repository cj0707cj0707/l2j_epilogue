# 11. Фикс линии видимости: атака/касты/чарджи сквозь преграды

> Правка от 11.09.2026. Задача: любые персонажи (игроки и боты) могут стрелять/кастовать
> и использовать любые скиллы (включая чардж-перемещения) по цели за стеной/преградой.
> **Статус: установлен** в `libs\GameServer.jar` (11.09.2026 ~00:16, при остановленном GS).

## Суть

- Геодата на сервере **не установлена** (`game\data\geodata` пуст): при старте грузится 0 регионов,
  все 1024 региона — `NullRegion`, `PATHFINDING` сам сбрасывается в 0. Рельеф/стены/земля ничего
  не блокируют изначально.
- Единственные оставшиеся препятствия — проверки **закрытых дверей** (`DoorData.checkIfDoorsBetween`)
  и **заборов** (`FenceData.checkIfFenceBetween`) внутри двух методов ядра
  `org.l2jmobius.gameserver.geoengine.GeoEngine`:
  - `canSeeTarget(int x, int y, int z, int targetX, int targetY, int targetZ, int instanceId)`;
  - `canMoveToTarget(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int instanceId)`

## Правка (один файл)

`org/l2jmobius/gameserver/geoengine/GeoEngine.java` — оба метода теперь всегда возвращают `true`
(комментарий с объяснением в коде). Все потребители идут через них:

- `canSeeTarget`: автоатака (`Creature.doAttack` → «You cannot see your target»), каст любых
  дальних скиллов (`Player.checkUseMagicConditions`), враждебные AoE
  (`Skill.checkForAreaOffensiveSkills`, сбор целей `skipLOS` в `Creature`), молчаливое обнуление
  урона дальними скиллами по игрокам (`Creature.reduceCurrentHp`), автотаргет ботов/`.play`
  (`AutoPlayTaskManager$AutoPlay` — сброс цели `setTarget(null)`), саммоны
  (`Summon`, `SkillChannelizer`), агро/ИИ NPC (`AttackableAI`, `SiegeGuardAI`, `FortSiegeGuardAI`),
  `ConditionMinDistance`, `Tower`, офсеты телепортов (`Creature.teleToLocation`, `Spawn`).
- `canMoveToTarget`: гейт **чардж-скиллов** (`FlyType.CHARGE` в `Player.checkUseMagicConditions` —
  Rush Impact, Shadow Step и т.п.: «The target is located where you cannot charge»), подход
  ботов/саммонов/мобов (`CreatureAI`, `SummonAI`, `AttackableAI`), автоподбор ботами предметов.

**Не тронуто:** `GeoEngine.getValidLocation(...)` — обычная ходьба по-прежнему подрезается
у закрытых дверей (сквозь дверь не пройти, только атаковать/кастовать); `PathFinding` (выключен);
PvP/peace-правила, олимпиада, статы.

## Установка и откат

- Всё в `_fix_geolos\`: `GeoEngine.java` (фикс), `apply-geolos.cmd` (установщик:
  бэкап → javac против `libs\GameServer.jar` (LibericaJDK 25) → `jar uf` двух классов),
  бэкап `GameServer.jar.pre-geolos`, верификация декомпиляцией (`_verify\src5`).
- Откат: `copy /Y "_fix_geolos\GameServer.jar.pre-geolos" "libs\GameServer.jar"` (GS остановлен).
- Подробности и план игровой проверки — `_fix_geolos\README.md`.

## Границы/замечания

- Если позже установить геодату — фикс продолжит пропускать атаки сквозь рельеф/стены
  (LOS всегда истинен); откат возвращает штатное поведение.
- Мобы/охрана тоже «видят» и атакуют цель за закрытой дверью и подходят к двери вплотную
  (без геодаты проход мобов сквозь стены и так был возможен).
- Требуется перезапуск GameServer (ядро — JAR). Проверка в игре — за администратором.
