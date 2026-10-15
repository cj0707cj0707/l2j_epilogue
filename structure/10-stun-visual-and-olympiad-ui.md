# 10. Визуал стана и панель противника на олимпиаде

> Правки от 10.09.2026 (вечер), вторая итерация (поздний вечер) — корневые причины найдены.
> Две задачи: (1) звёздочки стана над головой персонажа, (2) панель противника на олимпиаде —
> HP/CP и иконки его бафов/дебафов.

## Затронутое

| Слой | Файлы |
|---|---|
| Ядро (`libs\GameServer.jar`) | `entity\actor\Player.java` (broadcastStatusUpdate, **broadcastCharInfo**), `entity\actor\holders\creature\EffectList.java` (updateEffectIcons), `mechanics\olympiad\OlympiadGameTask.java` (+2 хелпера) |
| Скрипты (`game\data\scripts`) | `handlers\skill\effects\Stun.java` |
| Резерв JAR | `_olyfix\GameServer.jar.pre-olyui` (первая итерация), `_olyfix\GameServer.jar.pre-olyui2` (вторая) |
| Сборки | `_olyfix\build13_olyui`, `_olyfix\build14_olyui2` (javac, LibericaJDK 25, против `libs\GameServer.jar`) |
| Верификация | `_olyfix\_verify_olyui`, `_olyfix\_verify_olyui2` (декомпиляция vineflower'ом классов ИЗ обновлённого JAR) |

## Итог второй итерации (корневые причины)

### 1. Звёзды стана — ДВЕ причины, обе устранены

**Причина A (главная, ядро).** `Player.isOnlineInt()` возвращает 0 для безклиентских ботов
(`client == null`), а `Player.broadcastCharInfo()` стоял за гейтом `isOnlineInt() != 0` — поэтому
**ни одна CharInfo бот-игрока никогда не рассылалась во время боя**: окружающие клиенты не
получали обновлений внешности (маски AbnormalVisualEffect — звёзды стана, аура Celestial Shield
5576, цвета/титулы) до ближайшего прямого `sendInfo()` (спавн/телепорт). Отсюда наблюдение
«звёзды появляются только после боя»: в конце матча срабатывает прямой `broadcastInfo()` →
CharInfo без гейта. **Правка:** гейт заменён на `isOnline()` (для бота в мире — true, как у
реального игрока). Это же устраняет «нет анимации срабатывания Subclass Counter Celestial» —
5576 несёт AVE INVINCIBILITY, и его аура не доходила до клиентов по той же причине
(MagicSkillUse от `makeTriggerCast` рассылается и так).

**Причина B (датапак, первая итерация).** Часть скиллов стана не имела
`<abnormalVisualEffect>` в XML (886, 4992, 5240, 5241, 5449, 5888), а хендлер эффекта Stun
никаких визуалов не ставил. Правка скрипта `Stun.java`: `onStart` —
`startAbnormalVisualEffect(true, STUN)` после `startStunning()`, `onExit` —
`stopAbnormalVisualEffect(true, STUN)` после `stopStunning(false)`. Теперь звёзды ставятся для
**любого** скилла с эффектом Stun, включая скиллы без XML-атрибута.

Нюанс ретейла сохранён: при двух наложенных станах звёзды гаснут с окончанием первого.

### 2. Панель противника — семантика пакета исправлена

`ExOlympiadUserInfo` (0xFE:0x7A): byte side (арена-сторона бойца), objectId, имя, classId,
curHp, maxHp, curCp, maxCp. Иконки бафов/дебафов под панелью — `ExOlympiadSpelledInfo`
(0xFE:0x7B): objectId участника + список активных эффектов (displayId, displayLevel, время).
Клиент мапит пакет в слоты панели по реальной арене-стороне бойца (как у наблюдателей).

Было: стартовый пакет (соперник, side 1) + живые обновления с постоянным side 2 — обновления
попадали «не в свой» слот; `ExOlympiadSpelledInfo` участникам вообще не отправлялся.

Правки ядра:
- **`Player.broadcastStatusUpdate()`** — живые HP/CP-обновления в матче шлются с
  `getOlympiadSide()` (реальная сторона бойца);
- **`OlympiadGameTask`** — `broadcastBattleState()` / `broadcastPlayerState(subject)`: для
  каждого участника шлёт его `ExOlympiadUserInfo` с его реальной стороной **сопернику, самому
  участнику (инициализация обоих слотов панели) и зрителям**, вместе с `ExOlympiadSpelledInfo`.
  Вызовы: сразу после открытия ворот (`makeCompetitionStart`) + каждый 10-секундный проход
  боевого цикла (страховка от пропущенных событий + наполнение панелей поздно подключившихся
  зрителей);
- **`EffectList.updateEffectIcons()`** — при изменении списка эффектов участника активного
  матча `ExOlympiadSpelledInfo` отправляется и сопернику (видимые игроки того же матча), и
  зрителям (как раньше). Так на панели противника в реальном времени появляются и его бафы, и
  наложенные на него дебафы. Пассивы исключены (`EffectList.getEffects()`).

**Не тронуто.** Коды опкодов, дуэли (`ExDuelUpdateUserInfo`), PvP-флаги/RelationChanged,
механика боя, снятие дебафов, очки. Свои бафы клиент по-прежнему рисует из
`AbnormalStatusUpdate`.

## 1. Анимация стана (звёздочки над головой) — как устроено

«Звёзды над головой» рисуются клиентом, когда в `UserInfo`/`CharInfo` персонажа установлен бит
нормальной маски AbnormalVisualEffect `STUN` (маска 0x40; перечисление
`mechanics.skill.AbnormalVisualEffect`). Маска накапливается в `Creature`
(`startAbnormalVisualEffect` / `stopAbnormalVisualEffect`) и отправляется: себе — `UserInfo`,
окружающим — `CharInfo` (оба пакета пишут и нормальную, и special-маску).

Цепочка: хендлер эффекта `Stun` (скрипт) → `startStunning()` + принудительная установка бита →
`Player.updateAbnormalEffect()` = `broadcastUserInfo()` → `UserInfo` себе + CharInfo окружающим
(через `broadcastCharInfo` — задача 100 мс). Исправленный гейт в `broadcastCharInfo`
(причина A) восстанавливает рассылку для ботов.

## Проверки

- Ядро: `javac` (LibericaJDK 25) против JAR — без ошибок; `Player`(+`Player$1`), `EffectList`,
  `OlympiadGameTask` подменены в JAR (`jar uf`), декомпиляция из JAR подтверждает правки
  (`_verify_olyui`, `_verify_olyui2`).
- Скрипты: `javac` против JAR (`_testbuild\olyui2_scripts`) — без ошибок; применяются при
  компиляции скриптов на старте.
- Действует со следующего старта GameServer (ядро — JAR, скрипты — компиляция при старте).
- Сервер после правок не запускался — проверку в игре выполняет администратор.
